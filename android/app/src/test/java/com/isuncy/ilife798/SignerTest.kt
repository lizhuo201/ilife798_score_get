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

    /** nowMs / localTs / serverTs 三元组，覆盖服务端时间校正后的签名。 */
    private data class SkewVector(
        val businessId: String,
        val token: String,
        val uid: String,
        val nowMs: Long,
        val localTs: Long,
        val serverTs: Long,
        val expected: String
    )

    // 与 auto_score.py 旧算法实际输出核对过的固定向量（2026-10-10 验证）；
    // 新算法在 localTs == serverTs 时与旧算法完全等值，故这些向量仍然有效。
    private val vectors = listOf(
        Vector("DAILY_CHECK_IN", "t0k3n123456789", "12345678901", 1736412345678, "3f7eea53fe229163216154bd6c3ef71c"),
        Vector("read_x", "short", "u", 1736412300000, "0458f345a0f47e0f16791116fd3ad045"),
        Vector("popsreen", "", "987654321", 1700000000000, "da721fab9cabbeb4277ba52d9c45439a"),
        Vector("sign_extra", "aaaaaaaa", "bbbbbbbb", 1700000009999, "33c54936a2e10cefe8dd79a4bfdaab63"),
        Vector("DAILY_CHECK_IN", "tok", "uid123", 1750000000123, "dc01c5b72e66bd7d62f1e8e161e2c75b")
    )

    // 官方 App 3.1.7 签名算法（服务器时间校正）参考实现生成的固定向量
    private val skewVectors = listOf(
        SkewVector(
            "read_x", "t0k3n123456789", "9876543210123456",
            1760140000000L, 1760139999000L, 1760140001500L, "c4998acff81d33cd6b316fef7a4add8e"
        ),
        SkewVector(
            "watch_video", "eyJhbGciOiJIUzI1NiJ9.tokenpart", "8f3c1d2e4b5a6978",
            1760140123456L, 1760140121000L, 1760140122000L, "48f0958cf38d52135d0c0418f1a93302"
        ),
        SkewVector(
            "popsreen", "", "",
            1700000000000L, 1700000000000L, 1700000000000L, "ce054ec59e0ad22257e826e930bcad1d"
        ),
        SkewVector(
            "DAILY_CHECK_IN", "tok", "uid123",
            1750000000123L, 0L, 1750000000123L, "dc01c5b72e66bd7d62f1e8e161e2c75b"
        ),
        SkewVector(
            "big_skew_task", "abcdefgh12345678", "8765432109876543",
            1760140200000L, 1760130000000L, 1760140200000L, "be4359e05b5c0dcd140fe75d5ca480e0"
        )
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
    fun serverTimeVectorsMatchReference() {
        for (v in skewVectors) {
            assertEquals(
                "vector " + v.businessId + " @" + v.nowMs,
                v.expected,
                Signer.signWithServerTime(v.businessId, v.token, v.uid, v.nowMs, v.localTs, v.serverTs)
            )
        }
    }

    @Test
    fun noCorrectionWhenServerTimeMissingOrLocalTimeUnknown() {
        val base = Signer.sign("read_x", "t0k3n123456789", "9876543210123456", 1760140000000L)
        assertEquals(base, Signer.signWithServerTime("read_x", "t0k3n123456789", "9876543210123456", 1760140000000L, 0L, 1760140001500L))
        assertEquals(base, Signer.signWithServerTime("read_x", "t0k3n123456789", "9876543210123456", 1760140000000L, 1760139999000L, 0L))
    }

    @Test
    fun largeSkewIsIgnored() {
        // 偏差 1 小时 > 30 分钟阈值 → 忽略 time 字段，退回本机时间
        val now = 1700000000000L
        assertEquals(
            "ce054ec59e0ad22257e826e930bcad1d",
            Signer.signWithServerTime("popsreen", "", "", now, now - 3600000L, now)
        )
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
