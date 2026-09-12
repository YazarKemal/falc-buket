package com.prompthavenai.falcibuket.ui.visuals

import androidx.annotation.DrawableRes
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.model.ReadingType

/** Visual specification for a reading header artwork. */
data class ReadingArtwork(
    @DrawableRes val imageRes: Int,
    val aspectRatio: Float
)

/** Single source of truth mapping a [ReadingType] to its header artwork. */
fun ReadingType.artwork(): ReadingArtwork = when (this) {
    ReadingType.COFFEE -> ReadingArtwork(R.drawable.result_coffee_reading, 1200f / 1000f)
    ReadingType.TAROT -> ReadingArtwork(R.drawable.result_tarot_reading, 1536f / 1024f)
    ReadingType.LOVE -> ReadingArtwork(R.drawable.result_love_insight, 1536f / 1024f)
    ReadingType.CAREER -> ReadingArtwork(R.drawable.result_career_insight, 1536f / 1024f)
    ReadingType.DAILY -> ReadingArtwork(R.drawable.result_background, 1536f / 1024f)
    ReadingType.UNKNOWN -> ReadingArtwork(R.drawable.result_background, 1536f / 1024f)
}
