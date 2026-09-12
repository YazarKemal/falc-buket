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
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.FortuneCard
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.MemoryPreviewCard
import com.prompthavenai.falcibuket.ui.components.NightPromoCard
import com.prompthavenai.falcibuket.ui.components.OverlayImageCard
import com.prompthavenai.falcibuket.ui.components.SimpleGrid
import com.prompthavenai.falcibuket.ui.components.adaptiveColumns
import com.prompthavenai.falcibuket.ui.components.pressScale
import com.prompthavenai.falcibuket.ui.model.FortuneCatalog
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun HomeScreen(nav: NavController) {
    val popular = FortuneCatalog.popular
    val onOpen: (com.prompthavenai.falcibuket.data.model.FortuneType) -> Unit =
        { nav.navigate(Routes.openFortune(it)) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 1100.dp).fillMaxSize(),
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
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Popüler Fallar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.FORTUNE_CATALOG) }) {
                        Text("Tüm Fallar", color = Gold)
                    }
                }
            }
            item {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = adaptiveColumns(
                        availableWidth = maxWidth,
                        minCellWidth = 160.dp,
                        spacing = 16.dp,
                        max = 4
                    )
                    SimpleGrid(
                        items = popular,
                        columns = columns,
                        horizontalSpacing = 16.dp,
                        verticalSpacing = 16.dp
                    ) { type ->
                        FortuneCard(type, onOpen = onOpen)
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
                val memoryVm: com.prompthavenai.falcibuket.ui.viewmodel.MemoryViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel()
                val memState by memoryVm.state.collectAsState()
                LaunchedEffect(Unit) { memoryVm.load() }
                MemoryPreviewCard(memories = memState.facts)
            }
            item {
                NightPromoCard(onClick = { nav.navigate(Routes.result("Günlük")) })
            }
            item {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfacePlum)
                        .pressScale(onClick = { nav.navigate(Routes.FORTUNE_CATALOG) })
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tüm Fallar", style = MaterialTheme.typography.titleLarge, color = Gold)
                        Spacer(Modifier.height(4.dp))
                        Text("28 fal türünü keşfet", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                    Text("→", style = MaterialTheme.typography.headlineMedium, color = Gold)
                }
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
}
