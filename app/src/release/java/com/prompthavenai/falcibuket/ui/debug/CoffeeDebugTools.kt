package com.prompthavenai.falcibuket.ui.debug

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Release no-op karşılığı. Aynı paket/fonksiyon debug source set'inde gerçek
 * araçla sağlanır; release build'de debug araç kodu hiç derlenmez.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun CoffeePreprocessDebugTool(cup: Uri?, saucer: Uri?, modifier: Modifier = Modifier) {
    // no-op
}
