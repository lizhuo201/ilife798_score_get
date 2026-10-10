package com.isuncy.ilife798.core

import java.security.MessageDigest

/**
 * 签名算法与 auto_score.py 的 get_sign 完全一致：
 * md5(businessId + n + token[-8:] + uid[-8:] + SALT)，其中 n = (当前毫秒 // 10000) * 10。
 */
object Signer {

    fun sign(businessId: String, token: String, uid: String, nowMs: Long): String {
        val token8 = token.takeLast(8)
        val uid8 = uid.takeLast(8)
        val n = (nowMs / 10000) * 10
        val plain = businessId + n + token8 + uid8 + Constants.SALT
        return md5(plain)
    }

    /** 供业务层调用：使用当前时间。 */
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
