package com.isuncy.ilife798.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** 账号凭证，JSON 字段与 Python 版 ilife_accounts.json 完全一致。 */
@Serializable
data class Account(
    val phone: String,
    val token: String,
    val uid: String
)

/** mission-lst 中的单个任务原始字段。 */
data class MissionItem(
    val name: String,
    val refId: String,
    val score: Int,
    val limit: Int,
    val hasUrl: Boolean
)

/** mission-lst 解析结果。 */
data class MissionsResult(
    val score: Int,
    val totalScore: Int,
    val dailyLtime: Long,
    val dailyWeek: JsonPrimitive?,
    val dailyAdId: String?,
    val dailyScore: Int,
    val missions: List<MissionItem>,
    /** 响应体顶层 time 字段（毫秒），用于签名时间校正；缺失为 0。 */
    val serverTimeMs: Long = 0L
)

/** 待执行任务。 */
data class TaskPlan(
    val name: String,
    val refId: String,
    val score: Int,
    val type: Int,
    val runsNeeded: Int,
    val progress: String,
    /** true 表示每日签到：上报体使用 weekDay（1..7）而非 type。 */
    val isCheckIn: Boolean = false
)

/** score-send 的响应判定结果。 */
data class SendResult(
    val ok: Boolean,
    val code: String,
    val msg: String
)

/** 单账号一次执行的完整记录。 */
@Serializable
data class HistoryRecord(
    val time: Long,
    val phone: String,
    val successSends: Int,
    val failedSends: Int,
    val beforeScore: Int,
    val afterScore: Int,
    val logs: List<String>
)
