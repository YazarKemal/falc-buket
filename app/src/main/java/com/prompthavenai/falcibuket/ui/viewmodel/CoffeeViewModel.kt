package com.prompthavenai.falcibuket.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prompthavenai.falcibuket.data.model.CoffeeResult
import com.prompthavenai.falcibuket.data.remote.BuketBackend
import com.prompthavenai.falcibuket.data.remote.FortuneError
import com.prompthavenai.falcibuket.data.remote.FortuneErrorMapper
import com.prompthavenai.falcibuket.data.remote.ImageCompressor
import com.prompthavenai.falcibuket.data.remote.ImageProcessingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    // Ekran yaşam döngüsünden bağımsız geçici kamera dosyaları. Retry çalışsın
    // diye ekran dispose olduğunda DEĞİL, ViewModel temizlenince silinir.
    private val tempFiles = mutableListOf<File>()

    /** Kamera ile üretilen geçici dosyayı ViewModel sahipliğine alır. */
    fun trackTempFile(file: File) {
        synchronized(tempFiles) { tempFiles.add(file) }
    }

    override fun onCleared() {
        synchronized(tempFiles) {
            tempFiles.forEach { runCatching { it.delete() } }
            tempFiles.clear()
        }
        super.onCleared()
    }

    fun analyze(cupUri: Uri, saucerUri: Uri?, question: String? = null) {
        if (_state.value is CoffeeUiState.Loading) return
        lastCup = cupUri
        lastSaucer = saucerUri
        _state.value = CoffeeUiState.Loading(0)
        viewModelScope.launch {
            try {
                // Tüm görsel ön işleme Firebase çağrısından ÖNCE tamamlanır.
                val cupB64 = encode(cupUri, "CUP")
                val saucerB64 = saucerUri?.let { encode(it, "SAUCER") }
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

    private suspend fun encode(uri: Uri, slot: String): String {
        val processed = try {
            withContext(Dispatchers.IO) { ImageCompressor.process(getApplication(), uri) }
        } catch (e: ImageProcessingException) {
            Log.w(TAG, "COFFEE_IMAGE_STAGE slot=$slot stage=${e.stage} success=false code=${e.code}")
            throw ImageProcessingException(e.code, e.stage, slot, e)
        }
        Log.i(
            TAG,
            "COFFEE_IMAGE_STAGE slot=$slot stage=BASE64_READY success=true bytes=${processed.byteCount} " +
                "width=${processed.width} height=${processed.height} mime=${processed.mime}"
        )
        // Kaynak dosya burada SİLİNMEZ; ön işleme tamamlanır ve retry için dosya
        // geçerli kalır. Temizlik ViewModel.onCleared() tarafından yapılır.
        return ImageCompressor.toBase64(processed.jpeg)
    }

    fun reset() {
        _state.value = CoffeeUiState.Idle
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CoffeeViewModel(app) as T
    }

    companion object {
        private const val TAG = "FalcibuketCoffee"
    }
}
