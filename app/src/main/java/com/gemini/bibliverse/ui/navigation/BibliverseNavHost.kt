package com.gemini.bibliverse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gemini.bibliverse.ui.screens.AboutScreen
import com.gemini.bibliverse.ui.screens.AffirmationsScreen
import com.gemini.bibliverse.ui.screens.FavoritesScreen
import com.gemini.bibliverse.ui.screens.MainScreen
import com.gemini.bibliverse.ui.screens.SearchScreen
import com.gemini.bibliverse.ui.screens.SettingsScreen
import com.gemini.bibliverse.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

// Определяем маршруты
sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Search : Screen("search")
    object Affirmations : Screen("affirmations")
    object About : Screen("about")
}

@Composable
fun BibliverseNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    // This LaunchedEffect listens for navigation events from the ViewModel.
    // When an event is received, it navigates to the Main screen.
    LaunchedEffect(Unit) {
        viewModel.navigateToMainScreen.collectLatest {
            navController.navigate(Screen.Main.route) {
                // ALWAYS pop all destinations until the start destination, INCLUDING the start destination itself.
                // This ensures a clean back stack. Pressing back from the MainScreen will then exit the app.
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true // <-- Crucial change here!
                    // saveState = false // Explicitly don't save state of popped screens. Default is false anyway if not specified.
                }

                // Reuse the existing MainScreen if it's already there (unlikely after popUpTo(inclusive=true),
                // but good as a safeguard).
                launchSingleTop = true

                // DO NOT restore old state for MainScreen.
                // The notification is bringing NEW data, so MainScreen should initialize with that new data.
                // restoreState = false // Explicitly don't restore state. Default is false anyway if not specified.
            }
        }
    }

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
        composable(Screen.Search.route) {
            SearchScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Affirmations.route) {
            AffirmationsScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
    }
}
