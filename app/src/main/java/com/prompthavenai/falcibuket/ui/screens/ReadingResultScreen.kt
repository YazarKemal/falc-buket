package com.prompthavenai.falcibuket.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.model.CoffeeResult
import com.prompthavenai.falcibuket.data.model.FortuneType
import com.prompthavenai.falcibuket.data.model.Reading
import com.prompthavenai.falcibuket.data.repository.FortuneRepositoryHolder
import com.prompthavenai.falcibuket.ui.components.CoffeeLoadingVisual
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.MysticBackground
import com.prompthavenai.falcibuket.ui.components.ReadingArtworkHeader
import com.prompthavenai.falcibuket.ui.components.ReadingPreviewNotice
import com.prompthavenai.falcibuket.ui.model.FortuneCatalog
import com.prompthavenai.falcibuket.ui.model.ResultTarget
import com.prompthavenai.falcibuket.ui.theme.*
import com.prompthavenai.falcibuket.ui.viewmodel.CoffeeUiState
import com.prompthavenai.falcibuket.ui.viewmodel.CoffeeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val loadingStages = listOf(
    "Fincanına bakıyorum…",
    "İşaretleri inceliyorum…",
    "Geçmiş fallarınla bağlantı kuruyorum…"
)

private const val ResultMaxWidth = 840

/** Presentation data for a generic (non-coffee) result. */
private data class FortuneDisplay(
    val title: String,
    @DrawableRes val artworkRes: Int,
    val aspectRatio: Float,
    val historyEnabled: Boolean
)

private val DailyDisplay = FortuneDisplay(
    title = "Günlük Falı",
    artworkRes = R.drawable.result_background,
    aspectRatio = 1536f / 1024f,
    historyEnabled = true
)

private fun displayFor(type: FortuneType) = FortuneDisplay(
    title = type.title,
    artworkRes = type.resultArtwork.resultRes,
    aspectRatio = type.resultArtwork.resultAspectRatio,
    historyEnabled = type.historyEnabled
)

@Composable
fun ReadingResultScreen(nav: NavController, type: String, question: String? = null) {
    when (val target = FortuneCatalog.resolveResultTarget(type)) {
        ResultTarget.Coffee -> CoffeeResultContent(nav)
        is ResultTarget.Fortune -> GenericResultContent(nav, displayFor(target.type), target.type.id, question)
        ResultTarget.Daily -> GenericResultContent(nav, DailyDisplay, "Günlük", question)
        ResultTarget.Unknown -> UnknownFortuneScreen(nav)
    }
}

@Composable
private fun CoffeeResultContent(nav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: CoffeeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = context as androidx.lifecycle.ViewModelStoreOwner
    )
    val state by vm.state.collectAsState()

    MysticBackground(R.drawable.bg_mystic_soft_02) {
        when (val s = state) {
            is CoffeeUiState.Loading -> {
                var stage by remember { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(2600)
                        stage = (stage + 1).coerceAtMost(loadingStages.lastIndex)
                    }
                }
                Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CoffeeLoadingVisual()
                    Spacer(Modifier.height(20.dp))
                    Text(
                        loadingStages[stage],
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Bu birkaç saniye sürebilir ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is CoffeeUiState.Error -> {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.error.userMessage, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = TextCream)
                    Spacer(Modifier.height(20.dp))
                    GoldButton("Tekrar Dene", onClick = { vm.retry() })
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { nav.popBackStack() }) { Text("Geri dön", color = TextMuted) }
                }
            }
            is CoffeeUiState.Success -> CoffeeResultBody(nav, s.result)
            CoffeeUiState.Idle -> {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Fal oturumu bulunamadı. Lütfen fincan fotoğraflarını yeniden seç.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = TextCream
                    )
                    Spacer(Modifier.height(20.dp))
                    GoldButton("Geri dön", onClick = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun CoffeeResultBody(nav: NavController, r: CoffeeResult) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    Box(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.widthIn(max = ResultMaxWidth.dp).fillMaxWidth()
                .alpha(if (revealed) 1f else 0f).padding(20.dp)
        ) {
            ReadingArtworkHeader(
                FortuneType.COFFEE.resultArtwork.resultRes,
                FortuneType.COFFEE.resultArtwork.resultAspectRatio
            )
            Spacer(Modifier.height(18.dp))
            Text(r.title, style = MaterialTheme.typography.headlineMedium, color = Gold)
            Spacer(Modifier.height(8.dp))
            Text(r.summary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))

            if (r.visualObservations.isNotEmpty()) {
                Text("Fincanda Beliren İşaretler", style = MaterialTheme.typography.titleMedium, color = Rose)
                Spacer(Modifier.height(10.dp))
                r.visualObservations.forEach { obs ->
                    Box(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfacePlumSoft)
                            .padding(18.dp)
                    ) {
                        Column {
                            Text("✦ ${obs.observation}", style = MaterialTheme.typography.bodyMedium, color = TextCream)
                            Spacer(Modifier.height(6.dp))
                            Text(obs.interpretation, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Section("Genel Enerji", r.generalEnergy)
            Section("Aşk", r.love)
            Section("Kariyer & Para", r.careerMoney)
            Section("Yakın Gelecek", r.nearFuture)

            if (r.timeWindows.isNotEmpty()) {
                Text("Zaman Pencereleri", style = MaterialTheme.typography.titleMedium, color = Gold)
                Spacer(Modifier.height(10.dp))
                r.timeWindows.forEach { tw ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("•  ", color = Gold)
                        Text("${tw.topic}: ${tw.window}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

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
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GoldButton("Buket'e sor", modifier = Modifier.weight(1f), onClick = { nav.navigate("chat") })
                OutlinedButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                ) { Text("Tamam") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GenericResultContent(
    nav: NavController,
    display: FortuneDisplay,
    repoType: String,
    question: String?
) {
    val repo = remember { FortuneRepositoryHolder.repo }
    var reading by remember { mutableStateOf<Reading?>(null) }
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(repoType, question) {
        repo.analyzeReading(repoType, question).collectLatest {
            reading = it
            revealed = true
        }
    }

    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmer.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a"
    )

    MysticBackground(R.drawable.bg_mystic_soft_02) {
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
                Text("Örnek yorum hazırlanıyor…", style = MaterialTheme.typography.titleMedium, color = Gold, textAlign = TextAlign.Center)
                Text("Bu bir önizlemedir ✨", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        } else {
            val r = reading!!
            Box(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    Modifier.widthIn(max = ResultMaxWidth.dp).fillMaxWidth()
                        .alpha(if (revealed) 1f else 0f).padding(20.dp)
                ) {
                    ReadingArtworkHeader(display.artworkRes, display.aspectRatio)
                    Spacer(Modifier.height(18.dp))
                    Text(display.title, style = MaterialTheme.typography.headlineMedium, color = Gold)
                    Text(r.dateLabel, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Spacer(Modifier.height(12.dp))
                    ReadingPreviewNotice()
                    Spacer(Modifier.height(16.dp))
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
                    Spacer(Modifier.height(16.dp))
                    if (display.historyEnabled) {
                        Text(
                            "Kaydetme bu sürümde simüle edilir; gerçek arşiv bağlantısı yakında.",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Text(
                            "Bu önizleme kaydedilmez.",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (display.historyEnabled) {
                            GoldButton("Bu falı kaydet", modifier = Modifier.weight(1f), onClick = { repo.saveReading(r) })
                        } else {
                            GoldButton("Buket'e sor", modifier = Modifier.weight(1f), onClick = { nav.navigate("chat") })
                        }
                        OutlinedButton(
                            onClick = { nav.popBackStack() },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                        ) { Text("Tamam") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
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
