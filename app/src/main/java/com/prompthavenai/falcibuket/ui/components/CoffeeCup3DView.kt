package com.prompthavenai.falcibuket.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.prompthavenai.falcibuket.BuildConfig
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.theme.NightBg
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.ModelNode as FilamentModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.texture.setBitmap
import kotlinx.coroutines.delay

private const val LOAD_FALLBACK_TIMEOUT_MS = 8_000L
private const val DEFAULT_CUP_ASSET = "models/coffee_cup.glb"
private const val DEFAULT_INNER_ASSET = "models/coffee_cup_inner.glb"

private data class InnerOverlayMaterial(
    val texture: Texture?,
    val materialInstance: MaterialInstance
)

private fun createAtlasOverlayMaterial(
    engine: Engine,
    materialLoader: MaterialLoader,
    atlas: Bitmap
): InnerOverlayMaterial {
    val texture = Texture.Builder()
        .width(atlas.width)
        .height(atlas.height)
        .format(Texture.InternalFormat.SRGB8_A8)
        .sampler(Texture.Sampler.SAMPLER_2D)
        .build(engine)
    texture.setBitmap(engine, atlas)
    val sampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.REPEAT
    )
    sampler.wrapModeT = TextureSampler.WrapMode.CLAMP_TO_EDGE
    val materialInstance = materialLoader.createTextureInstance(
        texture = texture,
        isOpaque = true,
        metallic = 0f,
        roughness = 1f,
        reflectance = 0f
    )
    materialInstance.setDoubleSided(true)
    return InnerOverlayMaterial(texture, materialInstance)
}

/**
 * Yerel/çevrimdışı gerçek-zamanlı 3D Türk kahvesi fincanı görüntüleyicisi.
 *
 * - [modelAsset] gerçek Meshy fincanını yükler.
 * - [innerModelAsset] fincanın GERÇEK iç yüzeyinden çıkarılmış kaplama ağını yükler ve
 *   aynı ebeveyn dönüşümü altına yerleştirir; böylece otomatik hizalanır.
 * - [interiorAtlas] verilirse atlas opak PBR malzemesiyle iç kaplamaya uygulanır.
 * - [debugInnerMesh] yalnızca DEBUG derlemesinde çalışan teşhis modu: kaplamayı parlak
 *   renkte gösterir. Release'de asla etkinleşmez.
 * - Model yüklenemezse statik 2D görsele düşer.
 */
@Composable
fun CoffeeCup3DView(
    modifier: Modifier = Modifier,
    modelAsset: String = DEFAULT_CUP_ASSET,
    innerModelAsset: String = DEFAULT_INNER_ASSET,
    interiorAtlas: Bitmap? = null,
    debugInnerMesh: Boolean = false
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, modelAsset)
    val innerInstance = rememberModelInstance(modelLoader, innerModelAsset)

    var loadTimedOut by remember { mutableStateOf(false) }
    LaunchedEffect(modelInstance) {
        if (modelInstance == null) {
            delay(LOAD_FALLBACK_TIMEOUT_MS)
            loadTimedOut = true
        } else {
            loadTimedOut = false
        }
    }

    val debugActive = debugInnerMesh && BuildConfig.DEBUG
    val overlayMaterial = remember(engine, materialLoader, interiorAtlas, debugActive) {
        when {
            debugActive -> InnerOverlayMaterial(
                texture = null,
                materialInstance = materialLoader.createUnlitColorInstance(Color(1f, 0f, 1f, 1f))
            )
            interiorAtlas != null && !interiorAtlas.isRecycled ->
                createAtlasOverlayMaterial(engine, materialLoader, interiorAtlas)
            else -> null
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(SurfacePlum, NightBg))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (modelInstance == null && loadTimedOut) {
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
            materialLoader = materialLoader,
            autoCenterContent = true,
            autoFitContent = true,
            cameraManipulator = rememberCameraManipulator()
        ) {
            // Tek paylaşılan transform: Meshy fincan ve gerçek iç kaplama aynı yerel uzayda
            // (model-local) durur ve birlikte döner/ölçeklenir.
            Node {
                modelInstance?.let { instance ->
                    ModelNode(modelInstance = instance)
                }
                val material = overlayMaterial?.materialInstance
                if (innerInstance != null && material != null) {
                    val innerNode = remember(engine, innerInstance) {
                        FilamentModelNode(modelInstance = innerInstance)
                    }
                    NodeLifecycle(innerNode, null)
                    DisposableEffect(innerNode, material) {
                        innerNode.setMaterialInstance(material)
                        onDispose { }
                    }
                }
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
