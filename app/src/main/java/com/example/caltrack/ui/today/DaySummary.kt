package com.example.caltrack.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.ui.theme.CardShape
import com.example.caltrack.ui.theme.HeroCardShape
import com.example.caltrack.ui.theme.SmallCardShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaloriesCard(kcal: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Box(Modifier.fillMaxWidth()) {
            // Decorative cookie shape peeking in from the corner
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-36).dp)
                    .size(170.dp)
                    .rotate(18f)
                    .background(LocalContentColor.current.copy(alpha = 0.12f), MaterialShapes.Cookie9Sided.toShape()),
            )
            Column(Modifier.padding(horizontal = 28.dp, vertical = 26.dp)) {
                Text(stringResource(R.string.calories), style = MaterialTheme.typography.titleMedium)
                Row {
                    AnimatedNumber(
                        value = kcal,
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.kcal),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
    }
}

@Composable
fun MacroRow(carbs: Double, fat: Double, protein: Double, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MacroCard(
            label = stringResource(R.string.carbs),
            grams = carbs,
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        MacroCard(
            label = stringResource(R.string.fat),
            grams = fat,
            containerColor = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        MacroCard(
            label = stringResource(R.string.protein),
            grams = protein,
            containerColor = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MacroCard(
    label: String,
    grams: Double,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = SmallCardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row {
                AnimatedNumber(
                    value = grams,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.grams_unit),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

/** Share of calories coming from each macro (4 kcal/g carbs and protein, 9 kcal/g fat). */
@Composable
fun MacroSplitCard(carbs: Double, fat: Double, protein: Double, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val energy = listOf(carbs * 4, fat * 9, protein * 4)
    val total = energy.sum()
    val shares = energy.map { if (total > 0) (it / total).toFloat() else 0f }
    val segmentColors = listOf(colors.primary, colors.tertiary, colors.secondary)
    val labels = listOf(R.string.carbs, R.string.fat, R.string.protein)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(stringResource(R.string.calorie_split), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(14.dp))
            SegmentedBar(shares = shares, colors = segmentColors, trackColor = colors.surfaceContainerHighest)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                shares.forEachIndexed { index, share ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(segmentColors[index], CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(labels[index]) + " " +
                                stringResource(R.string.percent, (share * 100).roundToInt()),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedBar(shares: List<Float>, colors: List<Color>, trackColor: Color) {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val animated = shares.map { animateFloatAsState(it, spatial, label = "share").value }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val radius = CornerRadius(size.height / 2)
        // The spring can overshoot slightly below zero; such segments are simply not drawn
        val visible = animated.withIndex().filter { it.value > 0.005f }
        if (visible.isEmpty()) {
            drawRoundRect(trackColor, cornerRadius = radius)
            return@Canvas
        }
        val gap = 4.dp.toPx()
        val available = size.width - gap * (visible.size - 1)
        val sum = visible.sumOf { it.value.toDouble() }.toFloat()
        var x = 0f
        visible.forEach { (index, share) ->
            val width = available * share / sum
            drawRoundRect(colors[index], topLeft = Offset(x, 0f), size = Size(width, size.height), cornerRadius = radius)
            x += width + gap
        }
    }
}

/** Counts up/down to [value] instead of jumping; starts from 0 when first shown. */
@Composable
private fun AnimatedNumber(value: Double, style: TextStyle, modifier: Modifier = Modifier) {
    // Previews only render the first frame, so they start at the final value
    val initial = if (LocalInspectionMode.current) value.toFloat() else 0f
    val animated = remember { Animatable(initial) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
    Text(
        text = animated.value.roundToInt().toString(),
        // Tabular digits keep the width steady while counting
        style = style.copy(fontFeatureSettings = "tnum"),
        modifier = modifier,
    )
}
