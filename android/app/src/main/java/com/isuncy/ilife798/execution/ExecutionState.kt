package com.isuncy.ilife798.execution

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 本次执行的共享状态：UI 与服务都观察它。 */
object ExecutionState {

    private const val MAX_LOG_LINES = 500

    data class UiState(
        val running: Boolean = false,
        val phone: String = "",
        val accountDone: Int = 0,
        val accountTotal: Int = 0,
        val taskName: String = "",
        val successSends: Int = 0,
        val failedSends: Int = 0,
        val beforeScore: Int = 0,
        val afterScore: Int = 0,
        val logs: List<String> = emptyList(),
        val stopRequested: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun update(transform: (UiState) -> UiState) {
        _state.value = transform(_state.value)
    }

    fun log(line: String) {
        update { it.copy(logs = (it.logs + line).takeLast(MAX_LOG_LINES)) }
    }

    fun reset() {
        _state.value = UiState()
    }

    fun setRunning(running: Boolean) {
        update { it.copy(running = running) }
    }

    fun requestStop() {
        update { it.copy(stopRequested = true) }
    }
}
