package com.isuncy.ilife798.core

import java.util.Calendar
import java.util.TimeZone

/** 无 Android 依赖的时间工具，便于 JVM 单元测试。 */
object TimeUtil {

    /** 业务时间统一按北京时间（服务端时区）计算星期。 */
    private val BEIJING: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

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

    /** 北京时间下的 ISO 星期：周一=1 ... 周日=7（每日签到的 weekDay 字段）。 */
    fun isoWeekdayInBeijing(nowMs: Long): Int {
        val dow = Calendar.getInstance(BEIJING).apply { timeInMillis = nowMs }.get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 7 else dow - 1
    }

    /**
     * 归一化服务端 time 字段为毫秒：10 位按秒 ×1000，其余视为已经是毫秒；
     * 缺失/非法（<=0）返回 0，表示不做时间校正。
     */
    fun normalizeServerTs(raw: Long): Long = when {
        raw <= 0L -> 0L
        raw < 100_000_000_000L -> raw * 1000L
        else -> raw
    }
}
