package com.prompthavenai.falcibuket.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Centers content horizontally and caps its width for comfortable tablet reading. */
@Composable
fun AdaptiveWidth(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
            content()
        }
    }
}

/** Column count for a grid given available width, minimum cell width and spacing. */
fun adaptiveColumns(
    availableWidth: Dp,
    minCellWidth: Dp,
    spacing: Dp,
    max: Int
): Int {
    if (availableWidth <= 0.dp) return 1
    val perCell = minCellWidth + spacing
    if (perCell <= 0.dp) return 1
    val count = ((availableWidth + spacing) / perCell).toInt()
    return count.coerceIn(1, max)
}

/**
 * Small, non-scrolling grid built from chunked rows. Safe to nest inside a
 * vertically scrolling parent (unlike LazyVerticalGrid).
 */
@Composable
fun <T> SimpleGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    itemContent: @Composable (T) -> Unit
) {
    val cols = columns.coerceAtLeast(1)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
        items.chunked(cols).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                rowItems.forEach { item ->
                    Box(Modifier.weight(1f)) { itemContent(item) }
                }
                repeat(cols - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
