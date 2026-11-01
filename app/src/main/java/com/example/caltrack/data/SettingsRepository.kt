package com.example.caltrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.caltrack.data.ai.ApiKeyCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** Wallpaper-based colors (Android 12+); off uses the fixed CalTrack palettes. */
    val dynamicColor: Boolean = true,
    val hasGeminiApiKey: Boolean = false,
    val aiConsentGranted: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.settingsDataStore
    private val apiKeyCipher = ApiKeyCipher()

    val settings: Flow<AppSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val savedMode = prefs[ThemeModeKey]
            AppSettings(
                themeMode = ThemeMode.entries.firstOrNull { it.name == savedMode } ?: ThemeMode.System,
                dynamicColor = prefs[DynamicColorKey] ?: true,
                hasGeminiApiKey = !prefs[EncryptedGeminiKey].isNullOrBlank(),
                aiConsentGranted = prefs[AiConsentKey] ?: false,
            )
        }

    fun hasGeminiApiKey(): Flow<Boolean> = settings.map { it.hasGeminiApiKey }

    suspend fun geminiApiKey(): String? {
        val encrypted = dataStore.data.first()[EncryptedGeminiKey] ?: return null
        return try {
            apiKeyCipher.decrypt(encrypted)
        } catch (_: Exception) {
            // A restored or invalidated Keystore key cannot decrypt this ciphertext; discard it safely.
            removeGeminiApiKey()
            null
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        require(apiKey.isNotBlank())
        val encrypted = apiKeyCipher.encrypt(apiKey)
        dataStore.edit { it[EncryptedGeminiKey] = encrypted }
    }

    suspend fun removeGeminiApiKey() {
        dataStore.edit { it.remove(EncryptedGeminiKey) }
    }

    suspend fun setAiConsentGranted(granted: Boolean) {
        dataStore.edit { it[AiConsentKey] = granted }
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
        val EncryptedGeminiKey = stringPreferencesKey("gemini_api_key_ciphertext")
        val AiConsentKey = booleanPreferencesKey("ai_food_assistant_consent")
    }
}
