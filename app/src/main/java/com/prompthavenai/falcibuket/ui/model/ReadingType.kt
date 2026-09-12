package com.prompthavenai.falcibuket.ui.model

/**
 * UI-facing reading type. Parsed from the Turkish route label used by navigation.
 * This is intentionally independent from backend/persistence models.
 */
enum class ReadingType {
    COFFEE,
    TAROT,
    LOVE,
    CAREER,
    DAILY,
    UNKNOWN;

    companion object {
        fun fromRouteString(value: String?): ReadingType {
            val v = value?.trim() ?: return UNKNOWN
            return when {
                v.equals("Kahve", ignoreCase = true) -> COFFEE
                v.equals("Tarot", ignoreCase = true) -> TAROT
                v.equals("Aşk", ignoreCase = true) -> LOVE
                v.equals("Kariyer", ignoreCase = true) -> CAREER
                v.equals("Günlük", ignoreCase = true) -> DAILY
                else -> UNKNOWN
            }
        }
    }
}
