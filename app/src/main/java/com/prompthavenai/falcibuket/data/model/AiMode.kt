package com.prompthavenai.falcibuket.data.model

/**
 * Intended processing mode for a fortune type. This describes the future AI
 * pipeline, not whether a backend is currently wired up.
 */
enum class AiMode {
    VISION,
    TEXT,
    STRUCTURED,
    CAST
}
