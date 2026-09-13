package com.prompthavenai.falcibuket.ui.components

import android.graphics.Bitmap
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.gltfio.FilamentInstance
import com.prompthavenai.falcibuket.BuildConfig
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.theme.NightBg
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.model
import io.github.sceneview.model.renderableEntities
import io.github.sceneview.node.ModelNode as FilamentModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.texture.setBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "FalciBuket3D"
private const val LOAD_FALLBACK_TIMEOUT_MS = 8_000L
private const val DEFAULT_CUP_ASSET = "models/coffee_cup.glb"
private const val DEFAULT_INNER_ASSET = "models/coffee_cup_inner.glb"

const val CUP_MODE_NORMAL = 0
const val CUP_MODE_INNER_ONLY_MAGENTA = 1
const val CUP_MODE_OUTER_INNER_MAGENTA = 2
const val CUP_MODE_OUTER_INNER_ATLAS = 3

fun cupDebugModeLabel(mode: Int): String = when (mode) {
    CUP_MODE_INNER_ONLY_MAGENTA -> "Yalnız iç (magenta)"
    CUP_MODE_OUTER_INNER_MAGENTA -> "Dış + iç (magenta)"
    CUP_MODE_OUTER_INNER_ATLAS -> "Dış + iç (atlas)"
    else -> "Normal"
}

private fun logD(message: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, message)
}

private fun logW(message: String) {
    if (BuildConfig.DEBUG) Log.w(TAG, message)
}

private class OverlayMaterial(
    val texture: Texture?,
    val materialInstance: MaterialInstance
)

private fun createAtlasOverlayMaterial(
    engine: Engine,
    materialLoader: MaterialLoader,
    atlas: Bitmap
): OverlayMaterial {
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
    logD("ATLAS_MATERIAL_CREATED ${atlas.width}x${atlas.height} srgb=true opaque=true")
    return OverlayMaterial(texture, materialInstance)
}

private fun createMagentaOverlayMaterial(materialLoader: MaterialLoader): OverlayMaterial {
    val materialInstance = materialLoader.createUnlitColorInstance(Color(1f, 0f, 1f, 1f))
    materialInstance.setDoubleSided(true)
    logD("MAGENTA_MATERIAL_CREATED doubleSided=true")
    return OverlayMaterial(null, materialInstance)
}

private fun destroyOverlayMaterial(
    engine: Engine,
    materialLoader: MaterialLoader,
    holder: OverlayMaterial
) {
    runCatching { materialLoader.destroyMaterialInstance(holder.materialInstance) }
    holder.texture?.let { runCatching { engine.destroyTexture(it) } }
    logD("OVERLAY_MATERIAL_DESTROYED texture=${holder.texture != null}")
}

@Composable
private fun rememberInnerModelInstance(
    modelLoader: ModelLoader,
    asset: String
): FilamentInstance? {
    val context = LocalContext.current
    var instance by remember(modelLoader, asset) { mutableStateOf<FilamentInstance?>(null) }

    LaunchedEffect(modelLoader, asset) {
        logD("INNER_MODEL_LOAD_START asset=$asset")
        val result = runCatching {
            val bytes = withContext(Dispatchers.IO) {
                context.assets.open(asset).use { it.readBytes() }
            }
            logD("INNER_MODEL_BYTES_READ bytes=${bytes.size}")
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.flip()
            modelLoader.createModelInstance(buffer)
        }
        result.onSuccess { created ->
            instance = created
            val entities = runCatching { created.entities.size }.getOrDefault(-1)
            val renderables = runCatching { created.renderableEntities.size }.getOrDefault(-1)
            val materials = runCatching { created.materialInstances.size }.getOrDefault(-1)
            val bbox = runCatching { created.asset.boundingBox.toString() }.getOrDefault("?")
            logD(
                "INNER_MODEL_INSTANCE_CREATED entities=$entities renderables=$renderables " +
                    "materials=$materials bbox=$bbox"
            )
        }.onFailure { t ->
            instance = null
            logW(
                "INNER_MODEL_INSTANCE_FAILED stage=create class=${t.javaClass.name} " +
                    "message=${t.message}"
            )
        }
    }

    DisposableEffect(instance) {
        val current = instance
        onDispose {
            current?.let {
                logD("INNER_MODEL_INSTANCE_DISPOSED")
                runCatching { modelLoader.destroyModel(it.model) }
            }
        }
    }
    return instance
}

/**
 * Yerel/çevrimdışı gerçek-zamanlı 3D Türk kahvesi fincanı görüntüleyicisi.
 *
 * - [modelAsset] gerçek Meshy fincanını yükler.
 * - [innerModelAsset] fincanın GERÇEK iç yüzeyinden çıkarılmış kaplama ağını yükler.
 * - [interiorAtlas] verilirse opak PBR atlas malzemesiyle iç kaplamaya uygulanır.
 * - [debugMode] yalnızca DEBUG derlemesinde etkili teşhis modudur (release'de yok sayılır).
 */
@Composable
fun CoffeeCup3DView(
    modifier: Modifier = Modifier,
    modelAsset: String = DEFAULT_CUP_ASSET,
    innerModelAsset: String = DEFAULT_INNER_ASSET,
    interiorAtlas: Bitmap? = null,
    debugMode: Int = CUP_MODE_NORMAL
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, modelAsset)
    val innerInstance = rememberInnerModelInstance(modelLoader, innerModelAsset)

    var loadTimedOut by remember { mutableStateOf(false) }
    LaunchedEffect(modelInstance) {
        if (modelInstance == null) {
            delay(LOAD_FALLBACK_TIMEOUT_MS)
            loadTimedOut = true
        } else {
            loadTimedOut = false
            val entities = runCatching { modelInstance.entities.size }.getOrDefault(-1)
            logD("OUTER_MODEL_INSTANCE_CREATED entities=$entities")
        }
    }

    val effectiveMode = if (BuildConfig.DEBUG) debugMode else CUP_MODE_NORMAL
    val showOuter = effectiveMode != CUP_MODE_INNER_ONLY_MAGENTA
    val atlasReady = interiorAtlas != null && !interiorAtlas.isRecycled

    val overlayMaterial = remember(engine, materialLoader, interiorAtlas, effectiveMode) {
        when (effectiveMode) {
            CUP_MODE_INNER_ONLY_MAGENTA, CUP_MODE_OUTER_INNER_MAGENTA ->
                createMagentaOverlayMaterial(materialLoader)
            CUP_MODE_OUTER_INNER_ATLAS ->
                if (atlasReady) {
                    createAtlasOverlayMaterial(engine, materialLoader, interiorAtlas)
                } else {
                    createMagentaOverlayMaterial(materialLoader)
                }
            else ->
                if (atlasReady) {
                    createAtlasOverlayMaterial(engine, materialLoader, interiorAtlas)
                } else {
                    null
                }
        }
    }

    val previousHolder = remember { arrayOfNulls<OverlayMaterial>(1) }

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
            Node {
                if (showOuter) {
                    modelInstance?.let { instance ->
                        ModelNode(modelInstance = instance)
                    }
                }
                val holder = overlayMaterial
                if (innerInstance != null && holder != null) {
                    val innerNode = remember(engine, innerInstance) {
                        FilamentModelNode(modelInstance = innerInstance).also {
                            logD("INNER_MODEL_NODE_CREATED")
                        }
                    }
                    NodeLifecycle(innerNode, null)
                    DisposableEffect(innerNode, holder) {
                        innerNode.setMaterialInstance(holder.materialInstance)
                        logD("INNER_MODEL_MATERIAL_APPLIED mode=$effectiveMode")
                        val old = previousHolder[0]
                        if (old != null && old !== holder) {
                            destroyOverlayMaterial(engine, materialLoader, old)
                        }
                        previousHolder[0] = holder
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
