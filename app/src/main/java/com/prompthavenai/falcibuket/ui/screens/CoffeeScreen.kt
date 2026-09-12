package com.prompthavenai.falcibuket.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.CoffeeVideoAssets
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.MysticLoopVideo
import com.prompthavenai.falcibuket.ui.components.PhotoUploadSlot
import com.prompthavenai.falcibuket.ui.components.newCameraCapture
import com.prompthavenai.falcibuket.ui.components.uriSaver
import com.prompthavenai.falcibuket.ui.debug.CoffeePreprocessDebugTool
import com.prompthavenai.falcibuket.ui.theme.*
import com.prompthavenai.falcibuket.ui.viewmodel.CoffeeViewModel
import java.io.File

private fun deleteTempFile(file: File?) {
    if (file != null) runCatching { file.delete() }
}

@Composable
fun CoffeeScreen(nav: NavController) {
    val context = LocalContext.current
    val vm: CoffeeViewModel = viewModel(viewModelStoreOwner = context as androidx.lifecycle.ViewModelStoreOwner)

    var cupImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var saucerImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    // Yalnızca iptal/yerine-koyma anında silinen bekleyen kamera dosyası.
    // Kabul edilen dosyaların temizliği ViewModel yaşam döngüsüne aittir.
    var pendingCapture by remember { mutableStateOf<File?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraSlot by remember { mutableStateOf(0) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val capturedUri = cameraUri
        if (ok && capturedUri != null) {
            when (pendingCameraSlot) {
                1 -> cupImage = capturedUri
                2 -> saucerImage = capturedUri
            }
        } else {
            deleteTempFile(pendingCapture)
        }
        pendingCapture = null
        cameraUri = null
    }
    val pickCup = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) {
            deleteTempFile(pendingCapture)
            pendingCapture = null
            cameraUri = null
            cupImage = picked
        }
    }
    val pickSaucer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) {
            deleteTempFile(pendingCapture)
            pendingCapture = null
            cameraUri = null
            saucerImage = picked
        }
    }

    Box(Modifier.fillMaxSize().background(NightBg), contentAlignment = Alignment.TopCenter) {
        // Giriş hero'su: sessiz, döngülü arka plan videosu.
        MysticLoopVideo(
            videoRes = CoffeeVideoAssets.ENTRY_LOOP,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Form kontrollerinin okunabilirliği için koyu/erik scrim.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        NightBg.copy(alpha = 0.74f),
                        NightBg.copy(alpha = 0.88f),
                        NightBg.copy(alpha = 0.94f)
                    )
                )
            )
        )

        BoxWithConstraints(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            val twoColumns = maxWidth >= 640.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
                }
                Text("Fincanını Göster", style = MaterialTheme.typography.headlineMedium, color = Gold)
                Spacer(Modifier.height(8.dp))
                Text("Fincanın içini ve tabağını net şekilde fotoğraflaman yeterli.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))

                val cupSlot: @Composable (Modifier) -> Unit = { m ->
                    PhotoUploadSlot(
                        label = "Fincanın İçi",
                        uri = cupImage,
                        modifier = m,
                        onCamera = {
                            deleteTempFile(pendingCapture)
                            pendingCameraSlot = 1
                            val capture = newCameraCapture(context, "coffee")
                            vm.trackTempFile(capture.file)
                            pendingCapture = capture.file
                            cameraUri = capture.uri
                            takePicture.launch(capture.uri)
                        },
                        onGallery = {
                            pickCup.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
                val saucerSlot: @Composable (Modifier) -> Unit = { m ->
                    PhotoUploadSlot(
                        label = "Fincan Tabağı",
                        uri = saucerImage,
                        modifier = m,
                        onCamera = {
                            deleteTempFile(pendingCapture)
                            pendingCameraSlot = 2
                            val capture = newCameraCapture(context, "coffee")
                            vm.trackTempFile(capture.file)
                            pendingCapture = capture.file
                            cameraUri = capture.uri
                            takePicture.launch(capture.uri)
                        },
                        onGallery = {
                            pickSaucer.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }

                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        cupSlot(Modifier.weight(1f))
                        saucerSlot(Modifier.weight(1f))
                    }
                } else {
                    cupSlot(Modifier)
                    Spacer(Modifier.height(16.dp))
                    saucerSlot(Modifier)
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
                // Source-set gating: debug'da gerçek araç, release'te no-op (kod release'te yok).
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
