package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.screens.flows.GenericQuestionReadingScreen

/** Legacy Love flow, preserved via the reusable question screen. */
@Composable
fun LoveScreen(nav: NavController) {
    GenericQuestionReadingScreen(
        title = "Kalbindeki Soruyu Sor",
        subtitle = "Aşk hayatında neyi merak ediyorsun?",
        prompt = "Aşk hayatında neyi merak ediyorsun?",
        cta = "Aşk Falıma Bak",
        artworkRes = R.drawable.love_fortune,
        onBack = { nav.popBackStack() },
        onSubmit = { nav.navigate(Routes.result("Aşk")) }
    )
}

/** Legacy Career & Money flow, preserved via the reusable question screen. */
@Composable
fun CareerScreen(nav: NavController) {
    GenericQuestionReadingScreen(
        title = "Önündeki Yolu Keşfet",
        subtitle = "Kariyer veya para konusunda neyi merak ediyorsun?",
        prompt = "Kariyer veya para konusunda neyi merak ediyorsun?",
        cta = "Kariyer Falıma Bak",
        artworkRes = R.drawable.career_fortune,
        onBack = { nav.popBackStack() },
        onSubmit = { nav.navigate(Routes.result("Kariyer")) }
    )
}
