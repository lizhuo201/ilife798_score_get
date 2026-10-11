package com.isuncy.ilife798.core

import com.isuncy.ilife798.data.model.MissionsResult
import com.isuncy.ilife798.data.model.TaskPlan
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 任务过滤与执行规则，逐条复刻 auto_score.py 的 process_single_account。 */
object MissionLogic {

    /**
     * 按原脚本规则生成待执行队列：
     * 1) 每日签到：dailyRSP.adId 非空、且「本日已签」三种判据（daily.ltime 同日、今日流水计数、
     *    daily.week 位图）都不成立时才加入；
     * 2) 普通任务：score==0、含 url、名称含"权益"、score>100 的跳过；
     *    limit<=0 视为 5；真实剩余次数 = limit - 今日流水完成数；
     *    类型判定：refId=="popsreen" 或名称含"广告"/"全屏" → 4，其余 → 2。
     */
    fun buildTaskQueue(result: MissionsResult, todayCounts: Map<String, Int>, nowMs: Long): List<TaskPlan> {
        val tasks = mutableListOf<TaskPlan>()

        // 1. 每日签到（dailyRSP.adId 存在且非空才加入）
        val dailyAdId = result.dailyAdId
        if (dailyAdId != null && dailyAdId.isNotEmpty()) {
            val signedByLtime = result.dailyLtime > 0 && TimeUtil.isSameLocalDay(result.dailyLtime, nowMs)
            val signedByStream = todayCounts.getOrDefault(dailyAdId, 0) > 0
            val signedByWeekMask = isSignedTodayByWeekMask(result.dailyWeek, nowMs)
            if (!signedByLtime && !signedByStream && !signedByWeekMask) {
                tasks += TaskPlan(
                    name = "每日签到",
                    refId = dailyAdId,
                    score = result.dailyScore,
                    type = Constants.TASK_TYPE_CHECKIN,
                    runsNeeded = 1,
                    progress = "0/1",
                    isCheckIn = true
                )
            }
        }

        // 2. 普通任务
        for (m in result.missions) {
            if (m.score == 0 || m.hasUrl || m.name.contains("权益") || m.score > 100) continue

            val maxLimit = if (m.limit > 0) m.limit else Constants.DEFAULT_MISSION_LIMIT
            val realDone = todayCounts.getOrDefault(m.refId, 0)
            val runs = maxOf(0, maxLimit - realDone)

            val type = if (m.refId == "popsreen" || m.name.contains("广告") || m.name.contains("全屏")) {
                Constants.TASK_TYPE_AD
            } else {
                Constants.TASK_TYPE_NORMAL
            }

            if (runs > 0) {
                tasks += TaskPlan(
                    name = m.name,
                    refId = m.refId,
                    score = m.score,
                    type = type,
                    runsNeeded = runs,
                    progress = "$realDone/$maxLimit"
                )
            }
        }
        return tasks
    }

    /**
     * daily.week 是本周签到位图（bit0 = 周一，北京时间），对应位为 1 表示当天已签到。
     * 与第三方还原实现 signed_today(week_mask) = ((week >> (isoweekday-1)) & 1) 一致。
     */
    private fun isSignedTodayByWeekMask(dailyWeek: JsonPrimitive?, nowMs: Long): Boolean {
        val mask = dailyWeek?.content?.toIntOrNull() ?: return false
        if (mask <= 0) return false
        val bit = TimeUtil.isoWeekdayInBeijing(nowMs) - 1
        return (mask shr bit) and 1 == 1
    }

    /**
     * score-send 的参数组合。服务端 2026-10 起改用服务端时间签名后，各第三方还原实现分成两派：
     * 官方 App（dex 还原）发 s=true + 极简 body，web 工具发 s=1 + 带 addScore 的 body。
     * 执行器按 [SEND_VARIANTS] 顺序探测，第一组成功后固定沿用。
     */
    data class SendVariant(
        val s: String,
        val includeScore: Boolean,
        val signMode: Signer.SignMode,
        val appType: String,
        val versionCode: String,
        val userAgent: String
    ) {
        val label: String get() =
            signMode.name + " s=" + (if (s.isEmpty()) "无" else s) +
                (if (includeScore) " 带addScore" else " 极简body") +
                " AppType=" + appType
    }

    /**
     * 上报参数组合（按顺序探测，第一组成功后固定沿用）。
     * 第 1 组即 nocookies111/life-798 v5.4.4 classes.dex 分支A 的逐字还原：
     *   raw = adId + ((now + skew)/30000)*30 + token[-8:] + uid[-8:] + "AbCdEfGhIjKlMnOpQrStUvWxYz12345"
     *   URL = .../score-send?sign=<md5>&s=1，请求头 ApplicationType=1,1 + VersionCode=3.1.9
     *   UA = Android_ilife798_3.1.9，body = {"adId":..,"type":101}（官方渠道任务另带 addScore/addScoreType=4）
     * 之所以先前失败：v1.0.5/1.0.6 少了 raw 最前面的 adId（因此先发的那一组必然验签失败，
     * 而验签失败次数会触发服务端 -98 临时限流，把后面的正确组合全部挡住）。
     */
    val SEND_VARIANTS = listOf(
        // ★ 现行公式（分支A 原样）
        SendVariant("1", false, Signer.SignMode.N30_A_SKEW_ADID, "1,1", Constants.NC_VERSION_A, Constants.NC_UA_A),
        // 备用1：分支B 原样（1,5 / 2.0.178，不带 s，本机时间，UA 为 WaterWidget/5.4.4）
        SendVariant("", false, Signer.SignMode.N30_B_LOCAL_ADID, "1,5", Constants.NC_VERSION_B, Constants.NC_UA_B),
        // 备用2：分支A + addScore/addScoreType=4（官方 App 渠道任务可能要求的 body）
        SendVariant("1", true, Signer.SignMode.N30_A_SKEW_ADID, "1,1", Constants.NC_VERSION_A, Constants.NC_UA_A)
    )

    /**
     * 构造 score-send 请求体：
     * - 普通任务：{"adId": ..., "type": 101}，[SendVariant.includeScore] 为真时再附 addScoreType/addScore
     * - 每日签到：{"adId": ..., "weekDay": 1..7}（北京时间 ISO 星期，注意是大写 D），不带分数
     * 旧版脚本附带的小写 weekday 会被服务端视为无效请求，任何组合都不再发送。
     */
    fun buildSendBody(task: TaskPlan, nowMs: Long, variant: SendVariant): JsonObject = buildJsonObject {
        put("adId", task.refId)
        if (task.isCheckIn) {
            put("weekDay", TimeUtil.isoWeekdayInBeijing(nowMs))
        } else {
            put("type", 101)
            if (variant.includeScore) {
                put("addScoreType", 1)
                put("addScore", task.score)
            }
        }
    }
}
