package com.prompthavenai.falcibuket.ui.model

import com.prompthavenai.falcibuket.data.model.FortuneCategory
import com.prompthavenai.falcibuket.data.model.FortuneType

/** Destination chosen for a `result/{type}` or `fortune_result/{id}` argument. */
sealed interface ResultTarget {
    data object Coffee : ResultTarget
    data class Fortune(val type: FortuneType) : ResultTarget
    data object Daily : ResultTarget
    data object Unknown : ResultTarget
}

/**
 * Catalog helpers over the 28 [FortuneType] entries. "Popular" is a curated
 * ordered list, not category equality, so Tarot can be popular and a card type.
 */
object FortuneCatalog {

    val all: List<FortuneType> = FortuneType.entries

    val popular: List<FortuneType> = listOf(
        FortuneType.COFFEE,
        FortuneType.TAROT,
        FortuneType.LOVE,
        FortuneType.CAREER_MONEY,
        FortuneType.DREAM,
        FortuneType.BIRTH_CHART
    )

    fun byId(id: String?): FortuneType? {
        val value = id?.trim() ?: return null
        if (value.isEmpty()) return null
        return all.firstOrNull { it.id.equals(value, ignoreCase = true) }
    }

    fun forCategory(category: FortuneCategory): List<FortuneType> =
        all.filter { it.category == category }

    fun categoriesWithTypes(): List<Pair<FortuneCategory, List<FortuneType>>> =
        FortuneCategory.entries.map { it to forCategory(it) }

    /** Resolves both stable ids and legacy Turkish route labels. */
    fun fromLegacy(label: String?): FortuneType? {
        val value = label?.trim() ?: return null
        return when {
            value.equals("Kahve", ignoreCase = true) -> FortuneType.COFFEE
            value.equals("Tarot", ignoreCase = true) -> FortuneType.TAROT
            value.equals("Aşk", ignoreCase = true) -> FortuneType.LOVE
            value.equals("Kariyer", ignoreCase = true) -> FortuneType.CAREER_MONEY
            value.equals("Kariyer & Para", ignoreCase = true) -> FortuneType.CAREER_MONEY
            else -> byId(value)
        }
    }

    fun resolveResultTarget(value: String?): ResultTarget {
        val label = value?.trim() ?: return ResultTarget.Unknown
        if (label.isEmpty()) return ResultTarget.Unknown
        if (label.equals("Günlük", ignoreCase = true)) return ResultTarget.Daily
        if (label.equals("Kahve", ignoreCase = true)) return ResultTarget.Coffee
        val type = fromLegacy(label) ?: return ResultTarget.Unknown
        return if (type == FortuneType.COFFEE) ResultTarget.Coffee else ResultTarget.Fortune(type)
    }
}
