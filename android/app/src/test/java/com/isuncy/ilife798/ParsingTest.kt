package com.isuncy.ilife798

import com.isuncy.ilife798.data.remote.Parsers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsingTest {

    private fun json(s: String): JsonObject = Json.parseToJsonElement(s) as JsonObject

    // ---- isSuccessCode / isCodeZero ----

    @Test
    fun successCodes() {
        assertTrue(Parsers.isSuccessCode(json("{\"code\":\"0\"}")))
        assertTrue(Parsers.isSuccessCode(json("{\"code\":200}")))
        assertTrue(Parsers.isSuccessCode(json("{\"status\":\"0\"}")))
        assertTrue(Parsers.isSuccessCode(json("{\"status\":\"200\"}")))
        assertTrue(Parsers.isSuccessCode(json("{\"code\":null}")))
    }

    @Test
    fun failureCodes() {
        assertFalse(Parsers.isSuccessCode(json("{\"code\":\"401\"}")))
        assertFalse(Parsers.isSuccessCode(json("{\"code\":1}")))
        assertFalse(Parsers.isSuccessCode(json("{\"code\":\"\"}")))
        assertFalse(Parsers.isSuccessCode(json("{\"status\":\"500\"}")))
    }

    @Test
    fun codeZeroStrict() {
        assertTrue(Parsers.isCodeZero(json("{\"code\":\"0\"}")))
        assertTrue(Parsers.isCodeZero(json("{\"code\":0}")))
        assertFalse(Parsers.isCodeZero(json("{\"code\":\"200\"}")))
        assertFalse(Parsers.isCodeZero(json("{}")))
    }

    // ---- parseLogin ----

    @Test
    fun parseLoginExtractsTokenAndUid() {
        val j = json("{\"code\":\"0\",\"data\":{\"al\":{\"token\":\"tok123\",\"uid\":\"uid456\"}}}")
        val p = Parsers.parseLogin(j)
        assertEquals("tok123", p?.first)
        assertEquals("uid456", p?.second)
    }

    @Test
    fun parseLoginNullWhenMissing() {
        assertNull(Parsers.parseLogin(json("{\"code\":\"1\",\"data\":{}}")))
        assertNull(Parsers.parseLogin(json("{}")))
    }

    // ---- parseMissions ----

    @Test
    fun parseMissionsReadsFields() {
        val j = json("""
            {"data":{"accScoreRsp":{"score":320,"totalScore":888,"daily":{"ltime":1736412345678,"week":"3"}},
             "dailyRSP":{"adId":"DAILY_CHECK_IN","score":5},
             "missions":[{"name":"阅读文章","refId":"read_x","score":10,"limit":5},
                          {"name":"带链接","refId":"link_x","score":10,"limit":5,"url":"http://x"},
                          {"name":"字符串分数","refId":"str_x","score":"7","limit":"3"}]}}
        """.trimIndent())
        val r = Parsers.parseMissions(j)!!
        assertEquals(320, r.score)
        assertEquals(888, r.totalScore)
        assertEquals(1736412345678L, r.dailyLtime)
        assertEquals("3", (r.dailyWeek as JsonPrimitive).content)
        assertEquals("DAILY_CHECK_IN", r.dailyAdId)
        assertEquals(5, r.dailyScore)
        assertEquals(3, r.missions.size)
        assertEquals("read_x", r.missions[0].refId)
        assertEquals(10, r.missions[0].score)
        assertEquals(5, r.missions[0].limit)
        assertFalse(r.missions[0].hasUrl)
        assertTrue(r.missions[1].hasUrl)
        assertEquals(7, r.missions[2].score)
        assertEquals(3, r.missions[2].limit)
    }

    @Test
    fun parseMissionsToleratesMissingData() {
        val r = Parsers.parseMissions(json("{}"))
        assertEquals(0, r.score)
        assertEquals(0, r.totalScore)
        assertTrue(r.missions.isEmpty())
        assertNull(r.dailyAdId)
        val r2 = Parsers.parseMissions(json("{\"data\":123}"))
        assertEquals(0, r2.score)
        assertEquals(0, r2.totalScore)
    }

    // ---- parseTodayCounts ----

    @Test
    fun parseTodayCountsFromList() {
        val zero = 1736352000000L
        val j = json("""
            {"data":[{"ctime":1736412345678,"data":{"adId":"A"}},
                      {"ctime":1736412345679,"data":{"adId":"A"}},
                      {"ctime":1736412345680,"adId":"B"},
                      {"ctime":1736000000000,"data":{"adId":"OLD"}}]}
        """.trimIndent())
        val counts = Parsers.parseTodayCounts(j, zero)
        assertEquals(2, counts["A"])
        assertEquals(1, counts["B"])
        assertFalse(counts.containsKey("OLD"))
    }

    @Test
    fun parseTodayCountsFromNestedList() {
        val zero = 0L
        val j1 = json("{\"data\":{\"list\":[{\"ctime\":1000,\"data\":{\"adId\":\"A\"}}]}}")
        assertEquals(1, Parsers.parseTodayCounts(j1, zero)["A"])

        val j2 = json("{\"data\":{\"records\":[{\"ctime\":1000,\"adId\":\"B\"}]}}")
        assertEquals(1, Parsers.parseTodayCounts(j2, zero)["B"])
    }

    @Test
    fun parseTodayCountsToleratesBadShape() {
        assertTrue(Parsers.parseTodayCounts(json("{\"data\":{}}"), 0L).isEmpty())
        assertTrue(Parsers.parseTodayCounts(json("{}"), 0L).isEmpty())
        assertTrue(Parsers.parseTodayCounts(json("{\"data\":[1,2]}"), 0L).isEmpty())
    }
}
