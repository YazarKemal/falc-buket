package com.prompthavenai.falcibuket.data.model

data class FortuneCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageRes: Int
)

data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val sending: Boolean = false,
    val failed: Boolean = false
)

data class VisualObservation(val observation: String, val interpretation: String)

data class TimeWindow(val topic: String, val window: String)

data class CoffeeResult(
    val title: String,
    val summary: String,
    val visualObservations: List<VisualObservation>,
    val generalEnergy: String,
    val love: String,
    val careerMoney: String,
    val nearFuture: String,
    val highlight: String,
    val timeWindows: List<TimeWindow>
)

data class ReadingEntry(
    val id: String,
    val type: String,
    val question: String?,
    val createdAtMillis: Long,
    val title: String,
    val summary: String,
    val generalEnergy: String,
    val love: String,
    val careerMoney: String,
    val nearFuture: String,
    val highlight: String,
    val visualObservations: List<VisualObservation> = emptyList(),
    val timeWindows: List<TimeWindow> = emptyList()
) {
    val dateLabel: String
        get() {
            val diff = System.currentTimeMillis() - createdAtMillis
            return when {
                diff < 24 * 60 * 60 * 1000L -> "Bugün"
                diff < 2 * 24 * 60 * 60 * 1000L -> "Dün"
                diff < 7 * 24 * 60 * 60 * 1000L -> "${diff / (24 * 60 * 60 * 1000L)} gün önce"
                else -> "${diff / (7 * 24 * 60 * 60 * 1000L)} hafta önce"
            }
        }
}

data class Reading(
    val id: String,
    val type: String,
    val dateLabel: String,
    val generalEnergy: String,
    val love: String,
    val career: String,
    val nearFuture: String,
    val highlight: String
)

data class HistoryEntry(
    val title: String,
    val dateLabel: String,
    val imageRes: Int
)
