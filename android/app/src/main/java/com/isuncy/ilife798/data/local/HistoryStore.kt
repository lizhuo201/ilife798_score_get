package com.isuncy.ilife798.data.local

import android.content.Context
import com.isuncy.ilife798.core.Constants
import com.isuncy.ilife798.data.model.HistoryRecord
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 执行历史记录，存 filesDir/history.json。 */
object HistoryStore {

    private const val MAX_RECORDS = 100

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun file(context: Context): File = File(context.filesDir, Constants.HISTORY_FILE)

    fun load(context: Context): List<HistoryRecord> = try {
        val f = file(context)
        if (!f.exists()) emptyList() else json.decodeFromString<List<HistoryRecord>>(f.readText())
    } catch (e: Exception) {
        emptyList()
    }

    /** 新记录插到最前，最多保留 100 条。 */
    fun append(context: Context, record: HistoryRecord): List<HistoryRecord> {
        val list = (listOf(record) + load(context)).take(MAX_RECORDS)
        save(context, list)
        return list
    }

    fun save(context: Context, records: List<HistoryRecord>) {
        file(context).writeText(json.encodeToString(records))
    }

    /** 清空历史。 */
    fun clear(context: Context) {
        file(context).delete()
    }
}
