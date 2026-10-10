package com.isuncy.ilife798.core

import java.util.Calendar

/** 无 Android 依赖的本地日期工具，便于 JVM 单元测试。 */
object TimeUtil {

    /** 给定毫秒时间戳所在自然日的 00:00:00 毫秒时间戳（系统时区）。 */
    fun startOfTodayMs(nowMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** 两个时间戳是否处于同一自然日（系统时区）。 */
    fun isSameLocalDay(aMs: Long, bMs: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = aMs }
        val b = Calendar.getInstance().apply { timeInMillis = bMs }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
