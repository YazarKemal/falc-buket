package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.MysticBackground
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun PremiumScreen(nav: NavController) {
    MysticBackground(R.drawable.bg_mystic_soft_03) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 840.dp).fillMaxSize()
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
                    }
                }
                Image(
                    painterResource(R.drawable.premium_banner), null,
                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(24.dp))
                Text("Buket+", style = MaterialTheme.typography.headlineLarge, color = Gold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Daha detaylı fallar, uzun süreli hafıza ve sınırsız sohbet.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(24.dp))
                listOf(
                    "Kahve ve tarot fallarında derinlemesine yorum",
                    "Ayları kapsayan kişisel hafıza",
                    "Sınırsız Buket sohbeti",
                    "Öncelikli yeni özellikler"
                ).forEach { perk ->
                    Text(
                        "✦  $perk",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 8.dp)
                    )
                }
                Spacer(Modifier.height(28.dp))
                GoldButton(
                    "Yakında — Şimdilik Keşfet",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { nav.popBackStack() }
                )
                Text(
                    "Gerçek satın alma bir sonraki sürümde eklenecek.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
