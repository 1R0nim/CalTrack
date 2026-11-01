package com.example.caltrack.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * A thin neutral outline for the dedicated AMOLED theme. Light theme surfaces keep their existing
 * appearance.
 */
@Composable
fun calTrackSurfaceBorder(): BorderStroke? {
    val colors = MaterialTheme.colorScheme
    return if (colors.background.luminance() < 0.5f) {
        BorderStroke(1.dp, colors.outlineVariant)
    } else {
        null
    }
}

/** Adds the same outline to non-Card containers such as text fields and modal sheets. */
@Composable
fun Modifier.calTrackSurfaceOutline(shape: Shape): Modifier {
    val border = calTrackSurfaceBorder() ?: return this
    return this.border(border, shape)
}
