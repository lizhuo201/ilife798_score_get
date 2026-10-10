package com.isuncy.ilife798.execution

import com.isuncy.ilife798.core.Constants
import com.isuncy.ilife798.core.MissionLogic
import com.isuncy.ilife798.core.Signer
import com.isuncy.ilife798.core.TimeUtil
import com.isuncy.ilife798.data.model.Account
import com.isuncy.ilife798.data.model.MissionsResult
import com.isuncy.ilife798.data.remote.IlifApi
import com.isuncy.ilife798.data.remote.Parsers
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 核心执行器：逐条复刻 auto_score.py 的 execute_daily_routine。
 * sleeper 可注入，便于测试时把 sleep 换成 0。
 */
class ScoreRunner(private val sleeper: suspend (Long) -> Unit = { ms -> delay(ms) }) {

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
        val parsed: MissionsResult = IlifApi.getMissions(account.token)
            ?.let { Parsers.parseMissions(it) }
            ?: run {
                onLog("获取任务列表失败，跳过该账号")
                return AccountResult(account.phone, 0, 0, 0, 0)
            }
        val beforeScore = parsed.score

        // 2. 今日流水统计
        val todayCounts = IlifApi.getTodayCounts(account.token, todayZero)

        // 3. 生成待执行队列
        val queue = MissionLogic.buildTaskQueue(parsed, todayCounts, nowMs)
        onLog("当前积分 " + parsed.score + "（总 " + parsed.totalScore + "），待执行任务 " + queue.size + " 个")
        if (queue.isEmpty()) onLog("所有可用任务均已达上限，无需执行。")

        var successSends = 0
        var failedSends = 0

        for (task in queue) {
            onLog("- 任务[" + task.name + "] 执行 " + task.runsNeeded + " 次（进度 " + task.progress + "）")
            for (runIndex in 1..task.runsNeeded) {
                val sign = Signer.signNow(task.refId, account.token, account.uid)
                val body = MissionLogic.buildSendBody(task)
                val result = IlifApi.sendScore(account.token, sign, body)

                val ok: Boolean
                if (result == null) {
                    failedSends += 1
                    onLog("  x 第 " + runIndex + " 次上报网络异常")
                    ok = false
                } else if (result.ok) {
                    successSends += 1
                    onLog("  v 第 " + runIndex + " 次上报成功（+" + task.score + " 分）")
                    ok = true
                } else {
                    failedSends += 1
                    onLog("  x 第 " + runIndex + " 次上报失败 code=" + result.code + " msg=" + result.msg)
                    ok = false
                }

                // 与 Python 一致：每次上报后（含失败那次）都随机暂停，失败则 break 剩余次数
                sleeper(randomRunDelay())
                if (!ok) break
            }
        }

        // 4. 重取 mission-lst 记录最新积分
        val afterScore = IlifApi.getMissions(account.token)
            ?.let { Parsers.parseMissions(it)?.score }
            ?: beforeScore
        onLog("最终积分 " + afterScore)
        return AccountResult(account.phone, successSends, failedSends, beforeScore, afterScore)
    }

    private fun randomRunDelay(): Long =
        Random.nextLong(Constants.MIN_RUN_DELAY_MS, Constants.MAX_RUN_DELAY_MS + 1)
}
