package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.data.model.FortuneInteractionArt
import com.prompthavenai.falcibuket.data.model.FortuneType
import com.prompthavenai.falcibuket.data.model.InputMode
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.model.FortuneCatalog
import com.prompthavenai.falcibuket.ui.screens.flows.GenericBirthDataScreen
import com.prompthavenai.falcibuket.ui.screens.flows.GenericCardSelectionScreen
import com.prompthavenai.falcibuket.ui.screens.flows.GenericPhotoReadingScreen
import com.prompthavenai.falcibuket.ui.screens.flows.GenericQuestionReadingScreen
import com.prompthavenai.falcibuket.ui.screens.flows.GenericSymbolSelectionScreen
import com.prompthavenai.falcibuket.ui.screens.flows.SymbolSpec

/** Dispatches a fortune id to the reusable input flow for its [InputMode]. */
@Composable
fun FortuneFlowScreen(nav: NavController, fortuneId: String) {
    val type = remember(fortuneId) { FortuneCatalog.byId(fortuneId) }
    if (type == null) {
        UnknownFortuneScreen(nav)
        return
    }

    val back: () -> Unit = { nav.popBackStack() }
    val toResult: (String) -> Unit = { question ->
        nav.navigate(Routes.fortuneResult(type.id, question))
    }

    when (type.inputMode) {
        InputMode.PHOTO_VISION -> {
            if (type == FortuneType.COFFEE) {
                CoffeeScreen(nav)
            } else {
                GenericPhotoReadingScreen(
                    title = photoTitle(type),
                    subtitle = "Net bir fotoğraf yükle; işaretleri birlikte okuyalım.",
                    cta = "Yorumumu Gör",
                    artworkRes = FortuneInteractionArt.orCategoryArt(type),
                    onBack = back,
                    onSubmit = { toResult("") }
                )
            }
        }

        InputMode.CARD_SELECTION -> GenericCardSelectionScreen(
            title = "Kartlarını Seç",
            subtitle = type.subtitle,
            artworkRes = type.artworkRes,
            cta = "Kartlarımı Yorumla",
            onBack = back,
            onSubmit = { toResult("") }
        )

        InputMode.TEXT_INPUT -> GenericQuestionReadingScreen(
            title = type.title,
            subtitle = type.subtitle,
            prompt = "Rüyanı olabildiğince ayrıntılı anlat…",
            cta = "Rüyamı Yorumla",
            artworkRes = FortuneInteractionArt.orCategoryArt(type),
            onBack = back,
            maxChars = 4000,
            minLines = 5,
            onSubmit = { toResult(it) }
        )

        InputMode.BIRTH_DATA -> GenericBirthDataScreen(
            title = type.title,
            subtitle = type.subtitle,
            twoPeople = false,
            cta = "Yorumumu Gör",
            artworkRes = type.artworkRes,
            onBack = back,
            onSubmit = { _, _ -> toResult("") }
        )

        InputMode.TWO_PERSON_DATA -> GenericBirthDataScreen(
            title = type.title,
            subtitle = type.subtitle,
            twoPeople = true,
            cta = "Uyumumuzu Gör",
            artworkRes = type.artworkRes,
            onBack = back,
            onSubmit = { _, _ -> toResult("") }
        )

        InputMode.SYMBOL_SELECTION, InputMode.RANDOM_CAST -> GenericSymbolSelectionScreen(
            title = type.title,
            subtitle = type.subtitle,
            spec = symbolSpecFor(type),
            cta = "Yorumumu Gör",
            onBack = back,
            artworkRes = FortuneInteractionArt.orCategoryArt(type),
            onSubmit = { toResult("") }
        )

        InputMode.QUESTION_ONLY -> GenericQuestionReadingScreen(
            title = type.title,
            subtitle = type.subtitle,
            prompt = questionPrompt(type),
            cta = "Yorumumu Gör",
            artworkRes = type.artworkRes,
            onBack = back,
            onSubmit = { toResult(it) }
        )
    }
}

private fun photoTitle(type: FortuneType): String = when (type) {
    FortuneType.PALM -> "Elini Göster"
    FortuneType.TEA_LEAF -> "Yaprakları Göster"
    FortuneType.CANDLE_WAX -> "Mumunu Göster"
    else -> "Fotoğrafını Göster"
}

private fun questionPrompt(type: FortuneType): String = when (type) {
    FortuneType.LOVE -> "Aşk hayatında neyi merak ediyorsun?"
    FortuneType.CAREER_MONEY -> "Kariyer veya para konusunda neyi merak ediyorsun?"
    FortuneType.AURA -> "Auran hakkında neyi merak ediyorsun?"
    FortuneType.CHAKRA -> "Hangi enerji merkezini merak ediyorsun?"
    FortuneType.CRYSTAL_BALL -> "Kristal küreye sormak istediğin soru ne?"
    FortuneType.CLAIRVOYANCE -> "Sezgilerine danışmak istediğin konu ne?"
    else -> "Aklındaki soruyu yaz…"
}

private fun symbolSpecFor(type: FortuneType): SymbolSpec = when (type) {
    FortuneType.RUNES -> SymbolSpec(
        symbols = listOf("Fehu", "Uruz", "Ansuz", "Raido", "Kaunan", "Gebo", "Wunjo", "Sowilo"),
        randomCast = false,
        selectCount = 1
    )
    FortuneType.PENDULUM -> SymbolSpec(
        symbols = listOf("Evet", "Hayır", "Belirsiz"),
        randomCast = false,
        selectCount = 1
    )
    FortuneType.I_CHING -> SymbolSpec(
        symbols = listOf("Gök", "Toprak", "Su", "Ateş", "Rüzgar", "Göl", "Dağ", "Gök Gürültüsü"),
        randomCast = true,
        castLabel = "Çek"
    )
    FortuneType.DAISY -> SymbolSpec(
        symbols = listOf("Seviyor", "Sevmiyor"),
        randomCast = true,
        castLabel = "Papatya Kopar"
    )
    FortuneType.DICE -> SymbolSpec(
        symbols = listOf("1", "2", "3", "4", "5", "6"),
        randomCast = true,
        castLabel = "Zar At"
    )
    else -> SymbolSpec(symbols = listOf("Evet", "Hayır"), randomCast = true)
}
