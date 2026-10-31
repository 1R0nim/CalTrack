package com.example.caltrack

import android.app.Application
import com.example.caltrack.data.FoodRepository
import com.example.caltrack.data.SettingsRepository
import com.example.caltrack.data.local.CalTrackDatabase

class CalTrackApp : Application() {
    // Manual DI (no Hilt): app-wide singletons live here
    val repository: FoodRepository by lazy { FoodRepository(CalTrackDatabase.getInstance(this)) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
