package com.example.caltrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.caltrack.ui.today.TodayPalette
import com.example.caltrack.ui.today.isDarkCalTrackTheme

/** Initial in a playful shape; shape and color are derived from the name, so a food always looks the same. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FoodAvatar(
    name: String,
    modifier: Modifier = Modifier,
    expressiveTodayPalette: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val hash = name.trim().lowercase().hashCode()
    val shape = AvatarShapes[Math.floorMod(hash, AvatarShapes.size)].toShape()
    val colorIndex = Math.floorMod(hash / 7, 3)
    val (container, content) = if (expressiveTodayPalette && colors.isDarkCalTrackTheme()) {
        when (colorIndex) {
            0 -> TodayPalette.carbsCard to TodayPalette.carbsAccent
            1 -> TodayPalette.fatCard to TodayPalette.calorieText
            else -> TodayPalette.proteinCard to TodayPalette.proteinAccent
        }
    } else {
        when (colorIndex) {
            0 -> colors.primaryContainer to colors.onPrimaryContainer
            1 -> colors.tertiaryContainer to colors.onTertiaryContainer
            else -> colors.secondaryContainer to colors.onSecondaryContainer
        }
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .background(container, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val AvatarShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Gem,
    MaterialShapes.Puffy,
)
