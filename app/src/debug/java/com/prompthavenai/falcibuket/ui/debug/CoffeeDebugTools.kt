package com.prompthavenai.falcibuket.ui.debug

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.prompthavenai.falcibuket.data.remote.FortuneErrorCode
import com.prompthavenai.falcibuket.data.remote.ImageCompressor
import com.prompthavenai.falcibuket.data.remote.ImageProcessingException
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "FalcibuketCoffee"

/**
 * DEBUG-ONLY: aynı ön işleme hattını çalıştırır, Firebase/AI çağrısı YAPMAZ.
 * Bu dosya yalnızca `debug` source set'indedir; release build'de kodu bulunmaz.
 */
@Composable
fun CoffeePreprocessDebugTool(cup: Uri?, saucer: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    TextButton(
        onClick = {
            scope.launch {
                val report = validate(context.applicationContext as Application, cup, saucer)
                Toast.makeText(context, report, Toast.LENGTH_LONG).show()
            }
        },
        enabled = cup != null,
        modifier = modifier
    ) {
        Text("Ön işleme testi (debug)", color = TextMuted)
    }
}

private suspend fun validate(app: Application, cup: Uri?, saucer: Uri?): String {
    val cupResult = validateOne(app, "CUP", cup, required = true)
    val saucerResult = validateOne(app, "SAUCER", saucer, required = false)
    val report = "$cupResult | $saucerResult"
    Log.i(TAG, "COFFEE_PREPROCESS $report")
    return report
}

private suspend fun validateOne(app: Application, slot: String, uri: Uri?, required: Boolean): String {
    if (uri == null) return "$slot=${if (required) "FAIL_MISSING" else "SKIP"}"
    return try {
        val processed = withContext(Dispatchers.IO) { ImageCompressor.process(app, uri) }
        "$slot=PASS bytes=${processed.byteCount} dims=${processed.width}x${processed.height} mime=${processed.mime}"
    } catch (e: ImageProcessingException) {
        "$slot=FAIL code=${e.code} stage=${e.stage}"
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
        "$slot=FAIL code=${FortuneErrorCode.IMAGE_PROCESSING_FAILED} stage=UNKNOWN"
    }
}
