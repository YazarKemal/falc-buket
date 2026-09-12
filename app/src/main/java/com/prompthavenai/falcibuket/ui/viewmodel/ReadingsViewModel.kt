package com.prompthavenai.falcibuket.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prompthavenai.falcibuket.data.model.ReadingEntry
import com.prompthavenai.falcibuket.data.remote.BuketBackend
import com.prompthavenai.falcibuket.data.remote.FortuneError
import com.prompthavenai.falcibuket.data.remote.FortuneErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadingsUiState(
    val loading: Boolean = false,
    val readings: List<ReadingEntry> = emptyList(),
    val backendConfigured: Boolean = true,
    val error: FortuneError? = null
)

class ReadingsViewModel(private val backend: BuketBackend = ChatViewModel.defaultBackend()) : ViewModel() {

    private val _state = MutableStateFlow(ReadingsUiState())
    val state: StateFlow<ReadingsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val readings = backend.readings()
                _state.value = ReadingsUiState(readings = readings, backendConfigured = true)
            } catch (t: Throwable) {
                val err = FortuneErrorMapper.fromThrowable(t)
                val configured = err.code != com.prompthavenai.falcibuket.data.remote.FortuneErrorCode.AUTH_ERROR
                _state.value = ReadingsUiState(readings = emptyList(), backendConfigured = configured, error = err)
            }
        }
    }

    fun typeImage(type: String): Int {
        val fortune = com.prompthavenai.falcibuket.ui.model.FortuneCatalog.byId(type)
            ?: com.prompthavenai.falcibuket.ui.model.FortuneCatalog.fromLegacy(type)
        return fortune?.artworkRes ?: com.prompthavenai.falcibuket.R.drawable.result_background
    }
}
