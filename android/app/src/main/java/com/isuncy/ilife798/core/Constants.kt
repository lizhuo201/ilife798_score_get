package com.isuncy.ilife798.core

/** 与 Python 版 auto_score.py 保持一致的协议常量与执行参数。 */
object Constants {
    const val SALT = "aslkdvcniu34h9tgufh278wv2"

    // nocookies111/life-798 v5.4.4 APK（classes.dex）内实际携带的两枚签名盐：
    // 分支A（账户模式，AppType "1,1" + version "3.1.9"，s=1，服务器时间校正）
    const val SALT_NC_A = "AbCdEfGhIjKlMnOpQrStUvWxYz12345"
    // 分支B（设备模式，AppType "1,5" + version "2.0.178"，不带 s，本机时间）
    const val SALT_NC_B = "K9xP2QnR5T8wY3fA7cE1gJ4mL6oU0sZ"

    const val BASE_URL = "https://i.ilife798.com"
    const val VERSION_CODE = "3.1.7"
    const val VERSION_NUM = "31007"
    const val USER_AGENT = "Android_ilife798_$VERSION_CODE"

    // nocookies111/life-798 v5.4.4 官方 App 渠道（分支A）实际请求头：
    // ApplicationType=1,1 + VersionCode=3.1.9（注意是版本字符串，不是 31009）
    const val NC_VERSION_A = "3.1.9"
    const val NC_UA_A = "Android_ilife798_3.1.9"
    // 分支B（WaterWidget 设备渠道）实际请求头
    const val NC_VERSION_B = "2.0.178"
    const val NC_UA_B = "WaterWidget/5.4.4 (Android)"

    const val ACCOUNTS_FILE = "ilife_accounts.json"
    const val HISTORY_FILE = "history.json"

    // 执行节奏（与 Python 版一致）
    const val MIN_RUN_DELAY_MS = 12_000L
    const val MAX_RUN_DELAY_MS = 18_000L
    const val MIN_ACCOUNT_DELAY_MS = 20_000L
    const val MAX_ACCOUNT_DELAY_MS = 45_000L

    // 任务规则默认值
    const val DEFAULT_MISSION_LIMIT = 5
    const val DEFAULT_CHECKIN_SCORE = 5
    const val DEFAULT_CHECKIN_AD_ID = "DAILY_CHECK_IN"

    // score-send 参数组合探测：失败时换用其它组合，被限流则稍后重试同一组合
    const val PROBE_DELAY_MS = 5_000L
    const val RATE_LIMIT_CODE = "-98"
    // 被限流后等 60s 再试一次；仍被限流则中止该账号（避免继续消耗尝试次数）
    const val RATE_LIMIT_DELAY_MS = 60_000L
    const val MAX_RATE_LIMIT_RETRY = 1

    const val TASK_TYPE_CHECKIN = 1
    const val TASK_TYPE_NORMAL = 2
    const val TASK_TYPE_AD = 4
}
