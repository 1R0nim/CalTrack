package com.example.caltrack.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.material3.toShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.ui.theme.CardShape
import com.example.caltrack.ui.theme.HeroCardShape
import com.example.caltrack.ui.theme.SmallCardShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaloriesCard(kcal: Double, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.isDarkCalTrackTheme()
    val cardColor = if (darkTheme) TodayPalette.calorieCard else colors.primaryContainer
    val calorieTextColor = if (darkTheme) TodayPalette.calorieText else colors.onPrimaryContainer
    val supportingTextColor = if (darkTheme) {
        TodayPalette.calorieText.copy(alpha = 0.76f)
    } else {
        colors.onPrimaryContainer.copy(alpha = 0.78f)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = if (darkTheme) HeroCardShape else CardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = calorieTextColor,
        ),
    ) {
        Box(Modifier.fillMaxWidth()) {
            TodayCardArtwork(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-36).dp),
                size = 170.dp,
                alpha = if (darkTheme) 0.10f else 0.07f,
                shapeIndex = 0,
                rotation = 18f,
            )
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 25.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.consumed_calories),
                    style = MaterialTheme.typography.titleMedium,
                    color = supportingTextColor,


                )
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(
                        value = kcal,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = calorieTextColor,
                        ),
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.kcal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingTextColor,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
    }
}

/** Three individually filled cards keep the nutrients distinct and easy to compare. */
@Composable
fun MacroCardsRow(
    carbs: Double,
    fat: Double,
    protein: Double,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.isDarkCalTrackTheme()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 420.dp
        val gap = if (compact) 12.dp else 14.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            MacroMetric(
                label = R.string.carbs,
                grams = carbs,
                cardColor = if (darkTheme) TodayPalette.carbsCard else colors.primaryContainer,
                contentColor = if (darkTheme) TodayPalette.primaryText else colors.onPrimaryContainer,
                secondaryColor = if (darkTheme) TodayPalette.secondaryText else colors.onPrimaryContainer.copy(alpha = 0.78f),
                compact = compact,
                modifier = Modifier.weight(1f),
            )
            MacroMetric(
                label = R.string.fat,
                grams = fat,
                cardColor = if (darkTheme) TodayPalette.fatCard else colors.secondaryContainer,
                contentColor = if (darkTheme) TodayPalette.calorieText else colors.onSecondaryContainer,
                secondaryColor = if (darkTheme) {
                    TodayPalette.calorieText.copy(alpha = 0.78f)
                } else {
                    colors.onSecondaryContainer.copy(alpha = 0.78f)
                },
                compact = compact,
                modifier = Modifier.weight(1f),
            )
            MacroMetric(
                label = R.string.protein,
                grams = protein,
                cardColor = if (darkTheme) TodayPalette.proteinCard else colors.tertiaryContainer,
                contentColor = if (darkTheme) TodayPalette.primaryText else colors.onTertiaryContainer,
                secondaryColor = if (darkTheme) TodayPalette.secondaryText else colors.onTertiaryContainer.copy(alpha = 0.78f),
                compact = compact,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Share of calories from each macro: approximately 4 kcal/g for carbs and protein, 9 kcal/g for fat. */
@Composable
fun MacroDistributionCard(
    carbs: Double,
    fat: Double,
    protein: Double,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.isDarkCalTrackTheme()
    val energy = listOf(carbs * 4, fat * 9, protein * 4)
    val total = energy.sum()
    val shares = energy.map { if (total > 0) (it / total).toFloat() else 0f }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val animatedShares = shares.map { animateFloatAsState(it, spatial, label = "share").value }
    val segmentColors = listOf(colors.primary, colors.tertiary, colors.secondary)
    val labels = listOf("C", "G", "P")
    val visibleSegments = animatedShares.withIndex().filter { it.value > MinimumVisibleMacroShare }
    val primaryTextColor = if (darkTheme) TodayPalette.primaryText else colors.onSurface
    val secondaryTextColor = if (darkTheme) TodayPalette.secondaryText else colors.onSurfaceVariant

    val distributionCardColor = if (darkTheme) {
        TodayPalette.distributionCard
    } else {
        lerp(colors.surfaceContainerLow, colors.secondaryContainer, 0.34f)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = distributionCardColor),
    ) {
        Box(Modifier.fillMaxWidth()) {
            TodayCardArtwork(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-18).dp, y = 18.dp),
                size = 112.dp,
                alpha = if (darkTheme) 0.035f else 0.055f,
                shapeIndex = 4,
                rotation = 37f,
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.calorie_split),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTextColor,
                )
                SegmentedBar(
                    shares = animatedShares,
                    colors = segmentColors,
                    trackColor = if (darkTheme) TodayPalette.proteinCard else colors.surfaceContainerHighest,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (visibleSegments.isEmpty()) {
                        labels.forEachIndexed { index, label ->
                            MacroLegend(
                                label = label,
                                share = animatedShares[index],
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = secondaryTextColor,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        visibleSegments.forEach { (index, share) ->
                            MacroLegend(
                                label = labels[index],
                                share = share,
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = secondaryTextColor,
                                modifier = Modifier.weight(share),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroMetric(
    label: Int,
    grams: Double,
    cardColor: Color,
    contentColor: Color,
    secondaryColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val artworkVariant = when (label) {
        R.string.carbs -> 1
        R.string.fat -> 2
        else -> 3
    }
    Card(
        modifier = modifier.height(84.dp),
        shape = SmallCardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = contentColor,
        ),
    ) {
        Box(Modifier.fillMaxWidth()) {
            TodayCardArtwork(
                modifier = when (label) {
                    R.string.carbs -> Modifier.align(Alignment.TopStart).offset(x = (-14).dp, y = (-14).dp)
                    R.string.fat -> Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = 12.dp)
                    else -> Modifier.align(Alignment.BottomStart).offset(x = (-18).dp, y = 12.dp)
                },
                size = 72.dp,
                alpha = if (MaterialTheme.colorScheme.isDarkCalTrackTheme()) 0.06f else 0.045f,
                shapeIndex = artworkVariant,
                rotation = 12f + Math.floorMod(artworkVariant * 17, 47),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (compact) 16.dp else 18.dp,
                        end = if (compact) 16.dp else 18.dp,
                        top = 12.dp,
                        bottom = 8.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    AnimatedNumber(
                        value = grams,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.grams_unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroLegend(
    label: String,
    share: Float,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = secondaryTextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.percent, (share * 100).roundToInt()),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = primaryTextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private const val MinimumVisibleMacroShare = 0.005f

@Composable
private fun SegmentedBar(shares: List<Float>, colors: List<Color>, trackColor: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val radius = CornerRadius(size.height / 2)
        // The spring can overshoot slightly below zero; such segments are simply not drawn
        val visible = shares.withIndex().filter { it.value > MinimumVisibleMacroShare }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TodayCardArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    alpha: Float = 0.07f,
    shapeIndex: Int,
    rotation: Float = 18f,
) {
    val shape = when (Math.floorMod(shapeIndex, 6)) {
        0 -> MaterialShapes.Cookie9Sided.toShape()
        1 -> MaterialShapes.Clover4Leaf.toShape()
        2 -> MaterialShapes.Sunny.toShape()
        3 -> MaterialShapes.Cookie6Sided.toShape()
        4 -> MaterialShapes.Gem.toShape()
        else -> MaterialShapes.Puffy.toShape()
    }
    Box(
        modifier
            .size(size)
            .rotate(rotation)
            .background(LocalContentColor.current.copy(alpha = alpha), shape),
    )
}

/** Counts up/down to [value] instead of jumping; starts from 0 when first shown. */
@Composable
private fun AnimatedNumber(value: Double, style: TextStyle, modifier: Modifier = Modifier) {
    // Previews only render the first frame, so they start at the final value
    val initial = if (LocalInspectionMode.current) value.toFloat() else 0f
    val animated = remember { Animatable(initial) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(durationMillis = 360, easing = FastOutSlowInEasing))
    }
    Text(
        text = animated.value.roundToInt().toString(),
        // Tabular digits keep the width steady while counting
        style = style.copy(fontFeatureSettings = "tnum"),
        modifier = modifier,
    )
}