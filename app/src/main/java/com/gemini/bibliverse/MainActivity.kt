package com.gemini.bibliverse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.gemini.bibliverse.data.DataStoreManager
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.ui.navigation.BibliverseNavHost
import com.gemini.bibliverse.ui.navigation.Screen
import com.gemini.bibliverse.ui.theme.BibliverseTheme
import com.gemini.bibliverse.viewmodel.MainViewModel
import com.gemini.bibliverse.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Call installSplashScreen before super.onCreate() to launch splash screen.
        //installSplashScreen()

        super.onCreate(savedInstanceState)
        dataStoreManager = DataStoreManager(this)

        val viewModelFactory = ViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        handleIntent(intent)

        setContent {
            // FIX 1: Получаем системную тему для начального состояния
            val systemThemeIsDark = isSystemInDarkTheme()
            val initialTheme = if (systemThemeIsDark) "dark" else "light"

            val currentTheme by dataStoreManager.getTheme().collectAsState(initial = initialTheme)
            val isDarkTheme = currentTheme == "dark"

            // The NavController is now created and managed here, at the highest level.
            val navController = rememberNavController()

            // The LaunchedEffect for notifications is moved here to directly control the NavController.
            // This LaunchedEffect listens for navigation events from the ViewModel.
            // When an event is received, it navigates to the Main screen.
            LaunchedEffect(Unit) {
                viewModel.navigateToMainScreen.collectLatest {
                    // NOTE: The previous code was using `inclusive = true`, which was popping the
                    // MainScreen from the backstack and then recreating it, causing a "flicker".
                    // To fix this, we should not pop the MainScreen itself.
                    // The `inclusive = false` parameter ensures we only pop destinations
                    // up to the MainScreen, but leave the MainScreen itself on the stack.
                    // Then, `launchSingleTop = true` will re-use that existing instance.
                    navController.navigate(Screen.Main.route) {
                        // To avoid creating multiple copies of the same screen
                        // when we repeatedly click on an item that navigates to the main screen.
                        // ALWAYS pop all destinations until the start destination, INCLUDING the start destination itself.
                        // This ensures a clean back stack. Pressing back from the MainScreen will then exit the app.
                        popUpTo(navController.graph.startDestinationId) {
                            // This is for notifications. We always want to reuse the MainScreen.
                            inclusive = false // Don't pop Main screen itself from stack
                        }
                        // Reuse the existing MainScreen if it's already there.
                        launchSingleTop = true
                    }
                }
            }

            BibliverseTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // The NavController instance is passed down to the NavHost.
                    BibliverseNavHost(viewModel = viewModel, navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent) // It's good practice to call super
        handleIntent(intent)      // No need for null check on intent here
    }

    // FIX 1.1: Улучшенная обработка интента
    private fun handleIntent(intent: Intent) {
        if (intent.action == "SHOW_VERSE_ACTION") {
            val text = intent.getStringExtra("verse_text")
            val reference = intent.getStringExtra("verse_reference")
            if (text != null && reference != null) {
                val verse = Verse(text, reference)
                viewModel.setVerseFromNotification(verse)
            }
        }
    }
}
