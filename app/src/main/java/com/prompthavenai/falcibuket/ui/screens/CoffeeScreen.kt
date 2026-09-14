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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.prompthavenai.falcibuket.data.local.CoffeeSessionSnapshot
import com.prompthavenai.falcibuket.data.local.CoffeeSessionStore
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion
import com.prompthavenai.falcibuket.data.remote.CoffeeCupAtlasBuilder
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.CUP_MODE_NORMAL
import com.prompthavenai.falcibuket.ui.components.CoffeeCalibrationDialog
import com.prompthavenai.falcibuket.ui.components.CoffeeCup3DView
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.PhotoUploadSlot
import com.prompthavenai.falcibuket.ui.components.cupDebugModeLabel
import com.prompthavenai.falcibuket.ui.components.newCameraCapture
import com.prompthavenai.falcibuket.ui.components.uriSaver
import com.prompthavenai.falcibuket.ui.debug.CoffeePreprocessDebugTool
import com.prompthavenai.falcibuket.ui.theme.*
import com.prompthavenai.falcibuket.ui.viewmodel.CoffeeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private fun deleteTempFile(file: File?) {
    if (file != null) runCatching { file.delete() }
}

@Composable
fun CoffeeScreen(nav: NavController) {
    val context = LocalContext.current
    val vm: CoffeeViewModel = viewModel(viewModelStoreOwner = context as androidx.lifecycle.ViewModelStoreOwner)
    val scope = rememberCoroutineScope()

    // Mevcut AI akışı (dokunulmuyor).
    var cupImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var saucerImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }

    // Kalıcı iç-yüzey oturumu: süreç ölümünden sonra geri yüklenir.
    var session by remember { mutableStateOf(CoffeeSessionSnapshot.EMPTY) }
    var restoring by remember { mutableStateOf(true) }
    var atlas by remember { mutableStateOf<Bitmap?>(null) }
    var metrics by remember { mutableStateOf<CoffeeCupAtlasBuilder.Metrics?>(null) }
    var calibratingRegion by remember { mutableStateOf<CoffeeCupRegion?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var cupDebugMode by remember { mutableStateOf(CUP_MODE_NORMAL) }
    val importMutex = remember { Mutex() }

    var pendingCapture by remember { mutableStateOf<File?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraSlot by remember { mutableStateOf(0) }
    var pendingRegion by remember { mutableStateOf<CoffeeCupRegion?>(null) }

    LaunchedEffect(Unit) {
        session = withContext(Dispatchers.IO) { CoffeeSessionStore.load(context) }
        restoring = false
    }

    // Atlası yalnızca fotoğraf/kalibrasyon sürümü değişince yeniden üret.
    LaunchedEffect(session.revisionSignature()) {
        if (session.presentCount == 0) {
            atlas = null
            metrics = null
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) { CoffeeCupAtlasBuilder.build(context, session) }
        atlas = result.atlas
        metrics = result.metrics
    }

    fun importRegion(region: CoffeeCupRegion, uri: Uri, tempToDelete: File? = null) {
        scope.launch {
            importMutex.withLock {
                val result = withContext(Dispatchers.IO) {
                    val outcome = runCatching { CoffeeSessionStore.savePhoto(context, region, uri) }
                    deleteTempFile(tempToDelete)
                    outcome
                }
                result.onSuccess {
                    session = it
                    importError = null
                }.onFailure {
                    importError = "Fotoğraf alınamadı. Lütfen tekrar deneyin."
                }
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val capturedUri = cameraUri
        val tempFile = pendingCapture
        if (ok && capturedUri != null) {
            when (pendingCameraSlot) {
                1 -> cupImage = capturedUri
                2 -> saucerImage = capturedUri
                else -> {
                    val region = pendingRegion
                    if (region != null) {
                        importRegion(region, capturedUri, tempFile)
                    }
                }
            }
        } else {
            deleteTempFile(tempFile)
        }
        pendingCapture = null
        cameraUri = null
        pendingRegion = null
    }

    fun launchCamera(slot: Int, region: CoffeeCupRegion?, prefix: String) {
        deleteTempFile(pendingCapture)
        pendingCameraSlot = slot
        pendingRegion = region
        val capture = newCameraCapture(context, prefix)
        vm.trackTempFile(capture.file)
        pendingCapture = capture.file
        cameraUri = capture.uri
        takePicture.launch(capture.uri)
    }

    val pickCup = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; cupImage = picked }
    }
    val pickSaucer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; saucerImage = picked }
    }
    val pickLeft = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; importRegion(CoffeeCupRegion.LEFT_INNER, picked) }
    }
    val pickCenter = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; importRegion(CoffeeCupRegion.CENTER_INNER, picked) }
    }
    val pickRight = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) { deleteTempFile(pendingCapture); pendingCapture = null; cameraUri = null; importRegion(CoffeeCupRegion.RIGHT_INNER, picked) }
    }

    val calRegion = calibratingRegion
    if (calRegion != null) {
        val photo = session.photos[calRegion]
        if (photo != null) {
            CoffeeCalibrationDialog(
                title = "Kalibrasyon: ${regionTitle(calRegion)}",
                photoFile = photo.file,
                initial = session.geometryFor(calRegion),
                onConfirm = { geometry ->
                    scope.launch {
                        importMutex.withLock {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    CoffeeSessionStore.saveGeometry(context, calRegion, geometry, photo.revision)
                                }
                            }
                            result.onSuccess {
                                session = it
                                importError = null
                            }.onFailure {
                                importError = "Kalibrasyon kaydedilemedi."
                            }
                        }
                        calibratingRegion = null
                    }
                },
                onDismiss = { calibratingRegion = null }
            )
        } else {
            LaunchedEffect(calRegion) { calibratingRegion = null }
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
                    debugMode = cupDebugMode
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fincanı parmağınla çevirebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                if (restoring) {
                    Text("Oturum geri yükleniyor…", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                importError?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, color = Rose)
                }
                if (BuildConfig.DEBUG) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Teşhis: ${cupDebugModeLabel(cupDebugMode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { cupDebugMode = (cupDebugMode + 1) % 4 }) {
                            Text("Değiştir")
                        }
                    }
                    TextButton(onClick = {
                        scope.launch {
                            importMutex.withLock {
                                withContext(Dispatchers.IO) { CoffeeSessionStore.clear(context) }
                                session = CoffeeSessionSnapshot.EMPTY
                                atlas = null
                                metrics = null
                            }
                        }
                    }) { Text("Oturumu temizle") }
                }
                Spacer(Modifier.height(14.dp))

                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    RegionCapture(
                        step = 1,
                        title = "Sol Bölge",
                        guidance = "Fincanın iç yüzeyinin sol bölümünü karşıdan çek.",
                        photoUri = session.photos[CoffeeCupRegion.LEFT_INNER]?.file?.let { Uri.fromFile(it) },
                        needsCalibration = metrics?.needsCalibration?.get(0) == true,
                        onCalibrate = { calibratingRegion = CoffeeCupRegion.LEFT_INNER },
                        onCamera = { launchCamera(3, CoffeeCupRegion.LEFT_INNER, "coffee_left") },
                        onGallery = { pickLeft.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )
                    Spacer(Modifier.height(16.dp))
                    RegionCapture(
                        step = 2,
                        title = "Orta Bölge",
                        guidance = "Fincanın karşı iç yüzeyini ortalayarak çek.",
                        photoUri = session.photos[CoffeeCupRegion.CENTER_INNER]?.file?.let { Uri.fromFile(it) },
                        needsCalibration = metrics?.needsCalibration?.get(1) == true,
                        onCalibrate = { calibratingRegion = CoffeeCupRegion.CENTER_INNER },
                        onCamera = { launchCamera(4, CoffeeCupRegion.CENTER_INNER, "coffee_center") },
                        onGallery = { pickCenter.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )
                    Spacer(Modifier.height(16.dp))
                    RegionCapture(
                        step = 3,
                        title = "Sağ Bölge",
                        guidance = "Fincanın iç yüzeyinin sağ bölümünü karşıdan çek.",
                        photoUri = session.photos[CoffeeCupRegion.RIGHT_INNER]?.file?.let { Uri.fromFile(it) },
                        needsCalibration = metrics?.needsCalibration?.get(2) == true,
                        onCalibrate = { calibratingRegion = CoffeeCupRegion.RIGHT_INNER },
                        onCamera = { launchCamera(5, CoffeeCupRegion.RIGHT_INNER, "coffee_right") },
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
                            onCamera = { launchCamera(1, null, "coffee") },
                            onGallery = { pickCup.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        )
                    }
                    val saucerSlot: @Composable (Modifier) -> Unit = { m ->
                        PhotoUploadSlot(
                            label = "Fincan Tabağı",
                            uri = saucerImage,
                            modifier = m,
                            onCamera = { launchCamera(2, null, "coffee") },
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

private fun regionTitle(region: CoffeeCupRegion): String = when (region) {
    CoffeeCupRegion.LEFT_INNER -> "Sol Bölge"
    CoffeeCupRegion.CENTER_INNER -> "Orta Bölge"
    CoffeeCupRegion.RIGHT_INNER -> "Sağ Bölge"
}

@Composable
private fun RegionCapture(
    step: Int,
    title: String,
    guidance: String,
    photoUri: Uri?,
    needsCalibration: Boolean,
    onCalibrate: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    Text("$step. $title", style = MaterialTheme.typography.titleMedium, color = Gold)
    Spacer(Modifier.height(4.dp))
    Text(guidance, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    Spacer(Modifier.height(8.dp))
    PhotoUploadSlot(
        label = title,
        uri = photoUri,
        modifier = Modifier.fillMaxWidth(),
        onCamera = onCamera,
        onGallery = onGallery
    )
    if (photoUri != null && needsCalibration) {
        TextButton(onClick = onCalibrate) {
            Text("Fincan kenarını hizala", color = Gold)
        }
    }
}
