package com.prompthavenai.falcibuket.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prompthavenai.falcibuket.data.model.CoffeeResult
import com.prompthavenai.falcibuket.data.remote.BuketBackend
import com.prompthavenai.falcibuket.data.remote.FortuneError
import com.prompthavenai.falcibuket.data.remote.FortuneErrorMapper
import com.prompthavenai.falcibuket.data.remote.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CoffeeUiState {
    data object Idle : CoffeeUiState
    data class Loading(val stageIndex: Int = 0) : CoffeeUiState
    data class Success(val result: CoffeeResult) : CoffeeUiState
    data class Error(val error: FortuneError) : CoffeeUiState
}

/**
 * Activity kapsamında paylaşılır: CoffeeScreen analizi başlatır,
 * ReadingResultScreen sonucu gösterir.
 */
class CoffeeViewModel @JvmOverloads constructor(
    application: Application,
    private val backend: BuketBackend = ChatViewModel.defaultBackend()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<CoffeeUiState>(CoffeeUiState.Idle)
    val state: StateFlow<CoffeeUiState> = _state.asStateFlow()

    private var lastCup: Uri? = null
    private var lastSaucer: Uri? = null

    fun analyze(cupUri: Uri, saucerUri: Uri?, question: String? = null) {
        if (_state.value is CoffeeUiState.Loading) return
        lastCup = cupUri
        lastSaucer = saucerUri
        _state.value = CoffeeUiState.Loading(0)
        viewModelScope.launch {
            try {
                val cupB64 = withContext(Dispatchers.IO) { encode(cupUri) }
                val saucerB64 = saucerUri?.let { withContext(Dispatchers.IO) { encode(it) } }
                withContext(Dispatchers.Main) { if (_state.value is CoffeeUiState.Loading) _state.value = CoffeeUiState.Loading(1) }
                val result = backend.analyzeCoffee(cupB64, saucerB64, question)
                withContext(Dispatchers.Main) { if (_state.value is CoffeeUiState.Loading) _state.value = CoffeeUiState.Loading(2) }
                _state.value = CoffeeUiState.Success(result)
            } catch (t: Throwable) {
                _state.value = CoffeeUiState.Error(FortuneErrorMapper.fromThrowable(t))
            }
        }
    }

    fun retry() {
        val cup = lastCup ?: return
        analyze(cup, lastSaucer)
    }

    private suspend fun encode(uri: Uri): String {
        val bytes = ImageCompressor.compressToJpeg(getApplication(), uri)
        // Kamera geçici dosyalarını (FileProvider cache) analiz sonrası temizle.
        if (uri.scheme == "file") {
            withContext(Dispatchers.IO) {
                runCatching { java.io.File(uri.path!!).delete() }
            }
        }
        return ImageCompressor.toBase64(bytes)
    }

    fun reset() {
        _state.value = CoffeeUiState.Idle
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CoffeeViewModel(app) as T
    }
}
