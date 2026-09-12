package com.prompthavenai.falcibuket.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum

/** Rounded, aspect-correct header artwork for a reading result. */
@Composable
fun ReadingArtworkHeader(
    @DrawableRes imageRes: Int,
    aspectRatio: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfacePlum)
        ) {
            Image(
                painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit
            )
            Box(Modifier.matchParentSize().background(ArtworkScrim))
        }
    }
}

/** Gentle breathing artwork for the coffee analysis loading state. */
@Composable
fun CoffeeLoadingVisual(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loadingBreath")
    val breath by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    Image(
        painterResource(R.drawable.loading_analysis_magic),
        contentDescription = null,
        modifier = modifier
            .widthIn(max = 190.dp)
            .aspectRatio(640f / 960f)
            .graphicsLayer { alpha = breath },
        contentScale = ContentScale.Fit
    )
}
