package com.isuncy.ilife798.data.remote

import com.isuncy.ilife798.core.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/** OkHttp 单例与请求封装，请求头与 Python 版 get_headers 一致。 */
object ApiClient {

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun headers(
        token: String,
        appType: String = "1,1",
        versionCode: String = Constants.VERSION_NUM,
        userAgent: String = Constants.USER_AGENT
    ): Headers = Headers.Builder()
        .add("user-agent", userAgent)
        .add("VersionCode", versionCode)
        .add("ApplicationType", appType)
        .add("Content-Type", "application/json; charset=utf-8")
        .add("Accept", "application/json")
        .apply { if (token.isNotEmpty()) add("Authorization", token) }
        .build()

    /** GET，网络异常返回 null（与 Python 的 try/except 行为一致）。调用方负责 use/close。 */
    suspend fun get(path: String, token: String, params: List<Pair<String, String>> = emptyList()): Response? =
        withContext(Dispatchers.IO) {
            try {
                val url = (Constants.BASE_URL + path).toHttpUrl().newBuilder()
                    .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
                    .build()
                val request = Request.Builder().url(url).headers(headers(token)).get().build()
                http.newCall(request).execute()
            } catch (e: Exception) {
                null
            }
        }

    /** POST JSON，网络异常返回 null。调用方负责 use/close。请求头可按探测组合覆盖。 */
    suspend fun post(
        path: String,
        token: String,
        params: List<Pair<String, String>> = emptyList(),
        jsonBody: String? = null,
        appType: String = "1,1",
        versionCode: String = Constants.VERSION_NUM,
        userAgent: String = Constants.USER_AGENT
    ): Response? = withContext(Dispatchers.IO) {
        try {
            val url = (Constants.BASE_URL + path).toHttpUrl().newBuilder()
                .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
                .build()
            val body = (jsonBody ?: "").toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(url).headers(headers(token, appType, versionCode, userAgent)).post(body).build()
            http.newCall(request).execute()
        } catch (e: Exception) {
            null
        }
    }

    /** GET 且要求 HTTP 200（与 Python 对 mission-lst/score-lst 的 status_code 检查一致），解析失败返回 null。 */
    suspend fun getJsonStrict(path: String, token: String, params: List<Pair<String, String>> = emptyList()): JsonObject? {
        val res = get(path, token, params) ?: return null
        return res.use { resp ->
            if (resp.code != 200) null
            else resp.body?.string()?.let { Parsers.asJsonOrNull(it) }
        }
    }
}
