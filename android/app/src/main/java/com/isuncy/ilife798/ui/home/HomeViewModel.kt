package com.isuncy.ilife798.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isuncy.ilife798.data.local.AccountStore
import com.isuncy.ilife798.data.model.Account
import com.isuncy.ilife798.data.remote.IlifApi
import com.isuncy.ilife798.data.remote.Parsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccountItem(
    val account: Account,
    val valid: Boolean? = null,
    val score: Int? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _accounts = MutableStateFlow<List<AccountItem>>(emptyList())
    val accounts: StateFlow<List<AccountItem>> = _accounts.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val list = AccountStore.load(getApplication())
        _accounts.value = list.map { AccountItem(it, valid = null, score = null) }
        list.forEach { account -> checkAccount(account) }
    }

    private fun checkAccount(account: Account) {
        viewModelScope.launch {
            val valid = withContext(Dispatchers.IO) { IlifApi.verifySession(account.token) }
            val score = if (valid) {
                withContext(Dispatchers.IO) {
                    IlifApi.getMissions(account.token)?.let { Parsers.parseMissions(it)?.score }
                }
            } else null
            _accounts.value = _accounts.value.map {
                if (it.account.phone == account.phone) it.copy(valid = valid, score = score) else it
            }
        }
    }

    fun remove(phone: String) {
        val kept = AccountStore.load(getApplication()).filter { it.phone != phone }
        AccountStore.save(getApplication(), kept)
        _accounts.value = _accounts.value.filter { it.account.phone != phone }
    }

    fun importJson(text: String): Boolean {
        return try {
            val parsed = AccountStore.parse(text)
            val existing = AccountStore.load(getApplication())
            val merged = (existing + parsed).distinctBy { it.phone }
            AccountStore.save(getApplication(), merged)
            refresh()
            true
        } catch (e: Exception) {
            _importError.value = "导入失败：文件格式不正确"
            false
        }
    }

    fun addOrReplace(account: Account) {
        val existing = AccountStore.load(getApplication())
        val merged = existing.filter { it.phone != account.phone } + account
        AccountStore.save(getApplication(), merged)
        refresh()
    }

    fun clearImportError() {
        _importError.value = null
    }
}
