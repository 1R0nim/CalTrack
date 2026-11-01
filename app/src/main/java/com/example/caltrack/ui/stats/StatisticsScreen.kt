package com.example.caltrack.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caltrack.R
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

private val ChartAxisWidth = 44.dp
private val ChartHeight = 188.dp
private val ChartLabelHeight = 22.dp

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
    StatisticsDashboard(
        state = state,
        contentPadding = contentPadding,
        onPeriodSelected = onPeriodSelected,
        modifier = modifier,
    )
}

@Composable
private fun StatisticsDashboard(
    state: StatisticsUiState,
    contentPadding: PaddingValues,
    onPeriodSelected: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.section_stats),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.stats_dashboard_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PeriodSelector(selected = state.period, onSelected = onPeriodSelected)

        val summary = state.summary
        when {
            summary == null -> StatisticsLoadingState()
            summary.totals.loggedDays == 0 -> StatisticsEmptyState()
            else -> {
                Spacer(Modifier.height(20.dp))
                AnimatedContent(
                    targetState = summary,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                            scaleIn(initialScale = 0.975f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                            .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                    },
                    label = "statistics-period-content",
                ) { animatedSummary ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            ),
                    ) {
                        AverageCaloriesCard(animatedSummary)
                        Spacer(Modifier.height(16.dp))
                        MacronutrientAveragesCard(animatedSummary)
                        Spacer(Modifier.height(16.dp))
                        DailyCaloriesChartCard(animatedSummary)
                        Spacer(Modifier.height(16.dp))
                        MacroContributionCard(animatedSummary)
                        Spacer(Modifier.height(16.dp))
                        PeriodHighlightsCard(animatedSummary)
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.stats_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatisticsEmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 24.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), MaterialShapes.Clover4Leaf.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bar_chart),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = stringResource(R.string.stats_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.stats_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: StatisticsPeriod,
    onSelected: (StatisticsPeriod) -> Unit,
) {
    val periods = StatisticsPeriod.entries
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = calTrackSurfaceBorder(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
        ) {
            val itemWidth = maxWidth / periods.size
            val selectedIndex = periods.indexOf(selected)
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "period-indicator",
            )
            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(itemWidth)
                    .height(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(24.dp),
                    ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                periods.forEachIndexed { index, period ->
                    ToggleButton(
                        checked = period == selected,
                        onCheckedChange = { onSelected(period) },
                        colors = androidx.compose.material3.ToggleButtonDefaults.colors(
                            checkedContainerColor = Color.Transparent,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            periods.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
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
    }
}

private fun periodLabel(period: StatisticsPeriod): Int = when (period) {
    StatisticsPeriod.SevenDays -> R.string.stats_period_7
    StatisticsPeriod.ThirtyDays -> R.string.stats_period_30
    StatisticsPeriod.NinetyDays -> R.string.stats_period_90
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AverageCaloriesCard(summary: StatisticsSummary) {
    val stats = summary.totals
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 18.dp, bottomEnd = 44.dp),
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 22.dp, y = (-24).dp)
                    .size(132.dp)
                    .rotate(18f)
                    .background(
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.055f),
                        MaterialShapes.Gem.toShape(),
                    ),
            )
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(
                    text = stringResource(R.string.stats_average_calories),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.stats_average_only_logged),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = formatStatNumber(stats.avgKcal),
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                        },
                        label = "average-calories",
                    ) { calories ->
                        Text(
                            text = calories,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.kcal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
    val colors = MaterialTheme.colorScheme
    val totals = summary.totals
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_macros),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.stats_average_only_logged),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroAverageCard(
                    modifier = Modifier.weight(1.08f),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 14.dp, bottomEnd = 32.dp),
                    label = stringResource(R.string.protein),
                    average = formatStatNumber(totals.avgProtein),
                    total = formatStatNumber(totals.totalProtein),
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    accentColor = colors.secondary,
                )
                MacroAverageCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 34.dp, bottomStart = 30.dp, bottomEnd = 18.dp),
                    label = stringResource(R.string.carbs),
                    average = formatStatNumber(totals.avgCarbs),
                    total = formatStatNumber(totals.totalCarbs),
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface,
                    accentColor = colors.primary,
                )
                MacroAverageCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 34.dp),
                    label = stringResource(R.string.fat),
                    average = formatStatNumber(totals.avgFat),
                    total = formatStatNumber(totals.totalFat),
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer,
                    accentColor = colors.tertiary,
                )
            }
        }
    }
}

@Composable
private fun MacroAverageCard(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    label: String,
    average: String,
    total: String,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
) {
    Card(
        modifier = modifier.heightIn(min = 146.dp),
        shape = shape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = average,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.grams_unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f),
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Text(
                text = stringResource(R.string.stats_average_per_day),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f),
                maxLines = 2,
                minLines = 2,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_total),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.72f),
            )
            Text(
                text = stringResource(R.string.amount_grams, total),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
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
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 24.dp, bottomStart = 30.dp, bottomEnd = 16.dp),
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(
                text = stringResource(R.string.stats_daily_calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatDate(summary.range.startDate) + " – " + formatDate(summary.range.endDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            if (hasEntries) {
                DailyCaloriesChart(summary)
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(ChartHeight + ChartLabelHeight),
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
    val chartMax = if (chartMaxInitial == 0.0) 4.0 else chartMaxInitial * 1.1
    val range = chartMax - chartMin
    val ticks = List(5) { index -> chartMax - (range * index / 4.0) }
    val motionSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val animatedValues = summary.dailyCalories.map { point ->
        key(point.date) {
            animateFloatAsState(
                targetValue = point.kcal?.toFloat() ?: 0f,
                animationSpec = motionSpec,
                label = "daily-calories-bar",
            ).value
        }
    }
    val firstLoggedIndex = summary.dailyCalories.indexOfLast { it.kcal != null }
    var selectedIndex by remember(summary.period, summary.dailyCalories) {
        mutableIntStateOf(firstLoggedIndex)
    }
    val selectedPoint = summary.dailyCalories[selectedIndex.coerceIn(0, summary.dailyCalories.lastIndex)]
    val animatedSelection by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chart-selection",
    )

    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .width(ChartAxisWidth)
                .height(ChartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ticks.forEach { tick ->
                Text(
                    text = formatStatNumber(tick, fractionDigits = 0),
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
            val inactiveBarColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.68f)
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
            val selectedX = maxWidth * ((animatedSelection + 0.5f) / summary.dailyCalories.size)
            val tooltipX = (selectedX - 44.dp).coerceIn(0.dp, (maxWidth - 88.dp).coerceAtLeast(0.dp))

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(summary.dailyCalories) {
                        fun selectNearestLogged(x: Float) {
                            val projectedIndex = ((x / size.width) * summary.dailyCalories.size)
                                .toInt()
                                .coerceIn(0, summary.dailyCalories.lastIndex)
                            val nearest = summary.dailyCalories.indices
                                .filter { summary.dailyCalories[it].kcal != null }
                                .minByOrNull { abs(it - projectedIndex) }
                            if (nearest != null) selectedIndex = nearest
                        }
                        detectTapGestures { tap -> selectNearestLogged(tap.x) }
                    }
                    .pointerInput(summary.dailyCalories) {
                        fun selectNearestLogged(x: Float) {
                            val projectedIndex = ((x / size.width) * summary.dailyCalories.size)
                                .toInt()
                                .coerceIn(0, summary.dailyCalories.lastIndex)
                            val nearest = summary.dailyCalories.indices
                                .filter { summary.dailyCalories[it].kcal != null }
                                .minByOrNull { abs(it - projectedIndex) }
                            if (nearest != null) selectedIndex = nearest
                        }
                        detectDragGestures(
                            onDragStart = { selectNearestLogged(it.x) },
                            onDrag = { change, _ ->
                                selectNearestLogged(change.position.x)
                                change.consume()
                            },
                        )
                    },
            ) {
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
                val barWidth = (slotWidth * 0.62f)
                    .coerceAtLeast(1.5.dp.toPx())
                    .coerceAtMost(slotWidth)
                summary.dailyCalories.forEachIndexed { index, point ->
                    if (point.kcal == null) return@forEachIndexed
                    val centerX = slotWidth * (index + 0.5f)
                    val valueY = valueToY(animatedValues[index].toDouble(), chartMin, chartMax, size.height)
                    val barHeight = abs(zeroY - valueY)
                    val color = if (index == selectedIndex) barColor else inactiveBarColor
                    if (barHeight <= 1f) {
                        drawCircle(color, maxOf(2.dp.toPx(), barWidth / 2f), Offset(centerX, zeroY))
                    } else {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(centerX - barWidth / 2f, minOf(zeroY, valueY)),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(minOf(barWidth / 2f, barHeight / 2f)),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = tooltipX, y = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = formatChartDate(selectedPoint.date, summary.period),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    AnimatedContent(
                        targetState = selectedPoint.kcal,
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                        },
                        label = "chart-value",
                    ) { kcal ->
                        Text(
                            text = stringResource(R.string.amount_kcal, formatStatNumber(kcal ?: 0.0, fractionDigits = 0)),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }
            }

            val labelIndices = chartDateLabelIndices(summary.dailyCalories.size)
            val labelWidth = maxWidth / labelIndices.size.coerceAtLeast(1)
            labelIndices.forEach { index ->
                val point = summary.dailyCalories[index]
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
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
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

private fun chartDateLabelIndices(size: Int): List<Int> {
    val targetCount = when {
        size <= 7 -> size
        size <= 30 -> 6
        else -> 5
    }
    if (size <= targetCount) return (0 until size).toList()
    return (0 until targetCount)
        .map { index -> (index * (size - 1) / (targetCount - 1).toDouble()).roundToInt() }
        .distinct()
}

private fun formatChartDate(date: LocalDate, period: StatisticsPeriod): String {
    val pattern = if (period == StatisticsPeriod.SevenDays) "EEE d" else "d MMM"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

@Composable
private fun MacroContributionCard(summary: StatisticsSummary) {
    val colorScheme = MaterialTheme.colorScheme
    val contribution = summary.macroContribution
    val shares = listOf(
        contribution.carbsShare,
        contribution.fatShare,
        contribution.proteinShare,
    ).map { share -> if (share.isFinite()) share.coerceAtLeast(0.0) else 0.0 }
    val labels = listOf(
        stringResource(R.string.carbs),
        stringResource(R.string.fat),
        stringResource(R.string.protein),
    )
    val colors = listOf(colorScheme.primary, colorScheme.tertiary, colorScheme.secondary)
    val visibleIndices = shares.indices.filter { shares[it] > 0.005 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.stats_macro_contribution),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            MacroContributionBar(shares, colors, colorScheme.surfaceContainerHighest)
            if (visibleIndices.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.stats_no_macro_contribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    visibleIndices.forEach { index ->
                        MacroLegend(
                            modifier = Modifier.weight(shares[index].toFloat()),
                            label = labels[index],
                            share = shares[index],
                            color = colors[index],
                        )
                    }
                }
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
    val animatedShares = shares.mapIndexed { index, share ->
        key(index) {
            animateFloatAsState(
                targetValue = share.toFloat(),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "macro-share",
            ).value
        }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        drawRoundRect(trackColor, cornerRadius = CornerRadius(size.height / 2f))
        val visible = animatedShares.withIndex().filter { it.value > 0.005f }
        if (visible.isEmpty()) return@Canvas
        val gap = 4.dp.toPx()
        val available = size.width - gap * (visible.size - 1)
        val total = visible.sumOf { it.value.toDouble() }
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.stats_percent, (share * 100).roundToInt()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun PeriodHighlightsCard(summary: StatisticsSummary) {
    val locale = LocalConfiguration.current.locales[0]
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = CardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_period_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HighlightMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_highest_day),
                    value = summary.highestCalorieDay?.let { formatStatNumber(it.kcal) } ?: "—",
                    date = summary.highestCalorieDay?.date?.format(DateTimeFormatter.ofPattern("d MMM", locale)).orEmpty(),
                    containerColor = colors.surfaceContainerHigh,
                    accentColor = colors.primary,
                )
                HighlightMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stats_lowest_day),
                    value = summary.lowestCalorieDay?.let { formatStatNumber(it.kcal) } ?: "—",
                    date = summary.lowestCalorieDay?.date?.format(DateTimeFormatter.ofPattern("d MMM", locale)).orEmpty(),
                    containerColor = colors.secondaryContainer,
                    accentColor = colors.secondary,
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
    containerColor: Color,
    accentColor: Color,
) {
    Card(
        modifier = modifier.heightIn(min = 124.dp),
        shape = SmallCardShape,
        border = calTrackSurfaceBorder(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.kcal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (date.isNotEmpty()) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
