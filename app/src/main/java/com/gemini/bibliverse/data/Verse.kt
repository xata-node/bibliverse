package com.gemini.bibliverse.data

// Модель данных для стиха
data class Verse(
    val text: String,
    val reference: String,
    val isAffirmation: Boolean = false
)
