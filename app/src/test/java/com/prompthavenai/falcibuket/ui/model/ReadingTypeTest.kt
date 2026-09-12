package com.prompthavenai.falcibuket.ui.model

import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.visuals.artwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingTypeTest {

    @Test
    fun canonicalRouteLabelsMapToTypes() {
        assertEquals(ReadingType.COFFEE, ReadingType.fromRouteString("Kahve"))
        assertEquals(ReadingType.TAROT, ReadingType.fromRouteString("Tarot"))
        assertEquals(ReadingType.LOVE, ReadingType.fromRouteString("Aşk"))
        assertEquals(ReadingType.CAREER, ReadingType.fromRouteString("Kariyer"))
        assertEquals(ReadingType.DAILY, ReadingType.fromRouteString("Günlük"))
    }

    @Test
    fun trimsWhitespaceAndIgnoresCase() {
        assertEquals(ReadingType.COFFEE, ReadingType.fromRouteString("  Kahve  "))
        assertEquals(ReadingType.TAROT, ReadingType.fromRouteString("TAROT"))
        assertEquals(ReadingType.DAILY, ReadingType.fromRouteString("günlük"))
    }

    @Test
    fun missingOrUnknownValuesReturnUnknown() {
        assertEquals(ReadingType.UNKNOWN, ReadingType.fromRouteString(null))
        assertEquals(ReadingType.UNKNOWN, ReadingType.fromRouteString(""))
        assertEquals(ReadingType.UNKNOWN, ReadingType.fromRouteString("   "))
        assertEquals(ReadingType.UNKNOWN, ReadingType.fromRouteString("Coffee"))
        assertEquals(ReadingType.UNKNOWN, ReadingType.fromRouteString("Rüya"))
    }

    @Test
    fun eachTypeHasItsExpectedArtwork() {
        assertEquals(R.drawable.result_coffee_reading, ReadingType.COFFEE.artwork().imageRes)
        assertEquals(R.drawable.result_tarot_reading, ReadingType.TAROT.artwork().imageRes)
        assertEquals(R.drawable.result_love_insight, ReadingType.LOVE.artwork().imageRes)
        assertEquals(R.drawable.result_career_insight, ReadingType.CAREER.artwork().imageRes)
        assertEquals(R.drawable.result_background, ReadingType.DAILY.artwork().imageRes)
        assertEquals(R.drawable.result_background, ReadingType.UNKNOWN.artwork().imageRes)
    }

    @Test
    fun artworkAspectRatiosArePositive() {
        ReadingType.entries.forEach { type ->
            assertTrue("aspect ratio must be positive for $type", type.artwork().aspectRatio > 0f)
        }
    }
}
