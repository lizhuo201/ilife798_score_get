package com.isuncy.ilife798.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isuncy.ilife798.data.local.AccountStore
import com.isuncy.ilife798.data.model.Account
import com.isuncy.ilife798.data.remote.IlifApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class LoginUiState(
    val phone: String = "",
    val seed: Double = Random.nextDouble(),
    val captchaCode: String = "",
    val smsCode: String = "",
    val waitingSms: Boolean = false,
    val loading: Boolean = false,
    val message: String = "",
    val done: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun initPhone(phone: String) {
        if (phone.isNotEmpty() && _state.value.phone.isEmpty()) {
            _state.value = _state.value.copy(phone = phone)
        }
    }

    fun setPhone(v: String) { _state.value = _state.value.copy(phone = v) }
    fun setCaptchaCode(v: String) { _state.value = _state.value.copy(captchaCode = v) }
    fun setSmsCode(v: String) { _state.value = _state.value.copy(smsCode = v) }

    fun captchaUrl(): String = IlifApi.captchaUrl(_state.value.seed, System.currentTimeMillis())

    fun refreshCaptcha() {
        _state.value = _state.value.copy(seed = Random.nextDouble(), captchaCode = "")
    }

    fun submitGraphic() {
        val s = _state.value
        if (s.phone.isEmpty()) {
            _state.value = s.copy(message = "请输入手机号")
            return
        }
        if (s.captchaCode.isEmpty()) {
            _state.value = s.copy(message = "请输入图形验证码")
            return
        }
        _state.value = s.copy(loading = true, message = "")
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                IlifApi.sendLoginCode(s.phone, s.captchaCode, s.seed)
            }
            _state.value = _state.value.copy(
                loading = false,
                waitingSms = ok,
                message = if (ok) "短信验证码已下发" else "图形验证码错误或下发失败"
            )
        }
    }

    fun submitSms() {
        val s = _state.value
        if (s.smsCode.isEmpty()) {
            _state.value = s.copy(message = "请输入短信验证码")
            return
        }
        _state.value = s.copy(loading = true, message = "")
        viewModelScope.launch {
            val pair = withContext(Dispatchers.IO) { IlifApi.login(s.phone, s.smsCode) }
            if (pair != null) {
                val existing = AccountStore.load(getApplication())
                AccountStore.save(
                    getApplication(),
                    existing.filter { it.phone != s.phone } + Account(s.phone, pair.first, pair.second)
                )
                _state.value = s.copy(loading = false, done = true, message = "登录成功")
            } else {
                _state.value = s.copy(loading = false, message = "登录失败，请检查短信验证码")
            }
        }
    }
}
