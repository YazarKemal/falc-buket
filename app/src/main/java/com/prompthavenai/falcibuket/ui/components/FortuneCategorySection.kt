package com.prompthavenai.falcibuket.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.data.model.FortuneType
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.TextMuted

/** Labeled, adaptive grid section used by the fortune catalog and Home. */
@Composable
fun FortuneCategorySection(
    title: String,
    types: List<FortuneType>,
    onOpen: (FortuneType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Gold)
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = adaptiveColumns(
                availableWidth = maxWidth,
                minCellWidth = 160.dp,
                spacing = 16.dp,
                max = 4
            )
            SimpleGrid(
                items = types,
                columns = columns,
                horizontalSpacing = 16.dp,
                verticalSpacing = 16.dp
            ) { type ->
                FortuneCard(type, onOpen = onOpen)
            }
        }
    }
}

@Composable
fun FortuneCard(
    type: FortuneType,
    onOpen: (FortuneType) -> Unit,
    modifier: Modifier = Modifier
) {
    OverlayImageCard(
        painterResource(type.artworkRes),
        type.title,
        modifier = modifier.fillMaxWidth().height(180.dp),
        onClick = { onOpen(type) }
    ) {
        if (type.isPremium) {
            Text("Buket+", style = MaterialTheme.typography.labelMedium, color = Gold)
        }
        Text(type.title, style = MaterialTheme.typography.titleMedium)
        Text(
            type.subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            maxLines = 2
        )
    }
}
