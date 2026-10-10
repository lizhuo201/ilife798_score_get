package com.isuncy.ilife798.core

/** 与 Python 版 auto_score.py 保持一致的协议常量与执行参数。 */
object Constants {
    const val SALT = "aslkdvcniu34h9tgufh278wv2"
    const val BASE_URL = "https://i.ilife798.com"
    const val VERSION_CODE = "3.1.7"
    const val VERSION_NUM = "31007"
    const val USER_AGENT = "Android_ilife798_$VERSION_CODE"

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

    const val TASK_TYPE_CHECKIN = 1
    const val TASK_TYPE_NORMAL = 2
    const val TASK_TYPE_AD = 4
}
