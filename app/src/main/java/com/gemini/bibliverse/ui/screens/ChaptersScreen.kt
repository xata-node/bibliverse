package com.gemini.bibliverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.ui.navigation.Screen
import com.gemini.bibliverse.viewmodel.Book
import com.gemini.bibliverse.viewmodel.Chapter
import com.gemini.bibliverse.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(viewModel: MainViewModel, navController: NavController) {
    val books by viewModel.books.collectAsState()

    // Используем `rememberSaveable` для сохранения состояния expandedBooks при смене конфигурации
    var expandedBooks by rememberSaveable { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chapters") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(books) { book ->
                BookItem(
                    book = book,
                    viewModel = viewModel,
                    navController = navController,
                    isExpanded = expandedBooks.contains(book.name), // Проверяем, развернута ли книга
                    onToggleExpand = { // Обработчик для переключения состояния
                        expandedBooks = if (expandedBooks.contains(book.name)) {
                            expandedBooks - book.name
                        } else {
                            expandedBooks + book.name
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BookItem(
    book: Book,
    viewModel: MainViewModel,
    navController: NavController,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    // Используем `rememberSaveable` для сохранения состояния expandedChapters при смене конфигурации
    var expandedChapters by rememberSaveable { mutableStateOf(setOf<Int>()) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(book.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                book.chapters.forEach { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        viewModel = viewModel,
                        navController = navController,
                        isExpanded = expandedChapters.contains(chapter.number),
                        onToggleExpand = {
                            expandedChapters = if (expandedChapters.contains(chapter.number)) {
                                expandedChapters - chapter.number
                            } else {
                                expandedChapters + chapter.number
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterItem(
    chapter: Chapter,
    viewModel: MainViewModel,
    navController: NavController,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chapter ${chapter.number}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chapter.verses.forEach { verse ->
                    VerseItem(
                        verse = verse,
                        viewModel = viewModel,
                        onClick = {
                            viewModel.setCurrentVerse(verse)
                            navController.navigate(Screen.Main.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive =
                                        true // Often included, especially if startDestinationId is Main
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VerseItem(verse: Verse, viewModel: MainViewModel, onClick: () -> Unit) {
    // This now reactively collects the list of favorite verses.
    val favoriteVerses by viewModel.favoriteVerses.collectAsState()
    // The 'isFavorite' status is determined by checking if the current verse is in the list.
    // This will cause a recomposition ONLY when the favorites list changes.
    val isFavorite = verse in favoriteVerses

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.clickable(onClick = onClick) // Make the whole card clickable
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${verse.reference.split(":").last()}: ${verse.text}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.toggleFavorite(verse) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
