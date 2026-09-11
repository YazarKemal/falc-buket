package com.prompthavenai.falcibuket.data.model

data class FortuneCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageRes: Int
)

data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)

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
