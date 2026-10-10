package com.isuncy.ilife798.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.isuncy.ilife798.data.local.HistoryStore
import com.isuncy.ilife798.data.model.HistoryRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _records = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val records: StateFlow<List<HistoryRecord>> = _records.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _records.value = HistoryStore.load(getApplication())
    }

    fun clear() {
        HistoryStore.clear(getApplication())
        reload()
    }
}
