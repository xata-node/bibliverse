package com.gemini.bibliverse.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

// Repository for loading verses from files
class VerseRepository(private val context: Context) {

    private val versesFileName = "kjv-scriptures.txt"
    private val affirmationsFileName = "affirmations.txt"

    suspend fun loadVerses(): List<Verse> = withContext(Dispatchers.IO) {
        val verses = mutableListOf<Verse>()

        verses.addAll(loadBibleVerses())
        // Load user affirmations
        val startCount = verses.size
        verses.addAll(loadAffirmations())
        Log.d("VerseRepository", "Loaded ${verses.size - startCount} affirmations.")

        if (verses.isEmpty()) {
            verses.add(Verse("For God so loved the world...", "John 3:16"))
            Log.d("VerseRepository", "Using fallback verse as no files were loaded.")
        }

        verses
    }

    suspend fun loadBibleVerses(): List<Verse> = withContext(Dispatchers.IO) {
        val bibleVerses = mutableListOf<Verse>()

        try {
            context.assets.open(versesFileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).forEachLine { line ->
                    val parts = line.split("    ", limit = 2)
                    if (parts.size == 2) {
                        bibleVerses.add(Verse(text = parts[1].trim(), reference = parts[0].trim()))
                    }
                }
            }
            Log.d("VerseRepository", "Loaded ${bibleVerses.size} Bible verses.")
        } catch (e: Exception) {
            Log.e("VerseRepository", "Error loading kjv-scriptures.txt", e)
        }

        bibleVerses
    }

    // This function reads affirmations from internal storage.
    // If the file doesn't exist, it copies it from assets first.
    private fun loadAffirmations(): List<Verse> {
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
                    // Find line that is enclosed in double parenthesis and use it as reference
                    val verseReference = lines.find { it.startsWith("((") && it.endsWith("))") } ?: "(())"
                    // Join the rest of the lines to form the verse text
                    val verseText = lines.filter { it != verseReference }.joinToString("\n").trim()

                    if (verseText.isNotEmpty()) {
                        // Provide a clean reference to the app
                        val cleanReference = verseReference.removeSurrounding("((", "))")
                        affirmations.add(Verse(text = verseText, reference = cleanReference, isAffirmation = true))
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
                    // Ensure the reference is always wrapped in double parentheses when saving
                    writer.write("((${verse.reference}))")
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
