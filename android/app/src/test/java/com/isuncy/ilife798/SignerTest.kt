package com.isuncy.ilife798

import com.isuncy.ilife798.core.Signer
import org.junit.Assert.assertEquals
import org.junit.Test

class SignerTest {

    private data class Vector(
        val businessId: String,
        val token: String,
        val uid: String,
        val nowMs: Long,
        val expected: String
    )

    // 与 auto_score.py 实际输出核对过的固定向量（2026-10-10 验证）
    private val vectors = listOf(
        Vector("DAILY_CHECK_IN", "t0k3n123456789", "12345678901", 1736412345678, "3f7eea53fe229163216154bd6c3ef71c"),
        Vector("read_x", "short", "u", 1736412300000, "0458f345a0f47e0f16791116fd3ad045"),
        Vector("popsreen", "", "987654321", 1700000000000, "da721fab9cabbeb4277ba52d9c45439a"),
        Vector("sign_extra", "aaaaaaaa", "bbbbbbbb", 1700000009999, "33c54936a2e10cefe8dd79a4bfdaab63"),
        Vector("DAILY_CHECK_IN", "tok", "uid123", 1750000000123, "dc01c5b72e66bd7d62f1e8e161e2c75b")
    )

    @Test
    fun fixedVectorsMatchPython() {
        for (v in vectors) {
            assertEquals(
                "vector " + v.businessId + " @" + v.nowMs,
                v.expected,
                Signer.sign(v.businessId, v.token, v.uid, v.nowMs)
            )
        }
    }

    @Test
    fun md5KnownAnswer() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Signer.md5("abc"))
    }

    @Test
    fun md5OutputIsLowercaseHex32() {
        val out = Signer.md5("hello")
        assertEquals(32, out.length)
        assertEquals(out, out.lowercase())
    }
}
