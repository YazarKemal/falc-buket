package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.repository.OnboardingPrefs
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.theme.*
import kotlinx.coroutines.launch

private data class Page(val image: Int, val title: String, val body: String)

@Composable
fun OnboardingScreen(nav: NavController) {
    val pages = listOf(
        Page(R.drawable.onboard_companion, "Seni Tanıyan Bir Falcı",
            "FalcıBuket, anlattıklarını zamanla öğrenir ve fallarını sana özel yorumlar."),
        Page(R.drawable.onboard_memory, "Geçmişini Hatırlar",
            "Daha önce anlattığın kişileri, olayları ve beklentilerini sonraki fallarında hatırlar."),
        Page(R.drawable.onboard_future, "Her Fal Bir Öncekinden Daha Kişisel",
            "Geçmiş yorumların ve hayatındaki gelişmeler bir araya gelerek daha tutarlı bir fal deneyimi oluşturur.")
    )
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(NightBg)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { i ->
            Box(Modifier.fillMaxSize()) {
                Image(
                    painterResource(pages[i].image), null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, OverlayDark, NightBg)))
                )
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(pages[pagerState.currentPage].title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(pages[pagerState.currentPage].body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .size(if (i == pagerState.currentPage) 22.dp else 7.dp, 7.dp)
                            .clip(CircleShape)
                            .background(if (i == pagerState.currentPage) Gold else TextMuted.copy(alpha = 0.4f))
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(0) }
                }, enabled = pagerState.currentPage > 0) {
                    Text("Geri", color = if (pagerState.currentPage > 0) TextMuted else Color.Transparent)
                }
                Spacer(Modifier.weight(1f))
                GoldButton(
                    text = if (pagerState.currentPage == pages.lastIndex) "FalcıBuket'e Başla" else "Devam",
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        if (pagerState.currentPage == pages.lastIndex) {
                            OnboardingPrefs(nav.context).complete()
                            nav.navigate(Routes.MAIN) { popUpTo(0) }
                        } else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                )
            }
        }
    }
}
