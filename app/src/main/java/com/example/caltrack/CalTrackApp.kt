package com.example.caltrack

import android.app.Application
import com.example.caltrack.data.FoodRepository
import com.example.caltrack.data.OpenFoodFactsClient
import com.example.caltrack.data.SettingsRepository
import com.example.caltrack.data.ai.GeminiProvider
import com.example.caltrack.data.local.CalTrackDatabase

class CalTrackApp : Application() {
    // Manual DI (no Hilt): app-wide singletons live here
    val repository: FoodRepository by lazy { FoodRepository(CalTrackDatabase.getInstance(this)) }
    val openFoodFactsClient: OpenFoodFactsClient by lazy { OpenFoodFactsClient() }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val geminiProvider: GeminiProvider by lazy { GeminiProvider() }
}
