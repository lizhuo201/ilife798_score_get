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
    val missions: List<MissionItem>
)

/** 待执行任务（与 Python 版 tasks 列表结构一致）。 */
data class TaskPlan(
    val name: String,
    val refId: String,
    val score: Int,
    val type: Int,
    val weekday: JsonPrimitive?,
    val runsNeeded: Int,
    val progress: String
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
