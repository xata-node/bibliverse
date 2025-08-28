package com.gemini.bibliverse.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.ui.navigation.Screen
import com.gemini.bibliverse.viewmodel.MainViewModel

// Helper function to create and launch the share intent
private fun shareVerse(context: Context, verse: Verse) {
    val shareText = "\"${verse.text}\"\n- ${verse.reference}"
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, navController: NavController) {
    val verse by viewModel.currentVerse.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val context = LocalContext.current

    // Observe the current route from the NavController.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bibliverse") },
                actions = {
                    IconButton(onClick = { verse?.let { viewModel.toggleFavorite(it) } }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { verse?.let { shareVerse(context, it) } }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Verse"
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                // This helper function prevents navigating to a destination if we're already there
                val onNavigate: (String) -> Unit = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Main.route,
                        onClick = { /* Already on Home */ },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Chapters.route,
                        onClick = { onNavigate(Screen.Chapters.route) },
                        icon = { Icon(Icons.AutoMirrored.Default.List, contentDescription = "Chapters") },
                        label = { Text("Chapters") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Search.route,
                        onClick = { onNavigate(Screen.Search.route) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Favorites.route,
                        onClick = { onNavigate(Screen.Favorites.route) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Контейнер для карточки и кнопок навигации
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                var swipeProcessed by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { swipeProcessed = false },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (swipeProcessed) return@detectHorizontalDragGestures

                                    when {
                                        dragAmount > 50 -> {
                                            viewModel.showPreviousVerse()
                                            swipeProcessed = true
                                        }
                                        dragAmount < -50 -> {
                                            viewModel.showNextVerse()
                                            swipeProcessed = true
                                        }
                                    }
                                }
                            )
                        },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp, vertical = 24.dp) // Добавлен отступ для кнопок
                            .verticalScroll(rememberScrollState()), // Позволяет скроллить
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        verse?.let {
                            Text(
                                text = "\"${it.text}\"",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = it.reference,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center
                            )
                        } ?: CircularProgressIndicator()
                    }
                }

                // Кнопки навигации по бокам
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter) // Выравниваем по нижнему краю
                        .fillMaxWidth()
                        .padding(bottom = 16.dp), // Отступ снизу
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.showPreviousVerse() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Verse",
                            modifier = Modifier.size(36.dp) // Увеличенный размер
                        )
                    }
                    IconButton(onClick = { viewModel.showNextVerse() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Verse",
                            modifier = Modifier.size(36.dp) // Увеличенный размер
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.fetchAndShowNewVerse() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Another Verse")
            }
        }
    }
}
