package com.prompthavenai.falcibuket.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.theme.NightBg
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay

private const val LOAD_FALLBACK_TIMEOUT_MS = 8_000L

/**
 * Yerel/çevrimdışı gerçek-zamanlı 3D Türk kahvesi fincanı görüntüleyicisi.
 *
 * - GLB'yi uygulama asset'lerinden yükler (ağ yok).
 * - Modeli otomatik ortalar ve tamamını çerçeveler.
 * - Sürükleyerek orbit döndürme (SceneView kamera manipülatörü) destekler.
 * - Model yüklenemezse, belirli bir süre sonra korunmuş statik 2D görsele düşer.
 */
@Composable
fun CoffeeCup3DView(
    modifier: Modifier = Modifier,
    modelAsset: String = "models/coffee_cup.glb"
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, modelAsset)

    var loadTimedOut by remember { mutableStateOf(false) }
    LaunchedEffect(modelInstance) {
        if (modelInstance == null) {
            delay(LOAD_FALLBACK_TIMEOUT_MS)
            loadTimedOut = true
        } else {
            loadTimedOut = false
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(SurfacePlum, NightBg))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (modelInstance == null && loadTimedOut) {
            // Zarif statik geri dönüş: korunmuş 2D kahve görseli.
            Image(
                painterResource(R.drawable.coffee_upload),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            return@Box
        }

        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            autoCenterContent = true,
            autoFitContent = true,
            cameraManipulator = rememberCameraManipulator()
        ) {
            modelInstance?.let { instance ->
                ModelNode(modelInstance = instance, scaleToUnits = 1.0f)
            }
        }
        if (modelInstance == null) {
            Text(
                "Fincan yükleniyor…",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}
