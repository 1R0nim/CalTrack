package com.example.caltrack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Wallpaper-based colors need Android 12+. */
val DynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun CalTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && DynamicColorSupported && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && DynamicColorSupported -> dynamicLightColorScheme(context)
        darkTheme -> CalTrackDarkColorScheme
        else -> CalTrackLightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content,
    )
}
