package com.prompthavenai.falcibuket.ui.screens.flows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.ReadingFlowScaffold
import com.prompthavenai.falcibuket.ui.components.ReadingPreviewNotice
import com.prompthavenai.falcibuket.ui.components.SimpleGrid
import com.prompthavenai.falcibuket.ui.components.adaptiveColumns
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextCream

/** Configuration for a symbol/cast flow. */
data class SymbolSpec(
    val symbols: List<String>,
    val randomCast: Boolean,
    val selectCount: Int = 1,
    val castLabel: String = "Çek"
)

/** Reusable SYMBOL_SELECTION / RANDOM_CAST flow. */
@Composable
fun GenericSymbolSelectionScreen(
    title: String,
    subtitle: String,
    spec: SymbolSpec,
    cta: String,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var castOutcome by rememberSaveable { mutableStateOf("") }
    val random = remember { java.util.Random() }

    ReadingFlowScaffold(title = title, subtitle = subtitle, onBack = onBack, maxWidth = 1000.dp) {
        if (spec.randomCast) {
            GoldButton(
                spec.castLabel,
                modifier = Modifier.fillMaxWidth(),
                onClick = { castOutcome = spec.symbols[random.nextInt(spec.symbols.size)] }
            )
            Spacer(Modifier.height(20.dp))
            if (castOutcome.isNotBlank()) {
                Text("Sonuç: $castOutcome", style = MaterialTheme.typography.headlineMedium, color = Gold)
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = adaptiveColumns(
                    availableWidth = maxWidth,
                    minCellWidth = 110.dp,
                    spacing = 10.dp,
                    max = 6
                )
                SimpleGrid(
                    items = spec.symbols,
                    columns = columns,
                    horizontalSpacing = 10.dp,
                    verticalSpacing = 10.dp
                ) { symbol ->
                    SymbolChip(
                        label = symbol,
                        selected = symbol in selected,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            selected = when {
                                symbol in selected -> selected - symbol
                                selected.size < spec.selectCount -> selected + symbol
                                else -> selected
                            }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        GoldButton(
            cta,
            enabled = if (spec.randomCast) castOutcome.isNotBlank() else selected.size == spec.selectCount,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSubmit(castOutcome.ifBlank { selected.joinToString(", ") }) }
        )
        Spacer(Modifier.height(12.dp))
        ReadingPreviewNotice()
    }
}

@Composable
private fun SymbolChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Gold.copy(alpha = 0.25f) else SurfacePlum)
            .border(
                1.dp,
                if (selected) Gold else Gold.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Gold else TextCream,
            textAlign = TextAlign.Center
        )
    }
}
