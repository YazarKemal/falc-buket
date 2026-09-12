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
import com.prompthavenai.falcibuket.data.remote.FortuneErrorCode
import com.prompthavenai.falcibuket.data.remote.FortuneErrorMapper
import com.prompthavenai.falcibuket.data.remote.ImageCompressor
import com.prompthavenai.falcibuket.data.remote.ImageProcessingException
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
        // Kamera geçici dosyalarını yalnızca başarılı işleme sonrası temizle.
        if (uri.scheme == "file") {
            withContext(Dispatchers.IO) {
                runCatching { uri.path?.let { java.io.File(it).delete() } }
            }
        }
        return ImageCompressor.toBase64(processed.jpeg)
    }

    /**
     * DEBUG-ONLY: Aynı ön işleme hattını çalıştırır ama Firebase çağrısı YAPMAZ.
     * Cup ve tabak bağımsız raporlanır; hiçbir zaman callable çağrılmaz.
     */
    suspend fun validatePreprocessing(cupUri: Uri?, saucerUri: Uri?): String {
        val cup = validateOne("CUP", cupUri, required = true)
        val saucer = validateOne("SAUCER", saucerUri, required = false)
        val report = "$cup | $saucer"
        Log.i(TAG, "COFFEE_PREPROCESS $report")
        return report
    }

    private suspend fun validateOne(slot: String, uri: Uri?, required: Boolean): String {
        if (uri == null) return "$slot=${if (required) "FAIL_MISSING" else "SKIP"}"
        return try {
            val p = withContext(Dispatchers.IO) { ImageCompressor.process(getApplication(), uri) }
            "$slot=PASS bytes=${p.byteCount} dims=${p.width}x${p.height} mime=${p.mime}"
        } catch (e: ImageProcessingException) {
            "$slot=FAIL code=${e.code} stage=${e.stage}"
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            "$slot=FAIL code=${FortuneErrorCode.IMAGE_PROCESSING_FAILED} stage=UNKNOWN"
        }
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
