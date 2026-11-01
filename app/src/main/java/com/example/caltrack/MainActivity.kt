package com.example.caltrack

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caltrack.data.ThemeMode
import com.example.caltrack.ui.CalTrackAppShell
import com.example.caltrack.ui.SettingsViewModel
import com.example.caltrack.ui.theme.CalTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            val loadedSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val aiConnectionState by settingsViewModel.aiConnectionState.collectAsStateWithLifecycle()
            val aiConnectionDetail by settingsViewModel.aiConnectionDetail.collectAsStateWithLifecycle()
            // Nothing is drawn until the saved settings are read, so the palette never flashes.
            val settings = loadedSettings
            if (settings != null) {
                val darkTheme = when (settings.themeMode) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }
                DisposableEffect(darkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                        navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                    )
                    onDispose {}
                }
                CalTrackTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                    CalTrackAppShell(
                        settings = settings,
                        onThemeModeChange = settingsViewModel::setThemeMode,
                        onDynamicColorChange = settingsViewModel::setDynamicColor,
                        aiConnectionState = aiConnectionState,
                        aiConnectionDetail = aiConnectionDetail,
                        onSaveGeminiApiKey = settingsViewModel::saveGeminiApiKey,
                        onRemoveGeminiApiKey = settingsViewModel::removeGeminiApiKey,
                        onTestGeminiConnection = settingsViewModel::testGeminiConnection,
                    )
                }
            }
        }
    }
}

// Same scrims enableEdgeToEdge() uses by default (only visible with 3-button navigation)
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
