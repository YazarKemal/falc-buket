package com.prompthavenai.falcibuket.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prompthavenai.falcibuket.data.remote.BuketBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MemoryUiState(
    val facts: List<String> = emptyList(),
    val clearing: Boolean = false,
    val cleared: Boolean = false
)

class MemoryViewModel(private val backend: BuketBackend = ChatViewModel.defaultBackend()) : ViewModel() {

    private val _state = MutableStateFlow(MemoryUiState())
    val state: StateFlow<MemoryUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val facts = runCatching { backend.memoryFacts() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(facts = facts)
        }
    }

    fun clearMemory() {
        if (_state.value.clearing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(clearing = true)
            val ok = runCatching { backend.clearMemory() }.isSuccess
            _state.value = MemoryUiState(facts = emptyList(), clearing = false, cleared = ok)
        }
    }

    fun consumeCleared() {
        _state.value = _state.value.copy(cleared = false)
    }
}
