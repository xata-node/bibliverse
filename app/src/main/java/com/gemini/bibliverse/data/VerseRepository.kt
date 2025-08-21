package com.gemini.bibliverse.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

// Репозиторий для загрузки стихов из ассетов
class VerseRepository(private val context: Context) {

    private val versesFileName = "kjv-scriptures.txt"
    private val affirmationsFileName = "affirmations.txt"

    suspend fun loadVerses(): List<Verse> = withContext(Dispatchers.IO) {
        val verses = mutableListOf<Verse>()
        // Загрузка стихов из Библии
        try {
            context.assets.open(versesFileName).use { inputStream ->
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

        // --- NEW AFFIRMATIONS LOGIC ---
        val startCount = verses.size
        verses.addAll(loadAffirmationsFromFile())
        Log.d("VerseRepository", "Loaded ${verses.size - startCount} affirmations.")

        if (verses.isEmpty()) {
            verses.add(Verse("For God so loved the world...", "John 3:16"))
            Log.d("VerseRepository", "Using fallback verse as no files were loaded.")
        }

        verses
    }

    // This function reads affirmations from internal storage.
    // If the file doesn't exist, it copies it from assets first.
    private fun loadAffirmationsFromFile(): List<Verse> {
        val affirmations = mutableListOf<Verse>()
        val file = File(context.filesDir, affirmationsFileName)

        if (!file.exists()) {
            try {
                context.assets.open(affirmationsFileName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("VerseRepository", "Copied affirmations.txt from assets to internal storage.")
            } catch (e: Exception) {
                Log.e("VerseRepository", "Error copying affirmations.txt from assets", e)
                return emptyList()
            }
        }

        try {
            val text = file.readText()
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
                        affirmations.add(Verse(text = verseText, reference = verseReference, isAffirmation = true))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VerseRepository", "Error reading affirmations.txt from internal storage", e)
        }
        return affirmations
    }

    suspend fun saveAffirmations(affirmations: List<Verse>) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, affirmationsFileName)
        try {
            file.writer().use { writer ->
                affirmations.forEachIndexed { index, verse ->
                    writer.write(verse.text)
                    writer.write("\n")
                    writer.write(verse.reference)
                    if (index < affirmations.size - 1) {
                        writer.write("\n\n")
                    }
                }
            }
            Log.d("VerseRepository", "Successfully saved ${affirmations.size} affirmations.")
        } catch (e: Exception) {
            Log.e("VerseRepository", "Error saving affirmations.txt", e)
        }
    }
}
