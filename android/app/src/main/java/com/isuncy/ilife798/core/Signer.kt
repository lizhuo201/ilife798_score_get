package com.isuncy.ilife798.core

import java.security.MessageDigest

/**
 * 签名算法复刻官方 App 3.1.7（服务端 2026-10 起改用服务器时间签名）：
 *
 *   v20  = 10 * ((serverTs - localTs + nowMs) / 10000)
 *   sign = md5(businessId + v20 + token[-8:] + uid[-8:] + SALT)
 *
 * localTs 为请求任务列表（mission-lst）前的本机毫秒时间，serverTs 为该响应体顶层 time 字段
 * （毫秒），两者之差即本机相对服务器的时间偏差。旧版脚本直接用本机时间（等价于
 * localTs == serverTs），升级后会被服务端判为「验签失败 code=-1」。
 *
 * time 字段缺失（serverTs<=0）或偏差超过 [MAX_SKEW_MS] 时不做校正，退回旧行为。
 */
object Signer {

    /** 允许的服务端时间偏差上限，超过则视为 time 字段不可信。 */
    const val MAX_SKEW_MS: Long = 30L * 60 * 1000

    /** 带服务器时间校正的签名（每次上报都应使用上报时刻的 nowMs）。 */
    fun signWithServerTime(
        businessId: String,
        token: String,
        uid: String,
        nowMs: Long,
        localTs: Long,
        serverTs: Long
    ): String {
        val skew = serverTs - localTs
        val offset = if (serverTs > 0L && localTs > 0L && skew <= MAX_SKEW_MS && skew >= -MAX_SKEW_MS) skew else 0L
        val v20 = 10L * Math.floorDiv(nowMs + offset, 10000L)
        val plain = businessId + v20 + token.takeLast(8) + uid.takeLast(8) + Constants.SALT
        return md5(plain)
    }

    /** 不做时间校正（等价于 localTs == serverTs），用于固定向量测试与老逻辑对照。 */
    fun sign(businessId: String, token: String, uid: String, nowMs: Long): String =
        signWithServerTime(businessId, token, uid, nowMs, 0L, 0L)

    /** 使用本机当前时间、不做校正。 */
    fun signNow(businessId: String, token: String, uid: String): String =
        sign(businessId, token, uid, System.currentTimeMillis())

    fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return buildString(bytes.size * 2) {
            for (b in bytes) {
                val v = b.toInt() and 0xFF
                if (v < 0x10) append('0')
                append(Integer.toHexString(v))
            }
        }
    }
}
