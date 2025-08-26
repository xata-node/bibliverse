// --- Файл: MainActivity.kt ---
package com.gemini.bibliverse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
//import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gemini.bibliverse.data.DataStoreManager
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.ui.navigation.BibliverseNavHost
import com.gemini.bibliverse.ui.theme.BibliverseTheme
import com.gemini.bibliverse.viewmodel.MainViewModel
import com.gemini.bibliverse.viewmodel.ViewModelFactory

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
            val currentTheme by dataStoreManager.getTheme().collectAsState(initial = "light")
            val isDarkTheme = currentTheme == "dark"

            BibliverseTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BibliverseNavHost(viewModel = viewModel)
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
