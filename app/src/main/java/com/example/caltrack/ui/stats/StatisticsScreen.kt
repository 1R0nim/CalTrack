package com.example.caltrack.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caltrack.R
import com.example.caltrack.ui.components.ScreenTitle
import com.example.caltrack.ui.theme.CalTrackTheme
import com.example.caltrack.ui.theme.CardShape
import com.example.caltrack.ui.theme.SmallCardShape
import com.example.caltrack.ui.theme.calTrackSurfaceBorder
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val ChartAxisWidth = 40.dp
private val ChartHeight = 184.dp
private val ChartLabelHeight = 20.dp

@Composable
fun StatisticsRoute(
    viewModel: StatisticsViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(
        state = state,
        contentPadding = contentPadding,
        onPeriodSelected = viewModel::selectPeriod,
    )
}

@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    contentPadding: PaddingValues,
    onPeriodSelected: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) {
        ScreenTitle(stringResource(R.string.section_stats))
        PeriodSelector(selected = state.period, onSelected = onPeriodSelected)

        val summary = state.summary
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.stats_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            AverageCaloriesCard(summary)
            Spacer(Modifier.height(12.dp))
            MacronutrientAveragesCard(summary)
            Spacer(Modifier.height(12.dp))
            DailyCaloriesChartCard(summary)
            Spacer(Modifier.height(12.dp))
            MacroContributionCard(summary)
            Spacer(Modifier.height(12.dp))
            PeriodHighlightsCard(summary)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PeriodSelector(
    selected: StatisticsPeriod,
    onSelected: (StatisticsPeriod) -> Unit,
) {
    val periods = StatisticsPeriod.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        periods.forEachIndexed { index, period ->
            ToggleButton(
                checked = period == selected,
                onCheckedChange = { onSelected(period) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    periods.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(periodLabel(period)),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun periodLabel(period: StatisticsPeriod): Int = when (period) {
    StatisticsPeriod.SevenDays -> R.string.stats_period_7
    StatisticsPeriod.ThirtyDays -> R.string.stats_period_30
    StatisticsPeriod.NinetyDays -> R.string.stats_period_90
}

@Composable
private fun AverageCaloriesCard(summary: StatisticsSummary) {
    val stats = summary.totals
    val hasLoggedDays = stats.loggedDays > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(
                text = stringResource(R.string.stats_average_calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (hasLoggedDays) formatStatNumber(stats.avgKcal) else "—",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = if (hasLoggedDays) stringResource(R.string.kcal) else stringResource(R.string.stats_no_entries),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_total_period),
                    value = stringResource(R.string.amount_kcal, formatStatNumber(stats.totalKcal)),
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_days_logged),
                    value = stringResource(
                        R.string.stats_logged_days_value,
                        stats.loggedDays,
                        summary.range.calendarDays,
                    ),
                    supportingText = stringResource(R.string.stats_logged_days_caption),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    supportingText: String? = null,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MacronutrientAveragesCard(summary: StatisticsSummary) {
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.background.luminance() < 0.5f
    val hasLoggedDays = summary.totals.loggedDays > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.stats_macros), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.stats_average_only_logged),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroAverageCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.carbs),
                    average = if (hasLoggedDays) formatStatNumber(summary.totals.avgCarbs) else "—",
                    total = formatStatNumber(summary.totals.totalCarbs),
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    accentColor = if (darkTheme) colorScheme.primary else colorScheme.onPrimaryContainer,
                )
                MacroAverageCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.fat),
                    average = if (hasLoggedDays) formatStatNumber(summary.totals.avgFat) else "—",
                    total = formatStatNumber(summary.totals.totalFat),
                    containerColor = colorScheme.tertiaryContainer,
                    contentColor = colorScheme.onTertiaryContainer,
                    accentColor = if (darkTheme) colorScheme.tertiary else colorScheme.onTertiaryContainer,
                )
                MacroAverageCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.protein),
                    average = if (hasLoggedDays) formatStatNumber(summary.totals.avgProtein) else "—",
                    total = formatStatNumber(summary.totals.totalProtein),
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer,
                    accentColor = if (darkTheme) colorScheme.secondary else colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MacroAverageCard(
    modifier: Modifier,
    label: String,
    average: String,
    total: String,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
) {
    Card(
        modifier = modifier,
        shape = SmallCardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = average,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            Text(stringResource(R.string.stats_average_per_day), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.stats_total), style = MaterialTheme.typography.labelSmall)
            Text(
                text = stringResource(R.string.amount_grams, total),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun DailyCaloriesChartCard(summary: StatisticsSummary) {
    val hasEntries = summary.dailyCalories.any { it.kcal != null }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.stats_daily_calories), style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatDate(summary.range.startDate) + " – " + formatDate(summary.range.endDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (hasEntries) {
                DailyCaloriesChart(summary)
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(ChartHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.stats_chart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyCaloriesChart(summary: StatisticsSummary) {
    val values = summary.dailyCalories.mapNotNull { it.kcal }
    val chartMin = minOf(0.0, values.minOrNull() ?: 0.0)
    val chartMaxInitial = maxOf(0.0, values.maxOrNull() ?: 0.0)
    val chartMax = if (chartMin == 0.0 && chartMaxInitial == 0.0) 4.0 else chartMaxInitial
    val range = chartMax - chartMin
    val ticks = List(5) { index ->
        chartMax - (range * index / 4.0)
    }
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .width(ChartAxisWidth)
                .height(ChartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ticks.forEach { tick ->
                Text(
                    text = formatStatNumber(tick, fractionDigits = 1),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(ChartHeight),
        ) {
            val barColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
            Canvas(Modifier.fillMaxSize()) {
                repeat(5) { index ->
                    val y = size.height * index / 4f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val zeroY = valueToY(0.0, chartMin, chartMax, size.height)
                val slotWidth = size.width / summary.dailyCalories.size
                val barWidth = (slotWidth * 0.58f).coerceAtMost(slotWidth)
                summary.dailyCalories.forEachIndexed { index, point ->
                    val value = point.kcal ?: return@forEachIndexed
                    val centerX = slotWidth * (index + 0.5f)
                    val valueY = valueToY(value, chartMin, chartMax, size.height)
                    val barHeight = abs(zeroY - valueY)
                    if (barHeight <= 0f) {
                        drawCircle(
                            color = barColor,
                            radius = 2.5.dp.toPx(),
                            center = Offset(centerX, zeroY),
                        )
                    } else {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(centerX - barWidth / 2f, minOf(zeroY, valueY)),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(minOf(barWidth / 2f, barHeight / 2f)),
                        )
                    }
                }
            }

            val labelIndices = chartDateLabelIndices(summary.dailyCalories.size)
            labelIndices.forEach { index ->
                val point = summary.dailyCalories[index]
                val labelWidth = maxWidth / 7f
                val center = maxWidth * ((index + 0.5f) / summary.dailyCalories.size)
                val x = when (index) {
                    0 -> 0.dp
                    summary.dailyCalories.lastIndex -> maxWidth - labelWidth
                    else -> center - labelWidth / 2
                }
                Box(
                    modifier = Modifier
                        .offset(x = x, y = ChartHeight)
                        .width(labelWidth)
                        .height(ChartLabelHeight),
                    contentAlignment = when (index) {
                        0 -> Alignment.CenterStart
                        summary.dailyCalories.lastIndex -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
                ) {
                    Text(
                        text = formatChartDate(point.date, summary.period),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(ChartLabelHeight))
}

private fun valueToY(value: Double, min: Double, max: Double, height: Float): Float {
    val normalized = ((value - min) / (max - min)).toFloat()
    return height * (1f - normalized)
}

private fun chartDateLabelIndices(size: Int): List<Int> =
    if (size <= 7) {
        (0 until size).toList()
    } else {
        (0..6).map { index -> (index * (size - 1) / 6.0).roundToInt() }.distinct()
    }

private fun formatChartDate(date: LocalDate, period: StatisticsPeriod): String {
    val pattern = if (period == StatisticsPeriod.SevenDays) "EEE d" else "d MMM"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

@Composable
private fun MacroContributionCard(summary: StatisticsSummary) {
    val colorScheme = MaterialTheme.colorScheme
    val contribution = summary.macroContribution
    val segments = listOf(contribution.carbsShare, contribution.fatShare, contribution.proteinShare)
    val colors = listOf(colorScheme.primary, colorScheme.tertiary, colorScheme.secondary)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.stats_macro_contribution), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            MacroContributionBar(segments, colors, colorScheme.surfaceContainerHighest)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroLegend(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.carbs),
                    share = contribution.carbsShare,
                    color = colors[0],
                )
                MacroLegend(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.fat),
                    share = contribution.fatShare,
                    color = colors[1],
                )
                MacroLegend(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.protein),
                    share = contribution.proteinShare,
                    color = colors[2],
                )
            }
            if (contribution.totalMacroKcal <= 0.0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.stats_no_macro_contribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MacroContributionBar(
    shares: List<Double>,
    colors: List<Color>,
    trackColor: Color,
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        drawRoundRect(trackColor, cornerRadius = CornerRadius(size.height / 2f))
        val visible = shares.withIndex().filter { it.value > 0.005 }
        if (visible.isEmpty()) return@Canvas
        val gap = 4.dp.toPx()
        val available = size.width - gap * (visible.size - 1)
        val total = visible.sumOf { it.value }
        var x = 0f
        visible.forEach { (index, share) ->
            val width = available * (share / total).toFloat()
            drawRoundRect(
                color = colors[index],
                topLeft = Offset(x, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(size.height / 2f),
            )
            x += width + gap
        }
    }
}

@Composable
private fun MacroLegend(
    modifier: Modifier,
    label: String,
    share: Double,
    color: Color,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = stringResource(R.string.stats_macro_percent, label, (share * 100).roundToInt()),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun PeriodHighlightsCard(summary: StatisticsSummary) {
    val hasLoggedDays = summary.totals.loggedDays > 0
    val locale = LocalConfiguration.current.locales[0]
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.stats_period_summary), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HighlightMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_highest_day),
                    value = summary.highestCalorieDay?.let {
                        stringResource(R.string.amount_kcal, formatStatNumber(it.kcal))
                    } ?: "—",
                    date = summary.highestCalorieDay?.date?.format(
                        DateTimeFormatter.ofPattern("d MMM", locale),
                    ).orEmpty(),
                )
                HighlightMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_lowest_day),
                    value = summary.lowestCalorieDay?.let {
                        stringResource(R.string.amount_kcal, formatStatNumber(it.kcal))
                    } ?: "—",
                    date = summary.lowestCalorieDay?.date?.format(
                        DateTimeFormatter.ofPattern("d MMM", locale),
                    ).orEmpty(),
                )
                HighlightMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_days_logged),
                    value = if (hasLoggedDays) {
                        stringResource(
                            R.string.stats_logged_days_value,
                            summary.totals.loggedDays,
                            summary.range.calendarDays,
                        )
                    } else {
                        "—"
                    },
                    date = stringResource(R.string.stats_logged_days_short),
                )
            }
        }
    }
}

@Composable
private fun HighlightMetric(
    modifier: Modifier,
    label: String,
    value: String,
    date: String,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

private fun formatStatNumber(value: Double, fractionDigits: Int = 1): String {
    if (!value.isFinite()) return "—"
    return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = fractionDigits
    }.format(value)
}

@Preview(name = "Light")
@Composable
private fun StatisticsScreenPreview() {
    CalTrackTheme {
        Surface {
            StatisticsScreen(
                state = StatisticsUiState(isLoading = true),
                contentPadding = PaddingValues(0.dp),
                onPeriodSelected = {},
            )
        }
    }
}
