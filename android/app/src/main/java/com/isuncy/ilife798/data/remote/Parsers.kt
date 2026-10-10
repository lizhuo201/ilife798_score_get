package com.isuncy.ilife798.data.remote

import com.isuncy.ilife798.core.Constants
import com.isuncy.ilife798.data.model.MissionItem
import com.isuncy.ilife798.data.model.MissionsResult
import kotlinx.serialization.json.*

/** 响应解析，字段容错与 Python 版 dict.get 行为对齐。 */
object Parsers {

    fun asJsonOrNull(text: String): JsonObject? = try {
        Json.parseToJsonElement(text) as? JsonObject
    } catch (e: Exception) {
        null
    }

    /**
     * 与 Python「str(d.get('code', d.get('status'))) in ('0','200','None')」一致：
     * code 键存在时只看 code（值为 null 视为 'None' 即成功）；code 键缺失才看 status。
     */
    fun isSuccessCode(json: JsonObject): Boolean {
        val str = if (json.containsKey("code")) {
            json["code"].primitiveStringOrNull() ?: "None"
        } else {
            json["status"].primitiveStringOrNull() ?: "None"
        }
        return str in setOf("0", "200", "None")
    }

    /** 与 Python「str(d.get('code')) == '0'」一致。 */
    fun isCodeZero(json: JsonObject): Boolean = json["code"].primitiveStringOrNull() == "0"

    /** 解析登录响应 data.al.{token,uid}。 */
    fun parseLogin(json: JsonObject): Pair<String, String>? {
        val data = json["data"] as? JsonObject ?: return null
        val al = data["al"] as? JsonObject ?: return null
        val token = al["token"].primitiveStringOrNull() ?: return null
        val uid = al["uid"].primitiveStringOrNull() ?: return null
        return token to uid
    }

    /**
     * 解析 mission-lst → MissionsResult，与 Python process_single_account 的取值一致。
     * data 缺失时返回全零结果（Python 中 inner={} 仍会继续执行并做最终结算）。
     */
    fun parseMissions(json: JsonObject): MissionsResult {
        val data = json["data"] as? JsonObject ?: JsonObject(emptyMap())
        val acc = data["accScoreRsp"] as? JsonObject ?: JsonObject(emptyMap())
        val daily = acc["daily"] as? JsonObject ?: JsonObject(emptyMap())
        val dailyRsp = data["dailyRSP"] as? JsonObject

        val missions = (data["missions"] as? JsonArray).orEmpty().mapNotNull { el ->
            val m = el as? JsonObject ?: return@mapNotNull null
            MissionItem(
                name = m["name"].primitiveStringOrNull() ?: "",
                refId = m["refId"].primitiveStringOrNull() ?: "",
                score = m["score"].asInt(0),
                limit = m["limit"].asInt(Constants.DEFAULT_MISSION_LIMIT),
                hasUrl = hasTruthyUrl(m["url"])
            )
        }

        return MissionsResult(
            score = acc["score"].asInt(0),
            totalScore = acc["totalScore"].asInt(0),
            dailyLtime = daily["ltime"].asLong(0),
            dailyWeek = daily["week"] as? JsonPrimitive,
            dailyAdId = dailyRsp?.get("adId")?.primitiveStringOrNull(),
            dailyScore = dailyRsp?.get("score").asInt(Constants.DEFAULT_CHECKIN_SCORE),
            missions = missions
        )
    }

    /** 解析 score-lst，统计 todayZeroMs 之后各 adId 的完成次数（与 Python get_today_completed_counts 一致）。 */
    fun parseTodayCounts(json: JsonObject, todayZeroMs: Long): Map<String, Int> {
        val data = json["data"]
        val items: List<JsonElement> = when {
            data is JsonArray -> data.toList()
            data is JsonObject -> {
                val inner = data["list"] ?: data["records"]
                if (inner is JsonArray) inner.toList() else emptyList()
            }
            else -> emptyList()
        }

        val counts = mutableMapOf<String, Int>()
        for (el in items) {
            val item = el as? JsonObject ?: continue
            val ctime = item["ctime"].asLong(0)
            if (ctime < todayZeroMs) continue
            val itemData = item["data"] as? JsonObject
            val adId = (itemData?.get("adId") ?: item["adId"]).primitiveStringOrNull() ?: continue
            counts[adId] = (counts[adId] ?: 0) + 1
        }
        return counts
    }
}

// ---------- 顶层扩展（同包内 Parsers/IlifApi 直接可见，无需 import） ----------

/** JsonPrimitive（数值/字符串/布尔）转字符串，null 与 JsonNull 返回 null。 */
fun JsonElement?.primitiveStringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

/** 转 Int：先试整数再试浮点（兼容 5/"5"/5.0），失败返回默认值。 */
fun JsonElement?.asInt(def: Int = 0): Int {
    val p = this as? JsonPrimitive ?: return def
    if (p is JsonNull) return def
    p.content.toIntOrNull()?.let { return it }
    p.content.toDoubleOrNull()?.let { return it.toInt() }
    return def
}

fun JsonElement?.asLong(def: Long = 0L): Long {
    val p = this as? JsonPrimitive ?: return def
    if (p is JsonNull) return def
    p.content.toLongOrNull()?.let { return it }
    p.content.toDoubleOrNull()?.let { return it.toLong() }
    return def
}

/** Python「m.get('url')」真值判断：非空字符串或任意复合值视为有 url。 */
fun hasTruthyUrl(el: JsonElement?): Boolean = when (el) {
    null, is JsonNull -> false
    is JsonPrimitive -> el.content.isNotEmpty()
    else -> true
}
