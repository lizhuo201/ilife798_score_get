package com.isuncy.ilife798.core

import android.content.Context

/** 首次启动免责声明同意状态（SharedPreferences；卸载或清除数据后重新询问） */
object AgreementStore {

    private const val PREFS = "ilife798_prefs"
    private const val KEY_AGREED = "disclaimer_agreed"

    fun isAgreed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AGREED, false)

    fun setAgreed(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AGREED, true)
            .apply()
    }
}
