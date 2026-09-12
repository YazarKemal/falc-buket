package com.prompthavenai.falcibuket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.NightBg
import com.prompthavenai.falcibuket.ui.theme.TextCream

/** Shared scrollable scaffold for the generic reading input flows. */
@Composable
fun ReadingFlowScaffold(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 840.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier.fillMaxSize().background(NightBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Gold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}
