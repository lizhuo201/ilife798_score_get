package com.isuncy.ilife798

import com.isuncy.ilife798.core.MissionLogic
import com.isuncy.ilife798.core.TimeUtil
import com.isuncy.ilife798.data.model.MissionItem
import com.isuncy.ilife798.data.model.MissionsResult
import com.isuncy.ilife798.data.model.TaskPlan
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionLogicTest {

    // 2025-01-09 16:45 北京时间 → 周四（ISO 4），当日位图 = 1 shl 3 = 8
    private val nowMs = 1736412345678L
    private val todayBit = 1 shl (TimeUtil.isoWeekdayInBeijing(nowMs) - 1)

    private fun result(
        dailyAdId: String? = "DAILY_CHECK_IN",
        dailyLtime: Long = 0,
        dailyScore: Int = 5,
        dailyWeek: JsonPrimitive? = null,
        missions: List<MissionItem> = emptyList()
    ) = MissionsResult(
        score = 100,
        totalScore = 1000,
        dailyLtime = dailyLtime,
        dailyWeek = dailyWeek,
        dailyAdId = dailyAdId,
        dailyScore = dailyScore,
        missions = missions
    )

    private fun item(name: String, refId: String, score: Int, limit: Int, hasUrl: Boolean = false) =
        MissionItem(name, refId, score, limit, hasUrl)

    private fun plan(
        refId: String = "r",
        type: Int = 2,
        score: Int = 5,
        runsNeeded: Int = 1,
        progress: String = "0/1",
        isCheckIn: Boolean = false
    ) = TaskPlan("n", refId, score, type, runsNeeded, progress, isCheckIn)

    // ---- 过滤规则 ----

    @Test
    fun skipsZeroScoreTasks() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("零分任务", "zero", 0, 3))),
            emptyMap(), nowMs
        )
        assertTrue(q.none { it.refId == "zero" })
    }

    @Test
    fun skipsUrlTasks() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("带链接任务", "has_url", 5, 3, hasUrl = true))),
            emptyMap(), nowMs
        )
        assertTrue(q.none { it.refId == "has_url" })
    }

    @Test
    fun skipsEquityTasks() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("每日权益礼包", "equity", 5, 3))),
            emptyMap(), nowMs
        )
        assertTrue(q.none { it.refId == "equity" })
    }

    @Test
    fun skipsHighScoreTasks() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("高分任务", "big", 101, 3))),
            emptyMap(), nowMs
        )
        assertTrue(q.none { it.refId == "big" })
    }

    // ---- 限次 ----

    @Test
    fun zeroOrNegativeLimitBecomesFive() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(
                item("limit0", "l0", 5, 0),
                item("limitNeg", "lneg", 5, -1)
            )),
            emptyMap(), nowMs
        )
        assertEquals(5, q.first { it.refId == "l0" }.runsNeeded)
        assertEquals(5, q.first { it.refId == "lneg" }.runsNeeded)
    }

    @Test
    fun runsIsLimitMinusTodayDone() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("部分完成", "part", 5, 5))),
            mapOf("part" to 2), nowMs
        )
        val task = q.first { it.refId == "part" }
        assertEquals(3, task.runsNeeded)
        assertEquals("2/5", task.progress)
    }

    @Test
    fun fullyDoneTasksNotQueued() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("已完成", "done_all", 5, 3))),
            mapOf("done_all" to 3), nowMs
        )
        assertTrue(q.none { it.refId == "done_all" })
    }

    @Test
    fun overDoneTasksNotQueued() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("超额完成", "over", 5, 3))),
            mapOf("over" to 9), nowMs
        )
        assertTrue(q.none { it.refId == "over" })
    }

    // ---- 类型判定 ----

    @Test
    fun popsreenTypeIs4() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("全屏广告", "popsreen", 10, 1))),
            emptyMap(), nowMs
        )
        assertEquals(4, q.first { it.refId == "popsreen" }.type)
    }

    @Test
    fun nameAdOrFullscreenTypeIs4() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(
                item("观看广告", "ad_1", 10, 1),
                item("全屏任务", "fs_1", 10, 1)
            )),
            emptyMap(), nowMs
        )
        assertEquals(4, q.first { it.refId == "ad_1" }.type)
        assertEquals(4, q.first { it.refId == "fs_1" }.type)
    }

    @Test
    fun normalTypeIs2() {
        val q = MissionLogic.buildTaskQueue(
            result(missions = listOf(item("普通阅读", "read_x", 10, 1))),
            emptyMap(), nowMs
        )
        assertEquals(2, q.first { it.refId == "read_x" }.type)
    }

    // ---- 每日签到 ----

    @Test
    fun checkinQueuedWhenDailyAdIdPresent() {
        val q = MissionLogic.buildTaskQueue(result(), emptyMap(), nowMs)
        val task = q.first { it.refId == "DAILY_CHECK_IN" }
        assertEquals("每日签到", task.name)
        assertEquals(1, task.type)
        assertEquals(1, task.runsNeeded)
        assertEquals(5, task.score)
        assertTrue(task.isCheckIn)
    }

    @Test
    fun checkinUsesDailyScore() {
        val q = MissionLogic.buildTaskQueue(result(dailyScore = 8), emptyMap(), nowMs)
        assertEquals(8, q.first { it.refId == "DAILY_CHECK_IN" }.score)
    }

    @Test
    fun checkinSkippedWhenSignedTodayByLtime() {
        val q = MissionLogic.buildTaskQueue(result(dailyLtime = nowMs), emptyMap(), nowMs)
        assertTrue(q.none { it.refId == "DAILY_CHECK_IN" })
    }

    @Test
    fun checkinSkippedWhenSignedTodayByStream() {
        val q = MissionLogic.buildTaskQueue(
            result(), mapOf("DAILY_CHECK_IN" to 1), nowMs
        )
        assertTrue(q.none { it.refId == "DAILY_CHECK_IN" })
    }

    @Test
    fun checkinQueuedWhenLtimeIsYesterday() {
        val yesterday = nowMs - 24L * 3600 * 1000
        val q = MissionLogic.buildTaskQueue(result(dailyLtime = yesterday), emptyMap(), nowMs)
        assertEquals(1, q.count { it.refId == "DAILY_CHECK_IN" })
    }

    @Test
    fun checkinSkippedWhenWeekMaskHasTodayBit() {
        val q = MissionLogic.buildTaskQueue(
            result(dailyWeek = JsonPrimitive(todayBit)), emptyMap(), nowMs
        )
        assertTrue(q.none { it.refId == "DAILY_CHECK_IN" })
    }

    @Test
    fun checkinQueuedWhenWeekMaskBitIsAnotherDay() {
        val mondayOnly = 1
        assertTrue(todayBit != mondayOnly)
        val q = MissionLogic.buildTaskQueue(
            result(dailyWeek = JsonPrimitive(mondayOnly)), emptyMap(), nowMs
        )
        assertEquals(1, q.count { it.refId == "DAILY_CHECK_IN" })
    }

    @Test
    fun checkinToleratesNonNumericWeekMask() {
        val q = MissionLogic.buildTaskQueue(
            result(dailyWeek = JsonPrimitive("abc")), emptyMap(), nowMs
        )
        assertEquals(1, q.count { it.refId == "DAILY_CHECK_IN" })
    }

    @Test
    fun checkinSkippedWhenNoDailyAdId() {
        val q = MissionLogic.buildTaskQueue(result(dailyAdId = null), emptyMap(), nowMs)
        assertTrue(q.none { it.refId == "DAILY_CHECK_IN" })
    }

    @Test
    fun checkinSkippedWhenEmptyDailyAdId() {
        val q = MissionLogic.buildTaskQueue(result(dailyAdId = ""), emptyMap(), nowMs)
        assertTrue(q.none { it.refId == "DAILY_CHECK_IN" })
    }

    // ---- buildSendBody ----

    @Test
    fun missionBodyIsMinimalForAppChannelVariant() {
        val body = MissionLogic.buildSendBody(plan(refId = "read_x"), nowMs, MissionLogic.SEND_VARIANTS[0])
        assertEquals("read_x", (body["adId"] as JsonPrimitive).content)
        assertEquals("101", (body["type"] as JsonPrimitive).content)
        assertFalse(body.containsKey("weekDay"))
        assertFalse(body.containsKey("weekday"))
        assertFalse(body.containsKey("addScoreType"))
        assertFalse(body.containsKey("addScore"))
    }

    @Test
    fun missionBodyCarriesScoreForWebVariant() {
        val task = plan(refId = "read_x", score = 12)
        val body = MissionLogic.buildSendBody(task, nowMs, MissionLogic.SendVariant("1", true))
        assertEquals("read_x", (body["adId"] as JsonPrimitive).content)
        assertEquals("101", (body["type"] as JsonPrimitive).content)
        assertEquals("1", (body["addScoreType"] as JsonPrimitive).content)
        assertEquals("12", (body["addScore"] as JsonPrimitive).content)
        assertFalse(body.containsKey("weekDay"))
        assertFalse(body.containsKey("weekday"))
    }

    @Test
    fun checkinBodyUsesWeekDayOnlyForEveryVariant() {
        val task = plan(refId = "DAILY_CHECK_IN", type = 1, isCheckIn = true)
        for (variant in MissionLogic.SEND_VARIANTS) {
            val body = MissionLogic.buildSendBody(task, nowMs, variant)
            assertEquals("DAILY_CHECK_IN", (body["adId"] as JsonPrimitive).content)
            assertEquals("4", (body["weekDay"] as JsonPrimitive).content)
            assertFalse(body.containsKey("type"))
            assertFalse(body.containsKey("addScore"))
            assertFalse(body.containsKey("addScoreType"))
        }
    }

    @Test
    fun sendVariantsCoverBothFlagAndBodyStyles() {
        assertEquals(4, MissionLogic.SEND_VARIANTS.size)
        assertTrue(MissionLogic.SEND_VARIANTS.any { it.s == "1" && !it.includeScore })
        assertTrue(MissionLogic.SEND_VARIANTS.any { it.s == "true" && !it.includeScore })
        assertTrue(MissionLogic.SEND_VARIANTS.any { it.includeScore })
        assertEquals("s=1 极简body", MissionLogic.SEND_VARIANTS[0].label)
    }
}
