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
 * This is the only place fortune type -> drawable is defined. When a new
 * `category_*` asset is imported, replace the placeholder in the matching
 * branch (the future asset name is noted on each line).
 */
object FortuneArtwork {

    private const val LANDSCAPE = 1536f / 1024f
    private const val COFFEE = 1200f / 1000f

    fun spec(type: FortuneType): FortuneArtworkSpec = when (type) {
        // --- Popular (already imported) ---
        FortuneType.COFFEE -> FortuneArtworkSpec(R.drawable.coffee_fortune, R.drawable.result_coffee_reading, COFFEE)
        FortuneType.TAROT -> FortuneArtworkSpec(R.drawable.tarot_fortune, R.drawable.result_tarot_reading, LANDSCAPE)
        FortuneType.LOVE -> FortuneArtworkSpec(R.drawable.love_fortune, R.drawable.result_love_insight, LANDSCAPE)
        FortuneType.CAREER_MONEY -> FortuneArtworkSpec(R.drawable.career_fortune, R.drawable.result_career_insight, LANDSCAPE)

        // --- Cards (awaiting category_* assets) ---
        FortuneType.KATINA -> FortuneArtworkSpec(R.drawable.love_fortune, R.drawable.result_love_insight, LANDSCAPE) // TODO category_katina
        FortuneType.PLAYING_CARDS -> FortuneArtworkSpec(R.drawable.tarot_fortune, R.drawable.result_tarot_reading, LANDSCAPE) // TODO category_iskambil
        FortuneType.LENORMAND -> FortuneArtworkSpec(R.drawable.tarot_fortune, R.drawable.result_tarot_reading, LANDSCAPE) // TODO category_lenormand
        FortuneType.ORACLE -> FortuneArtworkSpec(R.drawable.tarot_fortune, R.drawable.result_tarot_reading, LANDSCAPE) // TODO category_oracle

        // --- Astrology & Numbers ---
        FortuneType.BIRTH_CHART -> FortuneArtworkSpec(R.drawable.home_daily_hero, R.drawable.result_background, LANDSCAPE) // TODO category_birth_chart
        FortuneType.DAILY_HOROSCOPE -> FortuneArtworkSpec(R.drawable.home_daily_hero, R.drawable.result_background, LANDSCAPE) // TODO category_horoscope
        FortuneType.COMPATIBILITY -> FortuneArtworkSpec(R.drawable.love_fortune, R.drawable.result_background, LANDSCAPE) // TODO category_compatibility
        FortuneType.NUMEROLOGY -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_numerology
        FortuneType.CHINESE_ZODIAC -> FortuneArtworkSpec(R.drawable.home_daily_hero, R.drawable.result_background, LANDSCAPE) // TODO category_chinese_zodiac
        FortuneType.MOON_READING -> FortuneArtworkSpec(R.drawable.home_daily_hero, R.drawable.result_background, LANDSCAPE) // TODO category_moon_reading
        FortuneType.BIORHYTHM -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_biorhythm

        // --- Ancient Methods ---
        FortuneType.RUNES -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_runes
        FortuneType.I_CHING -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_iching
        FortuneType.PALM -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_palm
        FortuneType.DREAM -> FortuneArtworkSpec(R.drawable.home_daily_hero, R.drawable.result_background, LANDSCAPE) // TODO category_dream
        FortuneType.DAISY -> FortuneArtworkSpec(R.drawable.love_fortune, R.drawable.result_background, LANDSCAPE) // TODO category_daisy
        FortuneType.DICE -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_dice
        FortuneType.TEA_LEAF -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_tea_leaf
        FortuneType.CANDLE_WAX -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_candle_wax

        // --- Energy & Intention ---
        FortuneType.PENDULUM -> FortuneArtworkSpec(R.drawable.result_background, R.drawable.result_background, LANDSCAPE) // TODO category_pendulum
        FortuneType.AURA -> FortuneArtworkSpec(R.drawable.bg_mystic_soft_03, R.drawable.result_background, LANDSCAPE) // TODO category_aura
        FortuneType.CHAKRA -> FortuneArtworkSpec(R.drawable.bg_mystic_soft_03, R.drawable.result_background, LANDSCAPE) // TODO category_chakra

        // --- Other ---
        FortuneType.CRYSTAL_BALL -> FortuneArtworkSpec(R.drawable.home_daily_hero, R.drawable.result_background, LANDSCAPE) // TODO category_crystal_ball
        FortuneType.CLAIRVOYANCE -> FortuneArtworkSpec(R.drawable.home_daily_hero, R.drawable.result_background, LANDSCAPE) // TODO category_clairvoyance
    }

    fun categoryRes(type: FortuneType): Int = spec(type).categoryRes

    fun resultRes(type: FortuneType): Int = spec(type).resultRes
}
