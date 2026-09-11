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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun CoffeeScreen(nav: NavController) {
    var cupImage by remember { mutableStateOf<Uri?>(null) }
    var saucerImage by remember { mutableStateOf<Uri?>(null) }

    val pickCup = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { cupImage = it }
    val pickSaucer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { saucerImage = it }

    Column(
        Modifier.fillMaxSize().background(NightBg).verticalScroll(rememberScrollState()).padding(20.dp)
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
        UploadSlot("Fincanın İçi", cupImage) {
            pickCup.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        Spacer(Modifier.height(16.dp))
        UploadSlot("Fincan Tabağı", saucerImage) {
            pickSaucer.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        Spacer(Modifier.height(28.dp))
        GoldButton(
            "Falımı Yorumla",
            enabled = cupImage != null && saucerImage != null,
            modifier = Modifier.fillMaxWidth(),
            onClick = { nav.navigate(Routes.result("Kahve")) }
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun UploadSlot(label: String, uri: Uri?, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = uri?.let {
            runCatching {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, it)
                ) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
            }.getOrNull()?.asImageBitmap()
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfacePlum.copy(alpha = 0.6f))
            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
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
}
