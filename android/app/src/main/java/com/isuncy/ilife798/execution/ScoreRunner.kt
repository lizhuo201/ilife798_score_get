package com.isuncy.ilife798.execution

import com.isuncy.ilife798.core.Constants
import com.isuncy.ilife798.core.MissionLogic
import com.isuncy.ilife798.core.Signer
import com.isuncy.ilife798.core.TimeUtil
import com.isuncy.ilife798.data.model.Account
import com.isuncy.ilife798.data.model.MissionsResult
import com.isuncy.ilife798.data.model.TaskPlan
import com.isuncy.ilife798.data.remote.IlifApi
import com.isuncy.ilife798.data.remote.Parsers
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 核心执行器：逐条复刻 auto_score.py 的 execute_daily_routine。
 * sleeper 可注入，便于测试时把 sleep 换成 0。
 */
class ScoreRunner(private val sleeper: suspend (Long) -> Unit = { ms -> delay(ms) }) {

    /** 当前生效的 score-send 参数组合下标（首次成功后固定沿用，跨账号保持）。 */
    private var variantIndex = 0

    /** 被服务端限流（code=-98）时抛出，用于中止当前账号而不是继续消耗尝试次数。 */
    private class RateLimitedException(msg: String) : Exception(msg)

    data class AccountResult(
        val phone: String,
        val successSends: Int,
        val failedSends: Int,
        val beforeScore: Int,
        val afterScore: Int
    )

    suspend fun run(
        accounts: List<Account>,
        skipVerify: Boolean,
        onLog: (String) -> Unit,
        onAccountStart: (index: Int, total: Int, phone: String) -> Unit,
        onAccountDone: (AccountResult) -> Unit
    ) {
        for ((index, account) in accounts.withIndex()) {
            onAccountStart(index + 1, accounts.size, account.phone)
            onLog("== 账号 [" + account.phone + "] 开始 ==")

            val result = if (!skipVerify && !IlifApi.verifySession(account.token)) {
                onLog("Token 已失效，跳过该账号")
                AccountResult(account.phone, 0, 0, 0, 0)
            } else {
                runOneAccount(account, onLog)
            }
            onAccountDone(result)
            onLog("== 账号 [" + account.phone + "] 结束：成功 " + result.successSends +
                " / 失败 " + result.failedSends + "，积分 " + result.beforeScore + " -> " +
                result.afterScore + " ==")

            if (index != accounts.lastIndex) {
                val delayMs = Random.nextLong(Constants.MIN_ACCOUNT_DELAY_MS, Constants.MAX_ACCOUNT_DELAY_MS + 1)
                onLog("账号间暂停 " + delayMs / 1000 + "s ...")
                sleeper(delayMs)
            }
        }
    }

    private suspend fun runOneAccount(account: Account, onLog: (String) -> Unit): AccountResult {
        val nowMs = System.currentTimeMillis()
        val todayZero = TimeUtil.startOfTodayMs(nowMs)

        // 1. mission-lst（HTTP 200 由 ApiClient.getJsonStrict 保证）
        //    localTs 为本次请求前的本机时间，与服务端返回的 time 字段求偏差用于签名校正
        val localTs = System.currentTimeMillis()
        val parsed: MissionsResult = IlifApi.getMissions(account.token)
            ?.let { Parsers.parseMissions(it) }
            ?: run {
                onLog("获取任务列表失败，跳过该账号")
                return AccountResult(account.phone, 0, 0, 0, 0)
            }
        val serverTs = parsed.serverTimeMs
        if (serverTs > 0L) {
            val skew = serverTs - localTs
            onLog("服务端时间偏差 " + skew + " ms" +
                if (skew <= Signer.MAX_SKEW_MS && skew >= -Signer.MAX_SKEW_MS) "（签名已按服务端时间校正）" else "（超出阈值，忽略校正）")
        }
        val beforeScore = parsed.score

        // 2. 今日流水统计
        val todayCounts = IlifApi.getTodayCounts(account.token, todayZero)

        // 3. 生成待执行队列
        val queue = MissionLogic.buildTaskQueue(parsed, todayCounts, nowMs)
        onLog("当前积分 " + parsed.score + "（总 " + parsed.totalScore + "），待执行任务 " + queue.size + " 个")
        if (queue.isEmpty()) onLog("所有可用任务均已达上限，无需执行。")
        else onLog("上报参数组合[" + MissionLogic.SEND_VARIANTS[variantIndex].label + "]")

        var successSends = 0
        var failedSends = 0

        try {
            for (task in queue) {
                onLog("- 任务[" + task.name + "] 执行 " + task.runsNeeded + " 次（进度 " + task.progress + "）")
                for (runIndex in 1..task.runsNeeded) {
                    val ok = sendOnce(task, account, localTs, serverTs, runIndex, onLog)
                    if (ok) successSends += 1 else failedSends += 1

                    // 与 Python 一致：每次上报后（含失败那次）都随机暂停，失败则 break 剩余次数
                    sleeper(randomRunDelay())
                    if (!ok) break
                }
            }
        } catch (e: RateLimitedException) {
            onLog("!! 服务器限流（code=" + Constants.RATE_LIMIT_CODE + "，msg=" + e.message + "）")
            onLog("!! 已中止本账号：验签失败或被判定请求过密都会触发临时封锁，继续尝试只会延长封锁时间。")
            onLog("!! 请等待 10 分钟以上再执行。")
        }

        // 4. 重取 mission-lst 记录最新积分
        val afterScore = IlifApi.getMissions(account.token)
            ?.let { Parsers.parseMissions(it)?.score }
            ?: beforeScore
        onLog("最终积分 " + afterScore)
        return AccountResult(account.phone, successSends, failedSends, beforeScore, afterScore)
    }

    /**
     * 上报一次（含参数组合探测）。成功返回 true 并记住该组合；失败按 MissionLogic.SEND_VARIANTS
     * 顺序换用下一组重试，被限流（code=-98）则等一会儿重试同一组合。
     * 网络异常不换组合，直接算失败。
     */
    private suspend fun sendOnce(
        task: TaskPlan,
        account: Account,
        localTs: Long,
        serverTs: Long,
        runIndex: Int,
        onLog: (String) -> Unit
    ): Boolean {
        var probe = variantIndex
        var rateLimitRetries = 0
        while (true) {
            val variant = MissionLogic.SEND_VARIANTS[probe]
            // 每次请求都重新取当前时间签名（nocookies 现行公式按 30 秒一桶校验）
            val sendMs = System.currentTimeMillis()
            val sign = Signer.sign(
                variant.signMode,
                task.refId, account.token, account.uid, sendMs, localTs, serverTs
            )
            val body = MissionLogic.buildSendBody(task, sendMs, variant)
            val result = IlifApi.sendScore(
                account.token, sign, variant.s, body,
                variant.appType, variant.versionCode, variant.userAgent
            )

            if (result == null) {
                onLog("  x 第 " + runIndex + " 次上报网络异常")
                return false
            }
            if (result.ok) {
                if (probe != variantIndex) {
                    variantIndex = probe
                    onLog("  · 参数组合[" + variant.label + "]可用，后续沿用")
                }
                onLog("  v 第 " + runIndex + " 次上报成功（+" + task.score + " 分）")
                return true
            }
            if (result.code == Constants.RATE_LIMIT_CODE) {
                if (rateLimitRetries < Constants.MAX_RATE_LIMIT_RETRY) {
                    rateLimitRetries += 1
                    onLog("  ! 第 " + runIndex + " 次被限流（code=" + result.code + "），" +
                        Constants.RATE_LIMIT_DELAY_MS / 1000 + "s 后重试一次")
                    sleeper(Constants.RATE_LIMIT_DELAY_MS)
                    continue
                }
                throw RateLimitedException(result.msg)
            }
            onLog("  x 第 " + runIndex + " 次上报失败 code=" + result.code + " msg=" + result.msg +
                "（参数[" + variant.label + "]）")
            if (probe == MissionLogic.SEND_VARIANTS.lastIndex) {
                onLog("  · 全部参数组合均失败，跳过该任务剩余次数")
                return false
            }
            probe += 1
            sleeper(Constants.PROBE_DELAY_MS)
            onLog("  · 换用参数组合[" + MissionLogic.SEND_VARIANTS[probe].label + "]重试")
        }
    }

    private fun randomRunDelay(): Long =
        Random.nextLong(Constants.MIN_RUN_DELAY_MS, Constants.MAX_RUN_DELAY_MS + 1)
}
