package com.gemini.biblify.data

import android.content.Context
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
        } catch (e: Exception) {
            // Обработка ошибки, если файл не найден
            e.printStackTrace()
        }

        // Загрузка аффирмаций
        try {
            context.assets.open("easter_eggs.txt").use { inputStream ->
                val text = inputStream.bufferedReader().use { it.readText() }
                val blocks = text.split("\n\n")
                blocks.forEach { block ->
                    val lines = block.lines().filter { it.isNotBlank() }
                    if (lines.isNotEmpty()) {
                        val text = lines[0].trim()
                        val reference = if (lines.size > 1) lines[1].trim() else "(Affirmation)"
                        verses.add(Verse(text = text, reference = reference, isAffirmation = true))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Запасной стих, если файлы не загрузились
        if (verses.isEmpty()) {
            verses.add(Verse("For God so loved the world...", "John 3:16"))
        }

        verses
    }
}
