package com.prompthavenai.falcibuket.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.BuildConfig
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion
import com.prompthavenai.falcibuket.data.remote.CoffeeCupAtlasBuilder
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.CoffeeCup3DView
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.PhotoUploadSlot
import com.prompthavenai.falcibuket.ui.components.newCameraCapture
import com.prompthavenai.falcibuket.ui.components.uriSaver
import com.prompthavenai.falcibuket.ui.debug.CoffeePreprocessDebugTool
import com.prompthavenai.falcibuket.ui.theme.*
import com.prompthavenai.falcibuket.ui.viewmodel.CoffeeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun deleteTempFile(file: File?) {
    if (file != null) runCatching { file.delete() }
}

@Composable
fun CoffeeScreen(nav: NavController) {
    val context = LocalContext.current
    val vm: CoffeeViewModel = viewModel(viewModelStoreOwner = context as androidx.lifecycle.ViewModelStoreOwner)

    // Mevcut AI akışı (dokunulmuyor).
    var cupImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var saucerImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }

    // Yeni 3D iç-yüzey akışı: üç bölge fotoğrafı.
    var leftUri by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var centerUri by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var rightUri by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var atlas by remember { mutableStateOf<Bitmap?>(null) }
    var debugInnerMesh by remember { mutableStateOf(false) }

    var pendingCapture by remember { mutableStateOf<File?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraSlot by remember { mutableStateOf(0) }
    var pendingRegion by remember { mutableStateOf<CoffeeCupRegion?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val capturedUri = cameraUri
        if (ok && capturedUri != null) {
            when (pendingCameraSlot) {
                1 -> cupImage = capturedUri
                2 -> saucerImage = capturedUri
                3 -> leftUri = capturedUri
                4 -> centerUri = capturedUri
                5 -> rightUri = capturedUri
            }
        } else {
            deleteTempFile(pendingCapture)
        }
        pendingCapture = null
        cameraUri = null
        pendingRegion = null
    }
    val pickCup = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; cupImage = picked }
    }
    val pickSaucer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; saucerImage = picked }
    }
    val pickLeft = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; leftUri = picked }
    }
    val pickCenter = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; centerUri = picked }
    }
    val pickRight = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; rightUri = picked }
    }

    // Üç bölge değişince atlası yerel olarak yeniden üret (ağ yok).
    LaunchedEffect(leftUri, centerUri, rightUri) {
        atlas = withContext(Dispatchers.IO) {
            CoffeeCupAtlasBuilder.build(
                context,
                mapOf(
                    CoffeeCupRegion.LEFT_INNER to leftUri,
                    CoffeeCupRegion.CENTER_INNER to centerUri,
                    CoffeeCupRegion.RIGHT_INNER to rightUri
                )
            )
        }
    }

    Box(Modifier.fillMaxSize().background(NightBg), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            val twoColumns = maxWidth >= 640.dp
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
                }
                Text("Fincanını Göster", style = MaterialTheme.typography.headlineMedium, color = Gold)
                Spacer(Modifier.height(6.dp))
                Text("Üç bölgeyi de çek; fincanın içine gerçek fotoğrafların yerleşecek.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))

                // 3D viewport scroll dışında: dikey sürükleme fincanı döndürür.
                CoffeeCup3DView(
                    modifier = Modifier.fillMaxWidth().weight(1.05f).clip(RoundedCornerShape(28.dp)),
                    interiorAtlas = atlas,
                    debugInnerMesh = debugInnerMesh
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fincanı parmağınla çevirebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                if (BuildConfig.DEBUG) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Teşhis: iç kaplama ağı",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = debugInnerMesh, onCheckedChange = { debugInnerMesh = it })
                    }
                }
                Spacer(Modifier.height(14.dp))

                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    RegionCapture(
                        step = 1,
                        title = "Sol Bölge",
                        guidance = "Fincanın iç yüzeyinin sol bölümünü karşıdan çek.",
                        uri = leftUri,
                        onCamera = {
                            deleteTempFile(pendingCapture); pendingCameraSlot = 3; pendingRegion = CoffeeCupRegion.LEFT_INNER
                            val capture = newCameraCapture(context, "coffee_left"); vm.trackTempFile(capture.file)
                            pendingCapture = capture.file; cameraUri = capture.uri; takePicture.launch(capture.uri)
                        },
                        onGallery = { pickLeft.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )
                    Spacer(Modifier.height(16.dp))
                    RegionCapture(
                        step = 2,
                        title = "Orta Bölge",
                        guidance = "Fincanın karşı iç yüzeyini ortalayarak çek.",
                        uri = centerUri,
                        onCamera = {
                            deleteTempFile(pendingCapture); pendingCameraSlot = 4; pendingRegion = CoffeeCupRegion.CENTER_INNER
                            val capture = newCameraCapture(context, "coffee_center"); vm.trackTempFile(capture.file)
                            pendingCapture = capture.file; cameraUri = capture.uri; takePicture.launch(capture.uri)
                        },
                        onGallery = { pickCenter.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )
                    Spacer(Modifier.height(16.dp))
                    RegionCapture(
                        step = 3,
                        title = "Sağ Bölge",
                        guidance = "Fincanın iç yüzeyinin sağ bölümünü karşıdan çek.",
                        uri = rightUri,
                        onCamera = {
                            deleteTempFile(pendingCapture); pendingCameraSlot = 5; pendingRegion = CoffeeCupRegion.RIGHT_INNER
                            val capture = newCameraCapture(context, "coffee_right"); vm.trackTempFile(capture.file)
                            pendingCapture = capture.file; cameraUri = capture.uri; takePicture.launch(capture.uri)
                        },
                        onGallery = { pickRight.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )

                    Spacer(Modifier.height(28.dp))
                    Text("Fal için Fincan & Tabak", style = MaterialTheme.typography.titleMedium, color = Gold)
                    Spacer(Modifier.height(12.dp))
                    val cupSlot: @Composable (Modifier) -> Unit = { m ->
                        PhotoUploadSlot(
                            label = "Fincanın İçi",
                            uri = cupImage,
                            modifier = m,
                            onCamera = {
                                deleteTempFile(pendingCapture); pendingCameraSlot = 1
                                val capture = newCameraCapture(context, "coffee"); vm.trackTempFile(capture.file)
                                pendingCapture = capture.file; cameraUri = capture.uri; takePicture.launch(capture.uri)
                            },
                            onGallery = { pickCup.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        )
                    }
                    val saucerSlot: @Composable (Modifier) -> Unit = { m ->
                        PhotoUploadSlot(
                            label = "Fincan Tabağı",
                            uri = saucerImage,
                            modifier = m,
                            onCamera = {
                                deleteTempFile(pendingCapture); pendingCameraSlot = 2
                                val capture = newCameraCapture(context, "coffee"); vm.trackTempFile(capture.file)
                                pendingCapture = capture.file; cameraUri = capture.uri; takePicture.launch(capture.uri)
                            },
                            onGallery = { pickSaucer.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        )
                    }
                    if (twoColumns) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            cupSlot(Modifier.weight(1f)); saucerSlot(Modifier.weight(1f))
                        }
                    } else {
                        cupSlot(Modifier); Spacer(Modifier.height(16.dp)); saucerSlot(Modifier)
                    }

                    Spacer(Modifier.height(28.dp))
                    GoldButton(
                        "Falımı Yorumla",
                        enabled = cupImage != null,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cup = cupImage ?: return@GoldButton
                            vm.analyze(cup, saucerImage)
                            nav.navigate(Routes.result("Kahve"))
                        }
                    )
                    Spacer(Modifier.height(20.dp))
                    CoffeePreprocessDebugTool(
                        cup = cupImage,
                        saucer = saucerImage,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun RegionCapture(
    step: Int,
    title: String,
    guidance: String,
    uri: Uri?,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    Text("$step. $title", style = MaterialTheme.typography.titleMedium, color = Gold)
    Spacer(Modifier.height(4.dp))
    Text(guidance, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    Spacer(Modifier.height(8.dp))
    PhotoUploadSlot(
        label = title,
        uri = uri,
        modifier = Modifier.fillMaxWidth(),
        onCamera = onCamera,
        onGallery = onGallery
    )
}
