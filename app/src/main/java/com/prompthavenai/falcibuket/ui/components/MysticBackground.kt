package com.prompthavenai.falcibuket.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.prompthavenai.falcibuket.ui.theme.NightBg

/**
 * Full-viewport decorative background with a readability scrim.
 * The artwork stays fixed behind the foreground slot; the caller places the
 * scrolling content inside [content] so the image does not stretch with it.
 */
@Composable
fun MysticBackground(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    imageAlpha: Float = 0.55f,
    scrimTop: Float = 0.72f,
    scrimBottom: Float = 0.95f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize().background(NightBg)) {
        Image(
            painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = imageAlpha
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(
                        NightBg.copy(alpha = scrimTop),
                        NightBg.copy(alpha = scrimBottom)
                    )
                )
            )
        )
        content()
    }
}

/** Simple vertical scrim used over artwork where a background image is not needed. */
val ArtworkScrim: Brush
    get() = Brush.verticalGradient(
        listOf(Color.Transparent, Color.Transparent, NightBg.copy(alpha = 0.55f))
    )
