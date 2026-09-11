package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.model.Reading
import com.prompthavenai.falcibuket.data.repository.FortuneRepositoryHolder
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ReadingResultScreen(nav: NavController, type: String) {
    val repo = remember { FortuneRepositoryHolder.repo }
    var reading by remember { mutableStateOf<Reading?>(null) }
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repo.analyzeReading(type, null).collectLatest { reading = it; revealed = true }
    }

    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmer.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a"
    )

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.result_background), null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.35f
        )
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(NightBg.copy(alpha = 0.85f), NightBg.copy(alpha = 0.95f))))
        )

        if (reading == null) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painterResource(R.drawable.teller_avatar), null,
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(60.dp)).alpha(shimmerAlpha),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(18.dp))
                Text("Buket fallarını yorumluyor…", style = MaterialTheme.typography.titleMedium, color = Gold, textAlign = TextAlign.Center)
                Text("Yıldızlar hizalanıyor ✨", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        } else {
            val r = reading!!
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .animateEnterExit(revealed).padding(20.dp)
            ) {
                Text("$type Falı", style = MaterialTheme.typography.headlineMedium, color = Gold)
                Text(r.dateLabel, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(Modifier.height(20.dp))
                Section("Genel Enerji", r.generalEnergy)
                Section("Aşk", r.love)
                Section("Kariyer & Para", r.career)
                Section("Yakın Gelecek", r.nearFuture)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.horizontalGradient(listOf(RoseSoft.copy(alpha = 0.35f), GoldDeep.copy(alpha = 0.3f))))
                        .padding(18.dp)
                ) {
                    Column {
                        Text("Buket'in Dikkatini Çeken", style = MaterialTheme.typography.titleMedium, color = Rose)
                        Spacer(Modifier.height(6.dp))
                        Text(r.highlight, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GoldButton("Bu falı kaydet", modifier = Modifier.weight(1f), onClick = { repo.saveReading(r) })
                    OutlinedButton(
                        onClick = { nav.navigate("chat") },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                    ) { Text("Buket'e sor") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Box(
        Modifier.fillMaxWidth().padding(bottom = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfacePlumSoft)
            .padding(18.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Gold)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun Modifier.animateEnterExit(revealed: Boolean): Modifier =
    this.alpha(if (revealed) 1f else 0f)
