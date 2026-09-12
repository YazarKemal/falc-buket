package com.prompthavenai.falcibuket.data.model

import androidx.annotation.DrawableRes
import com.prompthavenai.falcibuket.R

/**
 * Resolved artwork for a fortune type.
 *
 * [categoryRes] is used by catalog cards and [resultRes] by the result header.
 * [resultAspectRatio] keeps the result header free of letterboxing.
 */
data class FortuneArtworkSpec(
    @DrawableRes val categoryRes: Int,
    @DrawableRes val resultRes: Int,
    val resultAspectRatio: Float
)

/**
 * CENTRAL ARTWORK MAPPING.
 *
 * This is the only place fortune type -> drawable is defined. The 24 imported
 * `category_*` assets are mapped here; the original four types keep their
 * custom artwork.
 */
object FortuneArtwork {

    private const val LANDSCAPE = 1536f / 1024f
    private const val COFFEE = 1200f / 1000f
    private const val CATEGORY = 1448f / 1086f

    fun spec(type: FortuneType): FortuneArtworkSpec = when (type) {
        // --- Original four (custom artwork preserved) ---
        FortuneType.COFFEE -> FortuneArtworkSpec(R.drawable.coffee_fortune, R.drawable.result_coffee_reading, COFFEE)
        FortuneType.TAROT -> FortuneArtworkSpec(R.drawable.tarot_fortune, R.drawable.result_tarot_reading, LANDSCAPE)
        FortuneType.LOVE -> FortuneArtworkSpec(R.drawable.love_fortune, R.drawable.result_love_insight, LANDSCAPE)
        FortuneType.CAREER_MONEY -> FortuneArtworkSpec(R.drawable.career_fortune, R.drawable.result_career_insight, LANDSCAPE)

        // --- Cards ---
        FortuneType.KATINA -> FortuneArtworkSpec(R.drawable.category_katina, R.drawable.category_katina, CATEGORY)
        FortuneType.PLAYING_CARDS -> FortuneArtworkSpec(R.drawable.category_iskambil, R.drawable.category_iskambil, CATEGORY)
        FortuneType.LENORMAND -> FortuneArtworkSpec(R.drawable.category_lenormand, R.drawable.category_lenormand, CATEGORY)
        FortuneType.ORACLE -> FortuneArtworkSpec(R.drawable.category_oracle, R.drawable.category_oracle, CATEGORY)

        // --- Astrology & Numbers ---
        FortuneType.BIRTH_CHART -> FortuneArtworkSpec(R.drawable.category_birth_chart, R.drawable.category_birth_chart, CATEGORY)
        FortuneType.DAILY_HOROSCOPE -> FortuneArtworkSpec(R.drawable.category_horoscope, R.drawable.category_horoscope, CATEGORY)
        FortuneType.COMPATIBILITY -> FortuneArtworkSpec(R.drawable.category_compatibility, R.drawable.category_compatibility, CATEGORY)
        FortuneType.NUMEROLOGY -> FortuneArtworkSpec(R.drawable.category_numerology, R.drawable.category_numerology, CATEGORY)
        FortuneType.CHINESE_ZODIAC -> FortuneArtworkSpec(R.drawable.category_chinese_zodiac, R.drawable.category_chinese_zodiac, CATEGORY)
        FortuneType.MOON_READING -> FortuneArtworkSpec(R.drawable.category_moon_reading, R.drawable.category_moon_reading, CATEGORY)
        FortuneType.BIORHYTHM -> FortuneArtworkSpec(R.drawable.category_biorhythm, R.drawable.category_biorhythm, CATEGORY)

        // --- Ancient Methods ---
        FortuneType.RUNES -> FortuneArtworkSpec(R.drawable.category_runes, R.drawable.category_runes, CATEGORY)
        FortuneType.I_CHING -> FortuneArtworkSpec(R.drawable.category_iching, R.drawable.category_iching, CATEGORY)
        FortuneType.PALM -> FortuneArtworkSpec(R.drawable.category_palm, R.drawable.category_palm, CATEGORY)
        FortuneType.DREAM -> FortuneArtworkSpec(R.drawable.category_dream, R.drawable.category_dream, CATEGORY)
        FortuneType.DAISY -> FortuneArtworkSpec(R.drawable.category_daisy, R.drawable.category_daisy, CATEGORY)
        FortuneType.DICE -> FortuneArtworkSpec(R.drawable.category_dice, R.drawable.category_dice, CATEGORY)
        FortuneType.TEA_LEAF -> FortuneArtworkSpec(R.drawable.category_tea_leaf, R.drawable.category_tea_leaf, CATEGORY)
        FortuneType.CANDLE_WAX -> FortuneArtworkSpec(R.drawable.category_candle_wax, R.drawable.category_candle_wax, CATEGORY)

        // --- Energy & Intention ---
        FortuneType.PENDULUM -> FortuneArtworkSpec(R.drawable.category_pendulum, R.drawable.category_pendulum, CATEGORY)
        FortuneType.AURA -> FortuneArtworkSpec(R.drawable.category_aura, R.drawable.category_aura, CATEGORY)
        FortuneType.CHAKRA -> FortuneArtworkSpec(R.drawable.category_chakra, R.drawable.category_chakra, CATEGORY)

        // --- Other ---
        FortuneType.CRYSTAL_BALL -> FortuneArtworkSpec(R.drawable.category_crystal_ball, R.drawable.category_crystal_ball, CATEGORY)
        FortuneType.CLAIRVOYANCE -> FortuneArtworkSpec(R.drawable.category_clairvoyance, R.drawable.category_clairvoyance, CATEGORY)
    }

    fun categoryRes(type: FortuneType): Int = spec(type).categoryRes

    fun resultRes(type: FortuneType): Int = spec(type).resultRes
}
