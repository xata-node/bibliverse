package com.gemini.biblify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val FAVORITES_KEY = stringSetPreferencesKey("favorites")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
    }

    fun getTheme(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "light" // Светлая тема по умолчанию
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = theme
        }
    }

    fun getFavorites(): Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITES_KEY]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun saveFavorites(favorites: Set<Int>) {
        context.dataStore.edit { settings ->
            settings[FAVORITES_KEY] = favorites.map { it.toString() }.toSet()
        }
    }

    data class NotificationSettings(val enabled: Boolean, val hour: Int, val minute: Int)

    // FIX 1.C: Добавлено поле minute с значением по умолчанию 0
    fun getNotificationSettings(): Flow<NotificationSettings> = context.dataStore.data.map { preferences ->
        NotificationSettings(
            enabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: false,
            hour = preferences[NOTIFICATION_HOUR_KEY] ?: 8,
            minute = preferences[NOTIFICATION_MINUTE_KEY] ?: 0
        )
    }

    // FIX 1.A: Функция теперь принимает и сохраняет минуты
    suspend fun saveNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { settings ->
            settings[NOTIFICATIONS_ENABLED_KEY] = enabled
            settings[NOTIFICATION_HOUR_KEY] = hour
            settings[NOTIFICATION_MINUTE_KEY] = minute
        }
    }
}
