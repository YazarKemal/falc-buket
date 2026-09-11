package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.model.FortuneCategory
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.MemoryPreviewCard
import com.prompthavenai.falcibuket.ui.components.OverlayImageCard
import com.prompthavenai.falcibuket.ui.components.pressScale
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun HomeScreen(nav: NavController) {
    val categories = listOf(
        FortuneCategory("coffee", "Kahve Falı", "Fincanındaki işaretleri keşfet", R.drawable.coffee_fortune),
        FortuneCategory("tarot", "Tarot", "Kartların sana ne söylüyor?", R.drawable.tarot_fortune),
        FortuneCategory("love", "Aşk Falı", "Kalbindeki sorulara bak", R.drawable.love_fortune),
        FortuneCategory("career", "Kariyer & Para", "Önündeki fırsatları keşfet", R.drawable.career_fortune)
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("İyi akşamlar ✨", style = MaterialTheme.typography.bodyMedium)
                    Text("Bugün sana ne söylüyor?", style = MaterialTheme.typography.headlineMedium)
                }
                Image(
                    painterResource(R.drawable.teller_avatar), "Profil",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .pressScale(onClick = {}),
                    contentScale = ContentScale.Crop
                )
            }
        }
        item {
            OverlayImageCard(
                painterResource(R.drawable.home_daily_hero), "Günün Falı",
                modifier = Modifier.fillMaxWidth().height(340.dp),
                overlayTop = true,
                onClick = { nav.navigate(Routes.result("Günlük")) }
            ) {
                Text("Günün Falı", style = MaterialTheme.typography.headlineMedium, color = Gold)
                Spacer(Modifier.height(6.dp))
                Text("Bugünün enerjisini ve seni bekleyen gelişmeleri keşfet.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                GoldButton("Falımı Gör", onClick = { nav.navigate(Routes.result("Günlük")) })
            }
        }
        item { Text("Falını Seç", style = MaterialTheme.typography.titleLarge) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                categories.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { cat ->
                            OverlayImageCard(
                                painterResource(cat.imageRes), cat.title,
                                modifier = Modifier.weight(1f).height(190.dp),
                                onClick = { nav.navigate(routeFor(cat.id)) }
                            ) {
                                Text(cat.title, style = MaterialTheme.typography.titleMedium)
                                Text(cat.subtitle, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            OverlayImageCard(
                painterResource(R.drawable.teller_avatar), "Buket",
                modifier = Modifier.fillMaxWidth().height(230.dp),
                onClick = { nav.navigate(Routes.CHAT) }
            ) {
                Text("FalcıBuket ile Konuş", style = MaterialTheme.typography.titleLarge, color = Gold)
                Spacer(Modifier.height(4.dp))
                Text("Aklındaki soruyu sor. Buket seni ve geçmiş konuşmalarınızı zamanla tanır.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                GoldButton("Sohbete Başla", onClick = { nav.navigate(Routes.CHAT) })
            }
        }
        item {
            MemoryPreviewCard(memories = emptyList())
        }
        item {
            OverlayImageCard(
                painterResource(R.drawable.premium_banner), "Buket+",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                onClick = { nav.navigate(Routes.PREMIUM) }
            ) {
                Text("Buket+", style = MaterialTheme.typography.titleLarge, color = Gold)
                Spacer(Modifier.height(4.dp))
                Text("Daha detaylı fallar, uzun süreli hafıza ve sınırsız sohbet.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                GoldButton("Buket+'ı Keşfet", onClick = { nav.navigate(Routes.PREMIUM) })
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

private fun routeFor(id: String) = when (id) {
    "coffee" -> Routes.COFFEE
    "tarot" -> Routes.TAROT
    "love" -> Routes.LOVE
    else -> Routes.CAREER
}
