package com.gemini.biblify.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

// Модель данных для стиха
data class Verse(
    val text: String,
    val reference: String,
    val isAffirmation: Boolean = false
)
