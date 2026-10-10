package com.isuncy.ilife798.core

import com.isuncy.ilife798.data.model.MissionsResult
import com.isuncy.ilife798.data.model.TaskPlan
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 任务过滤与执行规则，逐条复刻 auto_score.py 的 process_single_account。 */
object MissionLogic {

    /**
     * 按原脚本规则生成待执行队列：
     * 1) 每日签到（ltime 与今日流水双保险判断是否已签到）；
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
            if (!signedByLtime && !signedByStream) {
                tasks += TaskPlan(
                    name = "每日签到",
                    refId = dailyAdId,
                    score = result.dailyScore,
                    type = Constants.TASK_TYPE_CHECKIN,
                    weekday = result.dailyWeek,
                    runsNeeded = 1,
                    progress = "0/1"
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
                    weekday = null,
                    runsNeeded = runs,
                    progress = "$realDone/$maxLimit"
                )
            }
        }
        return tasks
    }

    /** 构造 score-send 请求体：weekday 为 null 时不放入（与 Python 过滤 None 一致）。 */
    fun buildSendBody(task: TaskPlan): JsonObject = buildJsonObject {
        put("adId", task.refId)
        put("type", 101)
        if (task.weekday != null && task.weekday !is JsonNull) put("weekday", task.weekday)
        put("addScoreType", task.type)
        put("addScore", task.score)
    }
}
