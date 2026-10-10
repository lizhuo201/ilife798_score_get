package com.isuncy.ilife798

import com.isuncy.ilife798.core.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeUtilTest {

    @Test
    fun isoWeekdayUsesBeijingTime() {
        // 2025-01-05 20:40 UTC = 2025-01-06 04:40 北京时间 → 周一
        assertEquals(1, TimeUtil.isoWeekdayInBeijing(1736109600000L))
        // 2025-01-09 16:45 北京时间 → 周四
        assertEquals(4, TimeUtil.isoWeekdayInBeijing(1736412345678L))
        // 2025-01-04 22:13 北京时间 → 周六
        assertEquals(6, TimeUtil.isoWeekdayInBeijing(1736000000000L))
    }

    @Test
    fun normalizeServerTsHandlesMillisSecondsAndInvalidValues() {
        assertEquals(1760140001500L, TimeUtil.normalizeServerTs(1760140001500L))
        assertEquals(1760140000000L, TimeUtil.normalizeServerTs(1760140000L))
        assertEquals(0L, TimeUtil.normalizeServerTs(0L))
        assertEquals(0L, TimeUtil.normalizeServerTs(-1L))
    }

    @Test
    fun startOfTodayIsBeforeNowAndSameDay() {
        val now = 1736412345678L
        val zero = TimeUtil.startOfTodayMs(now)
        assertTrue(zero <= now)
        assertTrue(TimeUtil.isSameLocalDay(zero, now))
        assertTrue(TimeUtil.isSameLocalDay(zero, zero + 1000L))
    }
}
