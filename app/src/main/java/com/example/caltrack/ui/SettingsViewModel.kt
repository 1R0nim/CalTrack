package com.example.caltrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caltrack.CalTrackApp
import com.example.caltrack.data.AppSettings
import com.example.caltrack.data.SettingsRepository
import com.example.caltrack.data.ThemeMode
import com.example.caltrack.data.ai.AiProviderException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AiConnectionState { NOT_TESTED, TESTING, CONNECTED, INVALID_KEY, QUOTA, RATE_LIMITED, UNAVAILABLE, FAILED }

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val geminiProvider: com.example.caltrack.data.ai.AiProvider,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _aiConnectionState = MutableStateFlow(AiConnectionState.NOT_TESTED)
    val aiConnectionState: StateFlow<AiConnectionState> = _aiConnectionState.asStateFlow()

    /** Gemini's own error message for the last failed test, if it gave one. */
    private val _aiConnectionDetail = MutableStateFlow<String?>(null)
    val aiConnectionDetail: StateFlow<String?> = _aiConnectionDetail.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun saveGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            repository.setGeminiApiKey(apiKey.trim())
            _aiConnectionState.value = AiConnectionState.NOT_TESTED
        }
    }

    fun removeGeminiApiKey() {
        viewModelScope.launch {
            repository.removeGeminiApiKey()
            _aiConnectionState.value = AiConnectionState.NOT_TESTED
        }
    }

    fun testGeminiConnection() {
        if (_aiConnectionState.value == AiConnectionState.TESTING) return
        viewModelScope.launch {
            _aiConnectionState.value = AiConnectionState.TESTING
            _aiConnectionDetail.value = null
            val key = repository.geminiApiKey()
            if (key.isNullOrBlank()) {
                _aiConnectionState.value = AiConnectionState.INVALID_KEY
                return@launch
            }
            _aiConnectionState.value = try {
                geminiProvider.testConnection(key)
                AiConnectionState.CONNECTED
            } catch (error: AiProviderException) {
                _aiConnectionDetail.value = error.detail
                when (error.kind) {
                    AiProviderException.Kind.INVALID_KEY -> AiConnectionState.INVALID_KEY
                    AiProviderException.Kind.QUOTA -> AiConnectionState.QUOTA
                    AiProviderException.Kind.RATE_LIMITED -> AiConnectionState.RATE_LIMITED
                    AiProviderException.Kind.UNAVAILABLE -> AiConnectionState.UNAVAILABLE
                    else -> AiConnectionState.FAILED
                }
            } catch (_: Exception) {
                AiConnectionState.FAILED
            }
        }
    }

    fun setAiConsentGranted(granted: Boolean) {
        viewModelScope.launch { repository.setAiConsentGranted(granted) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalTrackApp
                SettingsViewModel(app.settingsRepository, app.geminiProvider)
            }
        }
    }
}
