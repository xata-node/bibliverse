package com.gemini.bibliverse.viewmodel

import android.app.Activity
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.gemini.bibliverse.billing.BillingManager
import com.gemini.bibliverse.data.DataStoreManager
import com.gemini.bibliverse.data.Verse
import com.gemini.bibliverse.data.VerseRepository
import com.gemini.bibliverse.notifications.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(FlowPreview::class)
class MainViewModel(private val application: Application) : ViewModel() {

    private val repository = VerseRepository(application)
    private val dataStoreManager = DataStoreManager(application)
    private val notificationScheduler = NotificationScheduler(application)

    // --- ИНТЕГРАЦИЯ BILLING MANAGER ---
    private val billingManager = BillingManager(application)
    val donationProducts = billingManager.products
    val billingMessage = billingManager.message

    private val _verses = MutableStateFlow<List<Verse>>(emptyList())
    private var verseToIndexMap: Map<Verse, Int> = emptyMap()

    // --- НОВЫЙ БЫСТРЫЙ ИНДЕКС ДЛЯ ПОИСКА ПО ССЫЛКАМ ---
    private var referenceSearchIndex: Map<String, List<Verse>> = emptyMap()

    private val _currentVerse = MutableStateFlow<Verse?>(null)
    val currentVerse = _currentVerse.asStateFlow()

    private val _favorites = dataStoreManager.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val isFavorite = combine(currentVerse, _favorites) { verse, favIndices ->
        verse?.let { v -> verseToIndexMap[v] in favIndices } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val favoriteVerses = combine(_favorites, _verses) { favIndices, verses ->
        favIndices.mapNotNull { verses.getOrNull(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _verseHistory = MutableStateFlow<List<Int>>(emptyList())
    private val _historyIndex = MutableStateFlow(0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // --- УЛУЧШЕННАЯ ЛОГИКА ПОИСКА ---
    private val filteredVerses = searchQuery
        .debounce(300) // Небольшая задержка перед началом поиска
        .combine(_verses) { query, verseList ->
            if (query.isBlank()) {
                _isSearching.value = false
                return@combine emptyList<Verse>()
            }
            if (verseList.isEmpty()) {
                _isSearching.value = false
                return@combine emptyList<Verse>()
            }

            val startTime = System.currentTimeMillis()
            val trimmedQuery = query.trim().lowercase()
            _isSearching.value = true

            // Шаг 1: Поиск в быстром индексе по ссылкам (главы и стихи)
            val referenceResults = referenceSearchIndex[trimmedQuery]
            val results = if (referenceResults != null) {
                referenceResults
            } else {
                // Шаг 2: Если в быстром индексе нет, делаем обычный текстовый поиск
                verseList.filter { verse ->
                    verse.text.lowercase().contains(trimmedQuery) ||
                            verse.reference.lowercase().contains(trimmedQuery)
                }
            }

            val endTime = System.currentTimeMillis()
            Log.d("SearchDebug", "Search for '$query' took ${endTime - startTime} ms. Found ${results.size} items.")
            _isSearching.value = false
            results
        }
        .flowOn(Dispatchers.Default) // ВЫПОЛНЯЕМ ФИЛЬТРАЦИЮ В ФОНОВОМ ПОТОКЕ

    val searchResults = combine(
        filteredVerses,
        _favorites
    ) { filtered, favIndices ->
        filtered.map { verse ->
            val index = verseToIndexMap[verse]
            SearchResult(verse, index != null && index in favIndices)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val theme = dataStoreManager.getTheme()
    val notificationSettings = dataStoreManager.getNotificationSettings()

    init {
        loadData()
        billingManager.startConnection() // Запускаем подключение к биллингу Google Play
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            _verses.value = repository.loadVerses()
            verseToIndexMap = _verses.value.withIndex().associate { (i, v) -> v to i }

            // --- СОЗДАНИЕ БЫСТРОГО ИНДЕКСА ДЛЯ ССЫЛОК ---
            referenceSearchIndex = _verses.value.groupBy { verse ->
                verse.reference.lowercase()
                    .substringAfter(" ") // Берем только "2:2" из "Genesis 2:2"
            }
            // Также добавляем поиск только по главе, например "2"
            val chapters = _verses.value.groupBy { it.reference.lowercase().substringAfter(" ").substringBefore(":") }

            val combinedIndex = referenceSearchIndex.toMutableMap()
            chapters.forEach { (chapter, verses) ->
                combinedIndex[chapter] = verses
            }

            referenceSearchIndex = combinedIndex

            val initialVerse = _pendingVerseFromNotification.value
            if (initialVerse != null) {
                // This call handles adding the notification verse to history
                handleNotificationVerse(initialVerse)
                _pendingVerseFromNotification.value = null
            } else {
                showInitialVerse()
            }
            _isSearching.value = false
        }
    }

    private val _pendingVerseFromNotification = MutableStateFlow<Verse?>(null)
    fun setVerseFromNotification(verse: Verse) {
        if (_verses.value.isEmpty()) {
            _pendingVerseFromNotification.value = verse
        } else {
            // If data is already loaded, handle the notification verse immediately
            handleNotificationVerse(verse)
        }
    }

    // New function to correctly handle history for notification verses
    private fun handleNotificationVerse(verse: Verse) {
        val index = verseToIndexMap[verse] ?: return // Verse not found
        val newHistory = _verseHistory.value + index
        _verseHistory.value = newHistory
        _historyIndex.value = newHistory.lastIndex
        _currentVerse.value = _verses.value.getOrNull(index)
    }

    fun showInitialVerse(verseToShow: Verse? = null) {
        if (_verses.value.isEmpty()) return

        val index = if (verseToShow != null) {
            verseToIndexMap[verseToShow]
        } else {
            null
        } ?: Random.nextInt(_verses.value.size)

        _currentVerse.value = _verses.value.getOrNull(index)
        _verseHistory.value = listOf(index)
        _historyIndex.value = 0
    }

    fun fetchAndShowNewVerse() {
        if (_verses.value.size <= 1) return
        val currentVerseIndex = verseToIndexMap[_currentVerse.value]
        var newIndex: Int
        do {
            newIndex = Random.nextInt(_verses.value.size)
        } while (newIndex == currentVerseIndex)

        val newHistory = _verseHistory.value + newIndex
        _verseHistory.value = newHistory
        _historyIndex.value = newHistory.lastIndex
        _currentVerse.value = _verses.value[newIndex]
    }

    fun showNextVerse() {
        val history = _verseHistory.value
        if (_historyIndex.value < history.lastIndex) {
            _historyIndex.value++
            val nextIndex = history[_historyIndex.value]
            _currentVerse.value = _verses.value[nextIndex]
        } else {
            fetchAndShowNewVerse()
        }
    }

    fun showPreviousVerse() {
        if (_historyIndex.value > 0) {
            _historyIndex.value--
            val previousIndex = _verseHistory.value[_historyIndex.value]
            _currentVerse.value = _verses.value[previousIndex]
        }
    }

    fun toggleFavorite(verse: Verse) {
        viewModelScope.launch {
            val verseIndex = verseToIndexMap[verse] ?: return@launch

            val currentFavorites = _favorites.value.toMutableSet()
            if (currentFavorites.contains(verseIndex)) {
                currentFavorites.remove(verseIndex)
            } else {
                currentFavorites.add(verseIndex)
            }
            dataStoreManager.saveFavorites(currentFavorites)
        }
    }

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveTheme(if (isDark) "dark" else "light")
        }
    }

    fun setNotifications(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dataStoreManager.saveNotificationSettings(enabled, hour, minute)
                if (enabled) {
                    notificationScheduler.scheduleDailyVerse(hour, minute)
                } else {
                    notificationScheduler.cancelDailyVerse()
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // --- Функции для донатов ---
    fun initiateDonation(activity: Activity, productDetails: ProductDetails) {
        billingManager.launchPurchaseFlow(activity, productDetails)
    }

    fun clearBillingMessage() {
        billingManager.clearMessage()
    }
}

data class SearchResult(val verse: Verse, val isFavorite: Boolean)
