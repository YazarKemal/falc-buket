package com.prompthavenai.falcibuket.ui.components

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.SurfacePlumSoft
import com.prompthavenai.falcibuket.ui.theme.TextCream
import java.io.File

/** Shared saveable for an optional picked/captured image URI. */
val uriSaver = androidx.compose.runtime.saveable.Saver<Uri?, String>(
    save = { it?.toString() ?: "" },
    restore = { if (it == "") null else Uri.parse(it) }
)

/** Creates a FileProvider URI for a new camera capture. */
fun newCameraUri(context: Context, prefix: String): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Reusable photo picker/capture slot used by coffee and generic photo flows. */
@Composable
fun PhotoUploadSlot(
    label: String,
    uri: Uri?,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 150.dp,
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
                .height(height)
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
private fun SlotAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
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
