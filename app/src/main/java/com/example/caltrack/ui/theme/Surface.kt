package com.example.caltrack.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** A quiet outline that keeps tonal surfaces distinct in both themes. */
@Composable
fun calTrackSurfaceBorder(): BorderStroke {
    val colors = MaterialTheme.colorScheme
    val alpha = if (colors.background.luminance() < 0.5f) 0.72f else 0.48f
    return BorderStroke(1.dp, colors.outlineVariant.copy(alpha = alpha))
}

/** Adds the same outline to non-Card containers such as text fields and modal sheets. */
@Composable
fun Modifier.calTrackSurfaceOutline(shape: Shape): Modifier {
    val border = calTrackSurfaceBorder()
    return this.border(border, shape)
}
