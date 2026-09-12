package com.prompthavenai.falcibuket.ui.screens.flows

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.PhotoUploadSlot
import com.prompthavenai.falcibuket.ui.components.ReadingFlowScaffold
import com.prompthavenai.falcibuket.ui.components.ReadingPreviewNotice
import com.prompthavenai.falcibuket.ui.components.findActivity
import com.prompthavenai.falcibuket.ui.components.newCameraCapture
import com.prompthavenai.falcibuket.ui.components.uriSaver
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import java.io.File

private fun deleteTempFile(path: String?) {
    if (path != null) runCatching { File(path).delete() }
}

/** Reusable single-photo flow for PHOTO_VISION types other than coffee. */
@Composable
fun GenericPhotoReadingScreen(
    title: String,
    subtitle: String,
    cta: String,
    @DrawableRes artworkRes: Int,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var photo by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable(stateSaver = uriSaver) { mutableStateOf<Uri?>(null) }
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && cameraUri != null) {
            photo = cameraUri
        } else {
            // Capture cancelled: drop the pending temp file and keep the previous photo.
            deleteTempFile(cameraPath)
            cameraPath = null
            cameraUri = null
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) {
            // Gallery pick replaces any pending camera capture.
            deleteTempFile(cameraPath)
            cameraPath = null
            cameraUri = null
            photo = picked
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Delete the pending capture when actually leaving (not on rotation).
            if (activity?.isChangingConfigurations != true) {
                deleteTempFile(cameraPath)
            }
        }
    }

    ReadingFlowScaffold(title = title, subtitle = subtitle, onBack = onBack) {
        Image(
            painterResource(artworkRes), null,
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(20.dp))
        PhotoUploadSlot(
            label = "Fotoğraf",
            uri = photo,
            modifier = Modifier.fillMaxWidth(),
            onCamera = {
                // Replace: drop the previous capture's temp file before creating a new one.
                deleteTempFile(cameraPath)
                val capture = newCameraCapture(context, "photo")
                cameraUri = capture.uri
                cameraPath = capture.file.absolutePath
                takePicture.launch(capture.uri)
            },
            onGallery = {
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Fotoğraf bu önizlemede analiz edilmez.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(Modifier.height(20.dp))
        GoldButton(
            cta,
            enabled = photo != null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                deleteTempFile(cameraPath)
                cameraPath = null
                onSubmit()
            }
        )
        Spacer(Modifier.height(12.dp))
        ReadingPreviewNotice()
    }
}
