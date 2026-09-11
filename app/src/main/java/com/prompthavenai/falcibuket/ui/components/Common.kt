package com.prompthavenai.falcibuket.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.theme.*

/** Pressable scale wrapper for card feedback. */
@Composable
fun Modifier.pressScale(onClick: () -> Unit, scaleAmount: Float = 0.97f): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) scaleAmount else 1f, label = "press")
    return this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = TextCream,
            disabledContainerColor = SurfacePlum,
            disabledContentColor = TextMuted
        ),
        contentPadding = PaddingValues(),
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(GoldDeep, Gold, GoldDeep)),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) Color(0xFF2A1A05) else TextMuted)
        }
    }
}

/** Image card with dark bottom gradient overlay; big visual, not a thumbnail. */
@Composable
fun OverlayImageCard(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    corner: Int = 24,
    overlayTop: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .clip(RoundedCornerShape(corner.dp))
        .animateContentSize()
    val clickable = if (onClick != null) base.pressScale(onClick) else base
    Box(clickable) {
        Image(
            painter, contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (overlayTop)
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, OverlayDark))
                    else
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, OverlayDark))
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}
