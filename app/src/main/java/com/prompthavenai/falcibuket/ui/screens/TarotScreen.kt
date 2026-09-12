package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.screens.flows.GenericCardSelectionScreen

/** Legacy Tarot flow, preserved via the reusable card-selection screen. */
@Composable
fun TarotScreen(nav: NavController) {
    GenericCardSelectionScreen(
        title = "Kartlarını Seç",
        subtitle = "İçinden geldiği gibi üç kart seç.",
        artworkRes = R.drawable.tarot_fortune,
        introArtworkRes = R.drawable.tarot_selection,
        cta = "Kartlarımı Yorumla",
        onBack = { nav.popBackStack() },
        cardCount = 6,
        selectCount = 3,
        onSubmit = { nav.navigate(Routes.result("Tarot")) }
    )
}
