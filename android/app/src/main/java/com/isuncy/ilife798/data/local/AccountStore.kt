package com.isuncy.ilife798.data.local

import android.content.Context
import com.isuncy.ilife798.core.Constants
import com.isuncy.ilife798.data.model.Account
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 账号凭证的本地 JSON 读写，与 Python 版 ilife_accounts.json 格式一致。 */
object AccountStore {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun file(context: Context): File = File(context.filesDir, Constants.ACCOUNTS_FILE)

    fun load(context: Context): List<Account> = try {
        val f = file(context)
        if (!f.exists()) emptyList() else json.decodeFromString<List<Account>>(f.readText())
    } catch (e: Exception) {
        emptyList()
    }

    fun save(context: Context, accounts: List<Account>) {
        file(context).writeText(json.encodeToString(accounts))
    }

    /** 解析导入的凭证 JSON（兼容 Python 版 ilife_accounts.json）。 */
    fun parse(text: String): List<Account> = json.decodeFromString<List<Account>>(text)
}
