package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.components.pressScale
import com.prompthavenai.falcibuket.ui.theme.*
import com.prompthavenai.falcibuket.ui.viewmodel.ReadingsViewModel

@Composable
fun ReadingsScreen() {
    val vm: ReadingsViewModel = viewModel()
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Image(
                painterResource(R.drawable.history_header), null,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
            Text("Fallarım", style = MaterialTheme.typography.headlineMedium, color = Gold)
        }
        if (state.loading && state.readings.isEmpty()) {
            item { Text("Falların yükleniyor…", color = TextMuted, style = MaterialTheme.typography.bodyMedium) }
        }
        if (!state.loading && !state.backendConfigured && state.readings.isEmpty()) {
            item {
                Text(
                    "Falların bu cihazda saklanamıyor çünkü Firebase bu sürümde bağlanmadı. " +
                        "Backend yapılandırıldığında falların burada listelenecek.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        items(state.readings) { entry ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfacePlum)
                    .pressScale(onClick = {})
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(vm.typeImage(entry.type)), null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(entry.title.ifBlank { "${entry.type} Falı" }, style = MaterialTheme.typography.titleMedium, color = TextCream)
                    Text(entry.dateLabel, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    if (entry.summary.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            entry.summary, style = MaterialTheme.typography.bodySmall,
                            color = TextMuted, maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
