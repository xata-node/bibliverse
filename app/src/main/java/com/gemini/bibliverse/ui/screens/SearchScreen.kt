package com.gemini.bibliverse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.ui.navigation.Screen
import com.gemini.bibliverse.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: MainViewModel, navController: NavController) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    // Get LocalFocusManager for closing On-Screen keyboard
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onSearchQueryChange("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search by word or reference...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surface
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // 1. Forcefully close On-Screen Keyboard immediately to prevent delay
                            focusManager.clearFocus()
                            // 2. Then navigate back
                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (searchQuery.trim().length >= 2 && searchResults.isEmpty()) {
                Text("No verses found.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults, key = { it.verse.text + it.verse.reference }) { result ->
                        SearchResultItem(
                            verse = result.verse,
                            query = searchQuery, // Pass query for highlighting
                            isFavorite = result.isFavorite,
                            onToggleFavorite = { viewModel.toggleFavorite(result.verse) },
                            onClick = {
                                // 1. Forcefully close On-Screen Keyboard to prevent delay
                                focusManager.clearFocus()
                                // 2. Вызываем централизованную функцию в ViewModel.
                                // ViewModel сначала установит стих, а затем отправит событие
                                // для навигации, которое будет обработано в MainActivity.
                                // Это решает проблему "гонки состояний" и гарантирует плавную анимацию.
                                viewModel.selectVerseAndNavigateHome(result.verse)
                                // 3. Prevent showing previous results when screen is opened next time.
                                viewModel.onSearchQueryChange("")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    verse: Verse,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    query: String
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Use the new HighlightedText composable
                HighlightedText(
                    fullText = verse.text,
                    query = query,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                HighlightedText(
                    fullText = verse.reference,
                    query = query,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A composable that displays text with parts of it highlighted based on a search query.
 * This function handles multiple keywords, partial words, and respects word boundaries.
 *
 * @param fullText The complete text to be displayed.
 * @param query The search query string. Keywords will be highlighted.
 * @param style The text style to be applied to the entire text.
 */
@Composable
private fun HighlightedText(
    fullText: String,
    query: String,
    style: TextStyle
) {
    // If the query is blank, no need to highlight anything.
    if (query.isBlank()) {
        Text(fullText, style = style)
        return
    }

    val annotatedString = buildAnnotatedString {
        // Get the individual keywords from the query.
        val queryWords = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        // Create a regex pattern that finds any of the keywords.
        // Regex.escape ensures that special characters in the query don't break the regex.
        // The `|` acts as an "OR" operator.
        val pattern = queryWords.joinToString(separator = "|") { Regex.escape(it) }.toRegex(RegexOption.IGNORE_CASE)

        // Find all matches of the pattern in the full text.
        val matches = pattern.findAll(fullText).toList()
        var lastIndex = 0

        matches.forEach { matchResult ->
            // Append the text before the current match.
            append(fullText.substring(lastIndex, matchResult.range.first))

            // Append the matched keyword with the highlight style.
            withStyle(
                style = SpanStyle(
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                append(matchResult.value)
            }
            // Update the last index to the end of the current match.
            lastIndex = matchResult.range.last + 1
        }

        // Append any remaining text after the last match.
        if (lastIndex < fullText.length) {
            append(fullText.substring(lastIndex))
        }
    }
    Text(annotatedString, style = style)
}
