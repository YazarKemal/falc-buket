package com.prompthavenai.falcibuket.data.model

/**
 * Interpretation family for a reading type. The 28 catalog types collapse into
 * these 7 families so the backend never needs 28 unrelated implementations.
 */
enum class ReadingFamily {
    VISION,
    CARDS,
    DREAM_TEXT,
    ASTROLOGY_CALCULATED,
    COMPATIBILITY,
    SYMBOL_CAST,
    QUESTION_INTUITION;

    companion object {
        /** Exhaustive mapping; adding a [FortuneType] forces a compile-time update here. */
        fun of(type: FortuneType): ReadingFamily = when (type) {
            FortuneType.COFFEE, FortuneType.PALM, FortuneType.TEA_LEAF, FortuneType.CANDLE_WAX -> VISION
            FortuneType.TAROT, FortuneType.KATINA, FortuneType.PLAYING_CARDS,
            FortuneType.LENORMAND, FortuneType.ORACLE -> CARDS
            FortuneType.DREAM -> DREAM_TEXT
            FortuneType.BIRTH_CHART, FortuneType.DAILY_HOROSCOPE, FortuneType.NUMEROLOGY,
            FortuneType.CHINESE_ZODIAC, FortuneType.MOON_READING, FortuneType.BIORHYTHM -> ASTROLOGY_CALCULATED
            FortuneType.COMPATIBILITY -> COMPATIBILITY
            FortuneType.RUNES, FortuneType.I_CHING, FortuneType.PENDULUM,
            FortuneType.DAISY, FortuneType.DICE -> SYMBOL_CAST
            FortuneType.LOVE, FortuneType.CAREER_MONEY, FortuneType.CRYSTAL_BALL,
            FortuneType.CLAIRVOYANCE, FortuneType.AURA, FortuneType.CHAKRA -> QUESTION_INTUITION
        }
    }
}

enum class ImageRole { PRIMARY, CUP, SAUCER }

enum class ImageReferenceKind { LOCAL_ATTACHMENT, PRIVATE_OBJECT }

data class ImageReference(
    val kind: ImageReferenceKind,
    val id: String
)

/**
 * Metadata/reference only — never the pixels themselves. Android [android.graphics.Bitmap]
 * and image bytes are resolved in the media layer and are not part of the domain.
 */
data class ImagePayloadDescriptor(
    val assetId: String,
    val role: ImageRole,
    val mimeType: String,
    val byteCount: Long,
    val reference: ImageReference,
    val width: Int? = null,
    val height: Int? = null
)

data class CardSelection(
    val position: Int,
    val cardId: String? = null,
    val reversed: Boolean = false
)

data class SymbolSelection(
    val symbolId: String,
    val position: Int
)

data class PlaceData(
    val label: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** Birth data with an ISO date; localized UI text is normalized before it reaches here. */
data class PersonData(
    val birthDate: String,
    val name: String? = null,
    val birthTime: String? = null,
    val timeZone: String? = null,
    val place: PlaceData? = null
)

data class AdditionalInputs(
    val dreamText: String? = null,
    val deckId: String? = null,
    val deckVersion: String? = null,
    val spreadId: String? = null,
    val castMethod: String? = null,
    val targetDate: String? = null
)

/**
 * Generic, typed submission for every reading type. This is the single domain
 * boundary that will eventually replace the per-type mock repository calls
 * without rewriting the UI.
 */
data class ReadingRequest(
    val readingType: FortuneType,
    val requestId: String,
    val question: String? = null,
    val selectedCards: List<CardSelection> = emptyList(),
    val selectedSymbols: List<SymbolSelection> = emptyList(),
    val birthData: PersonData? = null,
    val secondPersonData: PersonData? = null,
    val imagePayload: List<ImagePayloadDescriptor> = emptyList(),
    val additionalInputs: AdditionalInputs? = null,
    val schemaVersion: Int = 1
)
