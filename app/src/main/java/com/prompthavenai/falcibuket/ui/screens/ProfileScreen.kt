package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.components.AchievementSection
import com.prompthavenai.falcibuket.ui.components.ProfileIdentityHeader
import com.prompthavenai.falcibuket.ui.components.StreakCard
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun ProfileScreen() {
    val memoryVm: com.prompthavenai.falcibuket.ui.viewmodel.MemoryViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val memState by memoryVm.state.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { memoryVm.load() }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Buket'in Hafızası Temizlensin mi?", color = TextCream) },
            text = {
                Text(
                    "Buket seninle ilgili hatırladığı tüm detayları silecek. " +
                        "Falların silinmeyecek, ancak sonraki fallar daha genel geçer olacak.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        memoryVm.clearMemory()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Rose)
                ) { Text("Temizle") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Vazgeç", color = Gold) }
            },
            containerColor = NightBgAlt
        )
    }

    LaunchedEffect(memState.cleared) {
        if (memState.cleared) memoryVm.consumeCleared()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 1000.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            ProfileIdentityHeader(factCount = memState.facts.size)

            Spacer(Modifier.height(16.dp))
            StreakCard()

            Spacer(Modifier.height(20.dp))
            AchievementSection()

            Spacer(Modifier.height(20.dp))
            listOf(
                Triple("Buket'in Hafızası", Icons.Outlined.AutoAwesome, Gold),
                Triple("Bildirimler", Icons.Outlined.Notifications, Rose),
                Triple("Gizlilik", Icons.Outlined.Lock, Gold),
                Triple("Buket+", Icons.Outlined.Star, Rose),
                Triple("Hakkında", Icons.Outlined.Info, Gold)
            ).forEach { (label, icon, tint) ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfacePlum)
                        .clickable {
                            if (label == "Buket'in Hafızası") showClearDialog = true
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = tint)
                    Spacer(Modifier.width(14.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium, color = TextCream, modifier = Modifier.weight(1f))
                    if (label == "Buket'in Hafızası" && memState.facts.isNotEmpty()) {
                        Text("${memState.facts.size}", style = MaterialTheme.typography.labelLarge, color = Gold)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            HorizontalDivider(color = TextMuted.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Fal deneyimini kişiselleştirmek için Buket, konuşmalarındaki önemli detayları hafızasında tutar. " +
                    "Fotoğrafların yalnızca analiz için işlenir ve kalıcı olarak saklanmaz.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            Text("FalcıBuket v0.2.0 — AI", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(20.dp))
        }
    }
}
