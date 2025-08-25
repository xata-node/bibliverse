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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private var referenceSearchIndex: Map<String, List<Verse>> = emptyMap()

    // --- Affirmations State ---
    private val _affirmations = MutableStateFlow<List<Verse>>(emptyList())
    val affirmations = _affirmations.asStateFlow()
    val affirmationsEnabled = dataStoreManager.getAffirmationsEnabled()

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

    // --- УЛУЧШЕННАЯ ЛОГИКА ПОИСКА (IDE Agent's approach) ---
    // This flow reactively provides the list of verses that should be searched.
    private val searchableVerses = combine(
        _verses,
        affirmationsEnabled
    ) { allVerses, areAffirmationsEnabled ->
        if (areAffirmationsEnabled) {
            allVerses
        } else {
            allVerses.filter { !it.isAffirmation }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filteredVerses = searchQuery
        .debounce(300)
        .combine(searchableVerses) { query, verseList -> // Search the pre-filtered list
            // Сначала проверяем длину запроса здесь, в потоке данных
            if (query.trim().length < 2 && query.isNotBlank()) {
                _isSearching.value = false
                return@combine emptyList<Verse>()
            }

            if (query.isBlank() || verseList.isEmpty()) {
                _isSearching.value = false
                return@combine emptyList<Verse>()
            }

            _isSearching.value = true
            val startTime = System.currentTimeMillis()
            val trimmedQuery = query.trim().lowercase()
            // Handle empty query after trim, or query consisting only of spaces
            val queryWords = trimmedQuery.split(" ").filter { it.isNotBlank() }

            // Шаг 1: Поиск в быстром индексе по ссылкам (главы и стихи)
            val referenceResults = referenceSearchIndex[trimmedQuery]
            val results = if (referenceResults != null) {
                referenceResults.filter { verse -> verse in verseList } // Ensure result is in the searchable list
            } else {
                // Шаг 2: Если в быстром индексе нет, делаем обычный текстовый поиск
                verseList.filter { verse ->
                    if (queryWords.size == 1) {
                        // For single-word queries, keep the existing 'word-starts-with' behavior.
                        // Example: "hand" will match "hands", "handle".
                        verse.text.lowercase().containsWordStartingWith(trimmedQuery) ||
                                verse.reference.lowercase().containsWordStartingWith(trimmedQuery)
                    } else {
                        // For multi-word queries, perform a strict order phrase search.
                        // Example: "hand god" will match "hand god" but NOT "hands of god".
                        verse.text.lowercase().contains(trimmedQuery) ||
                                verse.reference.lowercase().contains(trimmedQuery)
                    }
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
            val allVerses = repository.loadVerses() // This now loads both
            _verses.value = allVerses
            _affirmations.value = allVerses.filter { it.isAffirmation } // Separate affirmations

            verseToIndexMap = allVerses.withIndex().associate { (i, v) -> v to i }

            val refIndex = allVerses.groupBy { verse ->
                verse.reference.lowercase()
                    .substringAfter(" ")
            }
            val chapters = allVerses.groupBy { it.reference.lowercase().substringAfter(" ").substringBefore(":") }

            val combinedIndex = refIndex.toMutableMap()
            chapters.forEach { (chapter, verseList) ->
                combinedIndex[chapter] = verseList
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
        if (_verses.value.isNotEmpty()) {
            // If data is already loaded, handle the notification verse immediately
            handleNotificationVerse(verse)
            _pendingVerseFromNotification.value = null // Clear it once handled
        } else {
            // If data is not yet loaded, store it to be processed by loadData later.
            _pendingVerseFromNotification.value = verse
        }
    }

    // New function to correctly handle history for notification verses
    private fun handleNotificationVerse(verse: Verse) {
        setCurrentVerse(verse)

    }

    // New private helper function to handle history updates
    private fun addVerseToHistoryAndShow(index: Int) {
        val newHistory = _verseHistory.value + index
        _verseHistory.value = newHistory
        _historyIndex.value = newHistory.lastIndex
        _currentVerse.value = _verses.value.getOrNull(index)
    }

    // This function is now the single point for setting a verse from UI or notifications
    fun setCurrentVerse(verse: Verse) {
        val index = verseToIndexMap[verse] ?: return
        addVerseToHistoryAndShow(index)
    }

    fun showInitialVerse(verseToShow: Verse? = null) {
        viewModelScope.launch {
            if (_verses.value.isEmpty()) return@launch

            // FIX 1: Check if affirmations are enabled before selecting the initial verse
            val affirmationsAreEnabled = affirmationsEnabled.first()
            val verseList = if (affirmationsAreEnabled) _verses.value else _verses.value.filter { !it.isAffirmation }

            if (verseList.isEmpty()) {
                Log.w("ViewModel", "Cannot show initial verse, list is empty after filtering.")
                _currentVerse.value = null
                return@launch
            }

            val index = if (verseToShow != null) {
                verseToIndexMap[verseToShow]
            } else {
                verseToIndexMap[verseList.random()]
            }

            _currentVerse.value = _verses.value.getOrNull(index ?: 0)
            _verseHistory.value = listOf(index ?: 0)
            _historyIndex.value = 0
        }
    }

    fun fetchAndShowNewVerse() {
        viewModelScope.launch {
            val affirmationsAreEnabled = affirmationsEnabled.first()
            val verseList = if (affirmationsAreEnabled) _verses.value else _verses.value.filter { !it.isAffirmation }

            if (verseList.size <= 1) return@launch

            val currentVerseIndex = verseToIndexMap[_currentVerse.value]
            var newIndex: Int
            do {
                val randomVerse = verseList.random()
                newIndex = verseToIndexMap[randomVerse] ?: -1
            } while (newIndex == currentVerseIndex || newIndex == -1)

            addVerseToHistoryAndShow(newIndex)
        }
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

    // --- Affirmations Logic ---
    fun setAffirmationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAffirmationsEnabled(enabled)
        }
    }

    fun addAffirmation(text: String, reference: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newAffirmation = Verse(text, reference, isAffirmation = true)
            val updatedAffirmations = _affirmations.value + newAffirmation
            repository.saveAffirmations(updatedAffirmations)

            _affirmations.value = updatedAffirmations
            _verses.value = _verses.value + newAffirmation
            verseToIndexMap = _verses.value.withIndex().associate { (i, v) -> v to i }
        }
    }

    fun removeAffirmation(affirmation: Verse) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAffirmations = _affirmations.value.filter { it != affirmation }
            repository.saveAffirmations(updatedAffirmations)

            _affirmations.value = updatedAffirmations
            _verses.value = _verses.value.filter { it != affirmation }
            verseToIndexMap = _verses.value.withIndex().associate { (i, v) -> v to i }
        }
    }

    fun editAffirmation(oldAffirmation: Verse, newText: String, newReference: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newAffirmation = Verse(newText, newReference, isAffirmation = true)
            val updatedAffirmations = _affirmations.value.map {
                if (it == oldAffirmation) newAffirmation else it
            }
            repository.saveAffirmations(updatedAffirmations)

            _affirmations.value = updatedAffirmations
            _verses.value = _verses.value.map {
                if (it == oldAffirmation) newAffirmation else it
            }
            verseToIndexMap = _verses.value.withIndex().associate { (i, v) -> v to i }
        }
    }
}

private fun String.containsWordStartingWith(queryPart: String): Boolean {
    if (queryPart.isBlank()) return true

    val lowerCaseText = this.lowercase()
    val lowerCaseQueryPart = queryPart.lowercase()

    var searchStartIndex = 0
    while (searchStartIndex <= lowerCaseText.length - lowerCaseQueryPart.length) {
        val matchIndex = lowerCaseText.indexOf(lowerCaseQueryPart, searchStartIndex)

        if (matchIndex == -1) {
            return false
        }

        // Check if the match is at the beginning of the text,
        // or if it's preceded by a non-letter/digit character (word boundary)
        if (matchIndex == 0 || !lowerCaseText[matchIndex - 1].isLetterOrDigit()) {
            return true
        }

        searchStartIndex = matchIndex + 1
    }
    return false
}

data class SearchResult(val verse: Verse, val isFavorite: Boolean)
