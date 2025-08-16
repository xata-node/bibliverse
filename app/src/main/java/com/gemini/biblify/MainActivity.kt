// --- Файл: MainActivity.kt ---
package com.gemini.biblify

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
import com.gemini.biblify.data.DataStoreManager
import com.gemini.biblify.data.Verse
import com.gemini.biblify.ui.navigation.BiblifyNavHost
import com.gemini.biblify.ui.theme.BiblifyTheme
import com.gemini.biblify.viewmodel.MainViewModel
import com.gemini.biblify.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStoreManager = DataStoreManager(this)

        val viewModelFactory = ViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        handleIntent(intent)

        setContent {
            val currentTheme by dataStoreManager.getTheme().collectAsState(initial = "dark")
            val isDarkTheme = currentTheme == "dark"

            BiblifyTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiblifyNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
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
