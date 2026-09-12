package com.prompthavenai.falcibuket.data.model

import androidx.annotation.DrawableRes
import com.prompthavenai.falcibuket.R

/**
 * CENTRAL INTERACTION ARTWORK MAPPING.
 *
 * Maps a fortune type to the scene shown inside its input flow. Screens must
 * read this mapping instead of embedding drawable IDs.
 */
object FortuneInteractionArt {

    /** Returns the interaction scene for [type], or null if it has none. */
    @DrawableRes
    fun forType(type: FortuneType): Int? = when (type) {
        FortuneType.PALM -> R.drawable.palm_capture_guide
        FortuneType.DREAM -> R.drawable.dream_input_header
        FortuneType.RUNES -> R.drawable.rune_selection_scene
        FortuneType.I_CHING -> R.drawable.iching_cast_scene
        FortuneType.PENDULUM -> R.drawable.pendulum_scene
        else -> null
    }

    /** Interaction scene for [type], falling back to the catalog artwork. */
    @DrawableRes
    fun orCategoryArt(type: FortuneType): Int = forType(type) ?: type.artworkRes
}
