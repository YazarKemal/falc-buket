package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.OverlayDark
import com.prompthavenai.falcibuket.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(nav: NavController) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(900), label = "splash")
    LaunchedEffect(Unit) {
        visible = true
        delay(1500)
        nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.splash_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(alpha),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(OverlayDark, androidx.compose.ui.graphics.Color.Transparent, OverlayDark))
            )
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "FalcıBuket",
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                color = Gold
            )
            Spacer(Modifier.height(8.dp))
            Text("Seni tanıyan kişisel falcın", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}
