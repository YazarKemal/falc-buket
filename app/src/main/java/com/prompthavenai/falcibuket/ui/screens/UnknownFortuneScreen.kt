package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.NightBg
import com.prompthavenai.falcibuket.ui.theme.TextCream
import com.prompthavenai.falcibuket.ui.theme.TextMuted

@Composable
fun UnknownFortuneScreen(
    nav: NavController,
    message: String = "Bu fal türü bulunamadı."
) {
    Box(
        Modifier.fillMaxSize().background(NightBg).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(message, style = MaterialTheme.typography.titleLarge, color = TextCream, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tüm fal türlerinden birini seçerek devam edebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            GoldButton(
                "Tüm Fallar",
                modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp),
                onClick = { nav.navigate(Routes.FORTUNE_CATALOG) }
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { nav.popBackStack() }) { Text("Geri dön", color = Gold) }
        }
    }
}
