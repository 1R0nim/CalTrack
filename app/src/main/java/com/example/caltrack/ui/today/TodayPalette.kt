package com.example.caltrack.ui.today

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Today accents; the base background is shared by every destination. */
internal object TodayPalette {
    val background = Color(0xFF10141A)
    val calorieCard = Color(0xFFBBCBF0)
    val calorieText = Color(0xFF344365)

    val carbsCard = Color(0xFF465879)
    val fatCard = Color(0xFFE5C7F3)
    val proteinCard = Color(0xFF303648)
    val distributionCard = Color(0xFF1D1D2A)
    val foodCard = Color(0xFF151621)

    val primaryText = Color(0xFFDCE5FF)
    val secondaryText = Color(0xFFB5B8C6)
    val carbsAccent = Color(0xFFAFC4FF)
    val fatAccent = Color(0xFFD8B4F8)
    val proteinAccent = Color(0xFFA7B5C7)

    val navigationSurface = Color(0xFF1D2938)
    val navigationSelected = Color(0xFF35445F)
}

internal fun ColorScheme.isDarkCalTrackTheme(): Boolean = background.luminance() < 0.5f
