package com.gemini.bibliverse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.ui.navigation.Screen
import com.gemini.bibliverse.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: MainViewModel, navController: NavController) {
    val favorites by viewModel.favoriteVerses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("You haven't added any favorites yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites, key = { it.text + it.reference }) { verse ->
                    // FIX 1.1: Используем компонент, который всегда показывает "избранное"
                    FavoriteItem(
                        verse = verse,
                        onRemove = { viewModel.toggleFavorite(verse) },
                        onClick = {
                            viewModel.setCurrentVerse(verse)
                            navController.navigate(Screen.Main.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true // Often included, especially if startDestinationId is Main
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

// Компонент для элемента в списке избранного
@Composable
fun FavoriteItem(verse: Verse, onRemove: () -> Unit, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.clickable(onClick = onClick) // Make the whole card clickable
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(verse.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(verse.reference, style = MaterialTheme.typography.bodySmall)
            }
            // Иконка всегда заполнена и работает на удаление
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Remove from Favorites",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
