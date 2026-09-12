package com.prompthavenai.falcibuket.ui.screens

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.theme.*
import com.prompthavenai.falcibuket.ui.viewmodel.CoffeeViewModel
import java.io.File

@Composable
fun CoffeeScreen(nav: NavController) {
    val context = LocalContext.current
    val vm: CoffeeViewModel = viewModel(viewModelStoreOwner = context as androidx.lifecycle.ViewModelStoreOwner)

    var cupImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var saucerImage by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }

    var pendingCameraSlot by remember { mutableStateOf(0) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun newCameraUri(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "coffee_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && cameraUri != null) {
            when (pendingCameraSlot) {
                1 -> cupImage = cameraUri
                2 -> saucerImage = cameraUri
            }
        }
    }
    val pickCup = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { cupImage = it }
    val pickSaucer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { saucerImage = it }

    Box(Modifier.fillMaxSize().background(NightBg), contentAlignment = Alignment.TopCenter) {
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
                Image(
                    painterResource(R.drawable.coffee_upload), null,
                    modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(24.dp))

                val cupSlot: @Composable (Modifier) -> Unit = { m ->
                    UploadSlot(
                        label = "Fincanın İçi",
                        uri = cupImage,
                        modifier = m,
                        onCamera = {
                            pendingCameraSlot = 1
                            cameraUri = newCameraUri().also { takePicture.launch(it) }
                        },
                        onGallery = {
                            pickCup.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
                val saucerSlot: @Composable (Modifier) -> Unit = { m ->
                    UploadSlot(
                        label = "Fincan Tabağı",
                        uri = saucerImage,
                        modifier = m,
                        onCamera = {
                            pendingCameraSlot = 2
                            cameraUri = newCameraUri().also { takePicture.launch(it) }
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
            }
        }
    }
}

private val uriSaver = androidx.compose.runtime.saveable.Saver<Uri?, String>(
    save = { it?.toString() ?: "" },
    restore = { if (it == "") null else Uri.parse(it) }
)

@Composable
private fun UploadSlot(
    label: String,
    uri: Uri?,
    modifier: Modifier = Modifier,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = uri?.let {
            runCatching {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, it)
                ) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.setTargetSampleSize(4)
                }
            }.getOrNull()?.asImageBitmap()
        }
    }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfacePlum.copy(alpha = 0.6f))
                .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .clickable(onClick = onGallery),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(bitmap!!, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AddAPhoto, null, tint = Gold)
                    Spacer(Modifier.height(8.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium, color = TextCream)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SlotAction("Fotoğraf Çek", Icons.Filled.PhotoCamera, Modifier.weight(1f), onCamera)
            SlotAction("Galeriden Seç", Icons.Filled.PhotoLibrary, Modifier.weight(1f), onGallery)
        }
    }
}

@Composable
private fun SlotAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfacePlumSoft)
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextCream)
    }
}
