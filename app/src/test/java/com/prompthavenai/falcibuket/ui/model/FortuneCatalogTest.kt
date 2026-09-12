package com.prompthavenai.falcibuket.ui.model

import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.model.AiMode
import com.prompthavenai.falcibuket.data.model.FortuneCategory
import com.prompthavenai.falcibuket.data.model.FortuneInteractionArt
import com.prompthavenai.falcibuket.data.model.FortuneType
import com.prompthavenai.falcibuket.data.model.InputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FortuneCatalogTest {

    private val originalFour = setOf(
        FortuneType.COFFEE, FortuneType.TAROT, FortuneType.LOVE, FortuneType.CAREER_MONEY
    )

    private val placeholderDrawables = setOf(
        R.drawable.result_background,
        R.drawable.home_daily_hero,
        R.drawable.love_fortune,
        R.drawable.tarot_fortune,
        R.drawable.career_fortune,
        R.drawable.coffee_fortune,
        R.drawable.bg_mystic_soft_03
    )

    private val expectedCategoryArt = mapOf(
        FortuneType.KATINA to R.drawable.category_katina,
        FortuneType.PLAYING_CARDS to R.drawable.category_iskambil,
        FortuneType.LENORMAND to R.drawable.category_lenormand,
        FortuneType.ORACLE to R.drawable.category_oracle,
        FortuneType.BIRTH_CHART to R.drawable.category_birth_chart,
        FortuneType.DAILY_HOROSCOPE to R.drawable.category_horoscope,
        FortuneType.COMPATIBILITY to R.drawable.category_compatibility,
        FortuneType.NUMEROLOGY to R.drawable.category_numerology,
        FortuneType.CHINESE_ZODIAC to R.drawable.category_chinese_zodiac,
        FortuneType.MOON_READING to R.drawable.category_moon_reading,
        FortuneType.BIORHYTHM to R.drawable.category_biorhythm,
        FortuneType.RUNES to R.drawable.category_runes,
        FortuneType.I_CHING to R.drawable.category_iching,
        FortuneType.PALM to R.drawable.category_palm,
        FortuneType.DREAM to R.drawable.category_dream,
        FortuneType.DAISY to R.drawable.category_daisy,
        FortuneType.DICE to R.drawable.category_dice,
        FortuneType.TEA_LEAF to R.drawable.category_tea_leaf,
        FortuneType.CANDLE_WAX to R.drawable.category_candle_wax,
        FortuneType.PENDULUM to R.drawable.category_pendulum,
        FortuneType.AURA to R.drawable.category_aura,
        FortuneType.CHAKRA to R.drawable.category_chakra,
        FortuneType.CRYSTAL_BALL to R.drawable.category_crystal_ball,
        FortuneType.CLAIRVOYANCE to R.drawable.category_clairvoyance
    )

    @Test
    fun catalogHasExactly28Types() {
        assertEquals(28, FortuneCatalog.all.size)
        assertEquals(FortuneType.entries.size, FortuneCatalog.all.size)
    }

    @Test
    fun idsAreUniqueRouteSafeAndNonBlank() {
        val ids = FortuneCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { id ->
            assertTrue("id blank", id.isNotBlank())
            assertTrue("id not route safe: $id", Regex("^[a-z0-9_]+$").matches(id))
        }
    }

    @Test
    fun titlesAndSubtitlesArePresent() {
        FortuneCatalog.all.forEach { type ->
            assertTrue("title blank for ${type.name}", type.title.isNotBlank())
            assertTrue("subtitle blank for ${type.name}", type.subtitle.isNotBlank())
        }
    }

    @Test
    fun everyCategoryIsPopulatedAndSumsTo28() {
        val total = FortuneCategory.entries.sumOf { FortuneCatalog.forCategory(it).size }
        assertEquals(28, total)
        FortuneCategory.entries.forEach { category ->
            assertTrue("empty category $category", FortuneCatalog.forCategory(category).isNotEmpty())
        }
    }

    @Test
    fun cardsCategoryContainsExactlyFive() {
        val cards = FortuneCatalog.forCategory(FortuneCategory.CARDS).map { it.id }.toSet()
        assertEquals(
            setOf("tarot", "katina", "playing_cards", "lenormand", "oracle"),
            cards
        )
        assertEquals(5, cards.size)
    }

    @Test
    fun inputModeMappingMatchesSpec() {
        val expected = mapOf(
            InputMode.PHOTO_VISION to setOf(
                FortuneType.COFFEE, FortuneType.PALM, FortuneType.TEA_LEAF, FortuneType.CANDLE_WAX
            ),
            InputMode.CARD_SELECTION to setOf(
                FortuneType.TAROT, FortuneType.KATINA, FortuneType.PLAYING_CARDS,
                FortuneType.LENORMAND, FortuneType.ORACLE
            ),
            InputMode.TEXT_INPUT to setOf(FortuneType.DREAM),
            InputMode.BIRTH_DATA to setOf(
                FortuneType.BIRTH_CHART, FortuneType.DAILY_HOROSCOPE, FortuneType.NUMEROLOGY,
                FortuneType.CHINESE_ZODIAC, FortuneType.MOON_READING, FortuneType.BIORHYTHM
            ),
            InputMode.TWO_PERSON_DATA to setOf(FortuneType.COMPATIBILITY),
            InputMode.SYMBOL_SELECTION to setOf(FortuneType.RUNES, FortuneType.PENDULUM),
            InputMode.RANDOM_CAST to setOf(FortuneType.I_CHING, FortuneType.DAISY, FortuneType.DICE),
            InputMode.QUESTION_ONLY to setOf(
                FortuneType.LOVE, FortuneType.CAREER_MONEY, FortuneType.CRYSTAL_BALL,
                FortuneType.CLAIRVOYANCE, FortuneType.AURA, FortuneType.CHAKRA
            )
        )
        expected.forEach { (mode, types) ->
            assertEquals(types, FortuneCatalog.all.filter { it.inputMode == mode }.toSet())
        }
        assertEquals(InputMode.entries.size, expected.size)
    }

    @Test
    fun aiModeFollowsInputMode() {
        FortuneCatalog.all.forEach { type ->
            val expected = when (type.inputMode) {
                InputMode.PHOTO_VISION -> AiMode.VISION
                InputMode.CARD_SELECTION, InputMode.BIRTH_DATA, InputMode.TWO_PERSON_DATA -> AiMode.STRUCTURED
                InputMode.SYMBOL_SELECTION, InputMode.RANDOM_CAST -> AiMode.CAST
                InputMode.TEXT_INPUT, InputMode.QUESTION_ONLY -> AiMode.TEXT
            }
            assertEquals("aiMode mismatch for ${type.name}", expected, type.aiMode)
        }
    }

    @Test
    fun popularIsCuratedUniqueAndOrdered() {
        assertEquals(
            listOf(
                FortuneType.COFFEE, FortuneType.TAROT, FortuneType.LOVE,
                FortuneType.CAREER_MONEY, FortuneType.DREAM, FortuneType.BIRTH_CHART
            ),
            FortuneCatalog.popular
        )
        assertEquals(FortuneCatalog.popular.size, FortuneCatalog.popular.toSet().size)
    }

    @Test
    fun legacyAndIdResolution() {
        assertEquals(ResultTarget.Coffee, FortuneCatalog.resolveResultTarget("Kahve"))
        assertEquals(ResultTarget.Coffee, FortuneCatalog.resolveResultTarget("coffee"))
        assertEquals(ResultTarget.Fortune(FortuneType.TAROT), FortuneCatalog.resolveResultTarget("Tarot"))
        assertEquals(ResultTarget.Fortune(FortuneType.LOVE), FortuneCatalog.resolveResultTarget("Aşk"))
        assertEquals(ResultTarget.Fortune(FortuneType.CAREER_MONEY), FortuneCatalog.resolveResultTarget("Kariyer"))
        assertEquals(ResultTarget.Fortune(FortuneType.KATINA), FortuneCatalog.resolveResultTarget("katina"))
        assertEquals(ResultTarget.Daily, FortuneCatalog.resolveResultTarget("Günlük"))
        assertEquals(ResultTarget.Unknown, FortuneCatalog.resolveResultTarget(null))
        assertEquals(ResultTarget.Unknown, FortuneCatalog.resolveResultTarget(""))
        assertEquals(ResultTarget.Unknown, FortuneCatalog.resolveResultTarget("bilinmeyen"))
        assertNotNull(FortuneCatalog.byId("  TAROT  "))
    }

    @Test
    fun originalFourKeepCustomArtwork() {
        assertEquals(R.drawable.coffee_fortune, FortuneType.COFFEE.artworkRes)
        assertEquals(R.drawable.result_coffee_reading, FortuneType.COFFEE.resultArtwork.resultRes)
        assertEquals(R.drawable.tarot_fortune, FortuneType.TAROT.artworkRes)
        assertEquals(R.drawable.result_tarot_reading, FortuneType.TAROT.resultArtwork.resultRes)
        assertEquals(R.drawable.love_fortune, FortuneType.LOVE.artworkRes)
        assertEquals(R.drawable.result_love_insight, FortuneType.LOVE.resultArtwork.resultRes)
        assertEquals(R.drawable.career_fortune, FortuneType.CAREER_MONEY.artworkRes)
        assertEquals(R.drawable.result_career_insight, FortuneType.CAREER_MONEY.resultArtwork.resultRes)
    }

    @Test
    fun twentyFourNewTypesHaveRealCategoryArtwork() {
        assertEquals(24, expectedCategoryArt.size)
        expectedCategoryArt.forEach { (type, res) ->
            assertEquals("category art mismatch for ${type.name}", res, type.artworkRes)
            assertEquals("result art mismatch for ${type.name}", res, type.resultArtwork.resultRes)
            assertTrue("bad aspect for ${type.name}", type.resultArtwork.resultAspectRatio > 0f)
        }
    }

    @Test
    fun noNewTypeUsesPlaceholderArtwork() {
        FortuneCatalog.all.filterNot { it in originalFour }.forEach { type ->
            assertFalse(
                "${type.name} still on placeholder artwork",
                type.artworkRes in placeholderDrawables
            )
            assertNotEquals("${type.name} missing artwork", 0, type.artworkRes)
        }
    }

    @Test
    fun interactionArtworkMappingIsComplete() {
        assertEquals(R.drawable.palm_capture_guide, FortuneInteractionArt.forType(FortuneType.PALM))
        assertEquals(R.drawable.dream_input_header, FortuneInteractionArt.forType(FortuneType.DREAM))
        assertEquals(R.drawable.rune_selection_scene, FortuneInteractionArt.forType(FortuneType.RUNES))
        assertEquals(R.drawable.iching_cast_scene, FortuneInteractionArt.forType(FortuneType.I_CHING))
        assertEquals(R.drawable.pendulum_scene, FortuneInteractionArt.forType(FortuneType.PENDULUM))

        // Interaction scenes are only defined for those five types.
        val withInteraction = FortuneCatalog.all.filter { FortuneInteractionArt.forType(it) != null }.toSet()
        assertEquals(
            setOf(
                FortuneType.PALM, FortuneType.DREAM, FortuneType.RUNES,
                FortuneType.I_CHING, FortuneType.PENDULUM
            ),
            withInteraction
        )
        assertNull(FortuneInteractionArt.forType(FortuneType.COFFEE))
        assertEquals(R.drawable.category_tea_leaf, FortuneInteractionArt.orCategoryArt(FortuneType.TEA_LEAF))
    }

    @Test
    fun onlyLegacyPopularTypesParticipateInHistory() {
        assertEquals(4, FortuneCatalog.all.count { it.historyEnabled })
        FortuneCatalog.all.filter { it.historyEnabled }.forEach {
            assertTrue(it.memoryEnabled)
        }
    }
}
