package com.example.caltrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** Wallpaper-based colors (Android 12+); off uses the fixed green palette. */
    val dynamicColor: Boolean = true,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        // An unreadable settings file falls back to defaults instead of crashing the app
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val savedMode = prefs[ThemeModeKey]
            AppSettings(
                themeMode = ThemeMode.entries.firstOrNull { it.name == savedMode } ?: ThemeMode.System,
                dynamicColor = prefs[DynamicColorKey] ?: true,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[ThemeModeKey] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DynamicColorKey] = enabled }
    }

    private companion object {
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val DynamicColorKey = booleanPreferencesKey("dynamic_color")
    }
}
