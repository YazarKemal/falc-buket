package com.prompthavenai.falcibuket.ui.model

import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.model.AiMode
import com.prompthavenai.falcibuket.data.model.FortuneCategory
import com.prompthavenai.falcibuket.data.model.FortuneType
import com.prompthavenai.falcibuket.data.model.InputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FortuneCatalogTest {

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
    fun artworkIsDefinedForEveryTypeAndLegacyResultImagesPreserved() {
        FortuneCatalog.all.forEach { type ->
            val art = type.resultArtwork
            assertNotEquals("category art missing for ${type.name}", 0, art.categoryRes)
            assertNotEquals("result art missing for ${type.name}", 0, art.resultRes)
            assertTrue("bad aspect for ${type.name}", art.resultAspectRatio > 0f)
        }
        assertEquals(R.drawable.result_coffee_reading, FortuneType.COFFEE.resultArtwork.resultRes)
        assertEquals(R.drawable.result_tarot_reading, FortuneType.TAROT.resultArtwork.resultRes)
        assertEquals(R.drawable.result_love_insight, FortuneType.LOVE.resultArtwork.resultRes)
        assertEquals(R.drawable.result_career_insight, FortuneType.CAREER_MONEY.resultArtwork.resultRes)
    }

    @Test
    fun onlyLegacyPopularTypesParticipateInHistory() {
        assertEquals(4, FortuneCatalog.all.count { it.historyEnabled })
        FortuneCatalog.all.filter { it.historyEnabled }.forEach {
            assertTrue(it.memoryEnabled)
        }
    }
}
