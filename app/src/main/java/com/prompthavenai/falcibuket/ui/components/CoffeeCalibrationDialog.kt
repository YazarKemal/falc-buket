package com.prompthavenai.falcibuket.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.data.model.CoffeeCupPhotoGeometry
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import java.io.File

/**
 * Manuel rim kalibrasyonu (DEBUG). Otomatik elips tespiti güvenilir değilse
 * kullanıcı elipsi fincanın iç ağzına hizalar. Basit ve amaçlıdır; büyük bir
 * editör değildir. Kaydetme yalnızca onayda yapılır.
 */
@Composable
fun CoffeeCalibrationDialog(
    title: String,
    photoFile: File,
    initial: CoffeeCupPhotoGeometry?,
    onConfirm: (CoffeeCupPhotoGeometry) -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap by produceState<ImageBitmap?>(null, photoFile) {
        value = runCatching {
            BitmapFactory.decodeFile(photoFile.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    val start = initial ?: CoffeeCupPhotoGeometry(0.5f, 0.5f, 0.35f, 0.35f, 0f)
    var cx by remember { mutableFloatStateOf(start.rimCenterX) }
    var cy by remember { mutableFloatStateOf(start.rimCenterY) }
    var rx by remember { mutableFloatStateOf(start.rimRadiusX) }
    var ry by remember { mutableFloatStateOf(start.rimRadiusY) }
    var rot by remember { mutableFloatStateOf(start.rimRotation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Gold) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    CoffeeCupPhotoGeometry(
                        rimCenterX = cx,
                        rimCenterY = cy,
                        rimRadiusX = rx,
                        rimRadiusY = ry,
                        rimRotation = rot
                    ).takeIf { it.isValid() } ?: start
                )
            }) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Elipsi fincanın İÇ ağzına hizala.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(8.dp))
                val image = bitmap
                if (image != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(image.width.toFloat() / image.height.toFloat())
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        Image(
                            image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                        Canvas(Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height.toFloat())) {
                            val c = Offset(cx * size.width, cy * size.height)
                            val radii = Size(rx * size.width * 2f, ry * size.height * 2f)
                            rotate(degrees = Math.toDegrees(rot.toDouble()).toFloat(), pivot = c) {
                                drawOval(
                                    color = Gold,
                                    topLeft = Offset(c.x - radii.width / 2f, c.y - radii.height / 2f),
                                    size = radii,
                                    style = Stroke(width = 4f)
                                )
                            }
                        }
                    }
                } else {
                    Text("Fotoğraf yükleniyor…", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Spacer(Modifier.height(12.dp))
                CalibrationSlider("Merkez X", cx, 0f, 1f) { cx = it }
                CalibrationSlider("Merkez Y", cy, 0f, 1f) { cy = it }
                CalibrationSlider("Yarıçap X", rx, 0.05f, 0.60f) { rx = it }
                CalibrationSlider("Yarıçap Y", ry, 0.05f, 0.60f) { ry = it }
                CalibrationSlider("Dönüş", rot, -0.6f, 0.6f) { rot = it }
            }
        }
    )
}

@Composable
private fun CalibrationSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(String.format("%.2f", value), style = MaterialTheme.typography.bodySmall, color = Gold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}
