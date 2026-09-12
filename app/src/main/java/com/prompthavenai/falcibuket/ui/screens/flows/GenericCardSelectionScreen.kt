package com.prompthavenai.falcibuket.ui.screens.flows

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.ReadingFlowScaffold
import com.prompthavenai.falcibuket.ui.components.ReadingPreviewNotice
import com.prompthavenai.falcibuket.ui.components.SimpleGrid
import com.prompthavenai.falcibuket.ui.components.adaptiveColumns
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.GoldDeep
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum

/** Reusable card-selection flow for all CARD_SELECTION types. */
@Composable
fun GenericCardSelectionScreen(
    title: String,
    subtitle: String,
    @DrawableRes artworkRes: Int,
    cta: String,
    onBack: () -> Unit,
    cardCount: Int = 6,
    selectCount: Int = 3,
    @DrawableRes introArtworkRes: Int? = null,
    onSubmit: (List<Int>) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(listOf<Int>()) }

    fun toggle(index: Int) {
        selected = when {
            index in selected -> selected - index
            selected.size < selectCount -> selected + index
            else -> selected
        }
    }

    ReadingFlowScaffold(title = title, subtitle = subtitle, onBack = onBack, maxWidth = 1000.dp) {
        if (introArtworkRes != null) {
            Image(
                painterResource(introArtworkRes), null,
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(20.dp))
        }
        Text(
            "İçinden geldiği gibi $selectCount kart seç. (${selected.size}/$selectCount)",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = adaptiveColumns(
                availableWidth = maxWidth,
                minCellWidth = 110.dp,
                spacing = 12.dp,
                max = 6
            )
            SimpleGrid(
                items = (0 until cardCount).toList(),
                columns = columns,
                horizontalSpacing = 12.dp,
                verticalSpacing = 14.dp
            ) { index ->
                SelectionCard(
                    selected = index in selected,
                    artworkRes = artworkRes,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { toggle(index) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        GoldButton(
            cta,
            enabled = selected.size == selectCount,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSubmit(selected) }
        )
        Spacer(Modifier.height(12.dp))
        ReadingPreviewNotice()
    }
}

@Composable
private fun SelectionCard(
    selected: Boolean,
    @DrawableRes artworkRes: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "card")
    Box(
        modifier
            .aspectRatio(0.62f)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(Gold, GoldDeep) else listOf(SurfacePlum, Color(0xFF2C1B45))
                )
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.7f) else Gold.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(artworkRes), null,
            modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
            alpha = if (selected) 1f else 0.55f
        )
    }
}
