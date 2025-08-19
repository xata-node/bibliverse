package com.gemini.bibliverse.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

// Репозиторий для загрузки стихов из ассетов
class VerseRepository(private val context: Context) {

    suspend fun loadVerses(): List<Verse> = withContext(Dispatchers.IO) {
        val verses = mutableListOf<Verse>()
        // Загрузка стихов из Библии
        try {
            context.assets.open("kjv-scriptures.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).forEachLine { line ->
                    val parts = line.split("    ", limit = 2)
                    if (parts.size == 2) {
                        verses.add(Verse(text = parts[1].trim(), reference = parts[0].trim()))
                    }
                }
            }
            Log.d("VerseRepository", "Loaded ${verses.size} Bible verses.")
        } catch (e: Exception) {
            // Обработка ошибки, если файл не найден
            Log.e("VerseRepository", "Error loading kjv-scriptures.txt", e)
        }

        // Загрузка аффирмаций
        try {
            val startCount = verses.size
            context.assets.open("easter_eggs.txt").use { inputStream ->
                val text = inputStream.bufferedReader().use { it.readText() }
                // Заменяем все \r\n на \n для корректного разделения
                val cleanedText = text.replace("\r\n", "\n")
                val blocks = cleanedText.split("\n\n")

                blocks.forEach { block ->
                    val lines = block.lines().filter { it.isNotBlank() }
                    if (lines.isNotEmpty()) {
                        // Ищем строку, которая начинается со скобки, для ссылки
                        val verseReference = lines.find { it.startsWith("(") && it.endsWith(")") } ?: "(Affirmation)"
                        // Объединяем остальные строки в текст стиха
                        val verseText = lines.filter { it != verseReference }.joinToString("\n").trim()

                        if (verseText.isNotEmpty()) {
                            verses.add(Verse(text = verseText, reference = verseReference, isAffirmation = true))
                        }
                    }
                }
            }
            Log.d("VerseRepository", "Loaded ${verses.size - startCount} easter eggs.")
        } catch (e: Exception) {
            Log.e("VerseRepository", "Error loading easter_eggs.txt", e)
        }

        // Запасной стих, если файлы не загрузились
        if (verses.isEmpty()) {
            verses.add(Verse("For God so loved the world...", "John 3:16"))
            Log.d("VerseRepository", "Using fallback verse as no files were loaded.")
        }

        verses
    }
}
