// --- Файл: ui/navigation/BibliverseNavHost.kt ---
package com.gemini.bibliverse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gemini.bibliverse.ui.screens.FavoritesScreen
import com.gemini.bibliverse.ui.screens.MainScreen
import com.gemini.bibliverse.ui.screens.SearchScreen
import com.gemini.bibliverse.ui.screens.SettingsScreen
import com.gemini.bibliverse.viewmodel.MainViewModel

// Определяем маршруты
sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Search : Screen("search") // Новый маршрут для поиска
}

@Composable
fun BibliverseNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel, navController = navController)
        }
        // Новый экран поиска
        composable(Screen.Search.route) {
            SearchScreen(viewModel = viewModel, navController = navController)
        }
    }
}