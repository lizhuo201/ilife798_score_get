package com.isuncy.ilife798.data.remote

import com.isuncy.ilife798.core.Constants
import com.isuncy.ilife798.data.model.SendResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 慧生活798 接口封装：路径、参数与 Python 版 api_get/api_post 逐项一致。 */
object IlifApi {

    /** 验证会话：GET /api/v1/acc/stat。 */
    suspend fun verifySession(token: String): Boolean {
        if (token.isEmpty()) return false
        val res = ApiClient.get("/api/v1/acc/stat", token) ?: return false
        val json = res.use { resp ->
            resp.body?.string()?.let { Parsers.asJsonOrNull(it) }
        } ?: return false
        return Parsers.isSuccessCode(json)
    }

    /** 图形验证码图片地址（App 内直接加载显示，s 为本次会话 seed）。 */
    fun captchaUrl(seed: Double, ts: Long): String =
        Constants.BASE_URL + "/api/v1/captcha/?s=" + seed + "&r=" + ts

    /** 下发短信验证码：POST /api/v1/acc/login/code，code==0 表示成功。 */
    suspend fun sendLoginCode(phone: String, graphicCode: String, seed: Double): Boolean {
        val body = buildJsonObject {
            put("un", phone)
            put("authCode", graphicCode)
            put("s", seed)
        }.toString()
        val res = ApiClient.post("/api/v1/acc/login/code", token = "", jsonBody = body) ?: return false
        val json = res.use { resp -> resp.body?.string()?.let { Parsers.asJsonOrNull(it) } } ?: return false
        return Parsers.isCodeZero(json)
    }

    /** 短信验证码登录：POST /api/v1/acc/login，成功返回 (token, uid)。 */
    suspend fun login(phone: String, smsCode: String): Pair<String, String>? {
        val body = buildJsonObject {
            put("un", phone)
            put("authCode", smsCode)
            put("openCode", "")
        }.toString()
        val res = ApiClient.post("/api/v1/acc/login", token = "", jsonBody = body) ?: return null
        val json = res.use { resp -> resp.body?.string()?.let { Parsers.asJsonOrNull(it) } } ?: return null
        return if (Parsers.isCodeZero(json)) Parsers.parseLogin(json) else null
    }

    /** 任务列表：GET /api/v1/acc/score/mission-lst（HTTP 200 才解析）。 */
    suspend fun getMissions(token: String): JsonObject? =
        ApiClient.getJsonStrict("/api/v1/acc/score/mission-lst", token)

    /** 今日流水：GET /api/v1/acc/score/score-lst，按 todayZeroMs 统计各任务完成次数。 */
    suspend fun getTodayCounts(token: String, todayZeroMs: Long): Map<String, Int> {
        val json = ApiClient.getJsonStrict(
            "/api/v1/acc/score/score-lst",
            token,
            params = listOf("page" to "1", "size" to "50", "hasCount" to "true", "src" to "")
        ) ?: return emptyMap()
        return try {
            Parsers.parseTodayCounts(json, todayZeroMs)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 积分上报：POST /api/v1/acc/score/score-send?sign=<md5>&s=<s>，网络异常返回 null。
     * s 由调用方按参数组合给出（官方 App 通道 "1" / dex 还原的 "true"）。
     */
    suspend fun sendScore(
        token: String,
        sign: String,
        s: String,
        body: JsonObject,
        appType: String = "1,1",
        versionCode: String = Constants.VERSION_NUM,
        userAgent: String = Constants.USER_AGENT
    ): SendResult? {
        val params = buildList {
            add("sign" to sign)
            if (s.isNotEmpty()) add("s" to s)
        }
        val res = ApiClient.post(
            "/api/v1/acc/score/score-send",
            token,
            params = params,
            jsonBody = body.toString(),
            appType = appType,
            versionCode = versionCode,
            userAgent = userAgent
        ) ?: return null
        return res.use { resp ->
            val json = resp.body?.string()?.let { Parsers.asJsonOrNull(it) } ?: return@use null
            SendResult(
                ok = Parsers.isSuccessCode(json),
                code = json["code"].primitiveStringOrNull() ?: json["status"].primitiveStringOrNull() ?: "",
                msg = json["msg"].primitiveStringOrNull() ?: "OK"
            )
        }
    }
}
