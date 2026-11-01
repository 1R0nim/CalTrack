package com.example.caltrack.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fixed palette used when wallpaper colors are off (or unavailable before Android 12):
// Material Theme Builder output for the green seed #63A002.

private val SmokedSurfacePrimary = Color(0xFF080809)
private val SmokedSurfaceSecondary = Color(0xFF101014)
private val SmokedSurfaceElevated = Color(0xFF1B1B1F)
private val PrimaryText = Color(0xFFDCE5FF)

val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF4C662B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEDA3),
    onPrimaryContainer = Color(0xFF354E16),
    secondary = Color(0xFF586249),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7C8),
    onSecondaryContainer = Color(0xFF404A33),
    tertiary = Color(0xFF386663),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCECE7),
    onTertiaryContainer = Color(0xFF1F4E4B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9FAEF),
    onBackground = Color(0xFF1A1C16),
    surface = Color(0xFFF9FAEF),
    onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFE1E4D5),
    onSurfaceVariant = Color(0xFF44483D),
    outline = Color(0xFF75796C),
    outlineVariant = Color(0xFFC5C8BA),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2F312A),
    inverseOnSurface = Color(0xFFF1F2E6),
    inversePrimary = Color(0xFFB1D18A),
    surfaceDim = Color(0xFFDADBD0),
    surfaceBright = Color(0xFFF9FAEF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F4E9),
    surfaceContainer = Color(0xFFEEEFE3),
    surfaceContainerHigh = Color(0xFFE8E9DE),
    surfaceContainerHighest = Color(0xFFE2E3D8),
)

val AmoledBlackColorScheme = darkColorScheme(
    primary = Color(0xFFAFC4FF),
    onPrimary = Color(0xFF1C2B48),
    primaryContainer = SmokedSurfaceElevated,
    onPrimaryContainer = PrimaryText,
    secondary = Color(0xFFA7B5C7),
    onSecondary = Color(0xFF202834),
    secondaryContainer = SmokedSurfaceSecondary,
    onSecondaryContainer = PrimaryText,
    tertiary = Color(0xFFD8B4F8),
    onTertiary = Color(0xFF352044),
    tertiaryContainer = SmokedSurfaceElevated,
    onTertiaryContainer = PrimaryText,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = PrimaryText,
    surface = Color(0xFF000000),
    onSurface = PrimaryText,
    surfaceVariant = SmokedSurfaceSecondary,
    onSurfaceVariant = Color(0xFF92929C),
    outline = Color(0x1AFFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    scrim = Color(0xFF000000),
    inverseSurface = SmokedSurfaceElevated,
    inverseOnSurface = PrimaryText,
    inversePrimary = Color(0xFF49618F),
    surfaceDim = Color(0xFF000000),
    surfaceBright = SmokedSurfaceElevated,
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = SmokedSurfacePrimary,
    surfaceContainer = SmokedSurfaceSecondary,
    surfaceContainerHigh = SmokedSurfaceSecondary,
    surfaceContainerHighest = SmokedSurfaceElevated,
)
