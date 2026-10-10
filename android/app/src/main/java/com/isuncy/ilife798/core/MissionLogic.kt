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
     * 构造 score-send 请求体（对齐 3.1.7 官方 App 的 native 逻辑）：
     * - 普通任务：{"adId": ..., "type": 101}
     * - 每日签到：{"adId": ..., "weekDay": 1..7}（北京时间 ISO 星期，注意是大写 D）
     * 旧版脚本附带的 addScore/addScoreType/weekday 会被服务端视为无效请求。
     */
    fun buildSendBody(task: TaskPlan, nowMs: Long): JsonObject = buildJsonObject {
        put("adId", task.refId)
        if (task.isCheckIn) {
            put("weekDay", TimeUtil.isoWeekdayInBeijing(nowMs))
        } else {
            put("type", 101)
        }
    }
}
