package com.example.caltrack.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.ui.formatMonthYear
import com.example.caltrack.ui.narrowWeekdays
import com.example.caltrack.ui.theme.CalTrackTheme
import com.example.caltrack.ui.theme.calTrackSurfaceOutline
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Month view for jumping to any past day; days with entries get a colored circle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSheet(
    selected: LocalDate,
    today: LocalDate,
    loggedDays: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var month by rememberSaveable(stateSaver = YearMonthSaver) { mutableStateOf(YearMonth.from(selected)) }

    fun pick(date: LocalDate) {
        onSelect(date)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.calTrackSurfaceOutline(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        CalendarContent(
            month = month,
            selected = selected,
            today = today,
            loggedDays = loggedDays,
            onMonthChange = { month = it },
            onDayClick = ::pick,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun CalendarContent(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    loggedDays: Set<LocalDate>,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatMonthYear(month),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            if (selected != today) {
                TextButton(onClick = { onDayClick(today) }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.today))
                }
            }
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }, shapes = IconButtonDefaults.shapes()) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.previous_month))
            }
            IconButton(
                onClick = { onMonthChange(month.plusMonths(1)) },
                shapes = IconButtonDefaults.shapes(),
                enabled = month < YearMonth.from(today),
            ) {
                Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.next_month))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            narrowWeekdays.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(motionScheme.defaultSpatialSpec()) { it * direction } +
                    fadeIn(motionScheme.defaultEffectsSpec())) togetherWith
                    (slideOutHorizontally(motionScheme.defaultSpatialSpec()) { -it * direction } +
                        fadeOut(motionScheme.fastEffectsSpec()))
            },
            label = "month",
        ) { shownMonth ->
            MonthGrid(shownMonth, selected, today, loggedDays, onDayClick)
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    loggedDays: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit,
) {
    // Weeks start on Monday
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val weeks = (leadingBlanks + month.lengthOfMonth() + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(weeks) { week ->
            Row {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            DayCell(
                                day = day,
                                isSelected = date == selected,
                                isToday = date == today,
                                isLogged = date in loggedDays,
                                enabled = date <= today,
                                onClick = { onDayClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isLogged: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.background.luminance() < 0.5f
    val shape = if (isSelected) MaterialShapes.Cookie9Sided.toShape() else CircleShape
    val container = when {
        isSelected -> colors.primary
        isLogged -> if (darkTheme) colors.tertiary.copy(alpha = 0.16f) else colors.tertiaryContainer
        else -> Color.Transparent
    }
    val content = when {
        isSelected -> colors.onPrimary
        isLogged -> if (darkTheme) colors.tertiary else colors.onTertiaryContainer
        enabled -> colors.onSurface
        else -> colors.onSurface.copy(alpha = 0.38f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
            .clip(shape)
            .background(container)
            .then(if (isToday && !isSelected) Modifier.border(2.dp, colors.primary, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = content,
        )
    }
}

private val YearMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { YearMonth.parse(it) },
)

@Preview(showBackground = true)
@Composable
private fun CalendarContentPreview() {
    val today = LocalDate.now()
    val selected = today.minusDays(5)
    CalTrackTheme {
        CalendarContent(
            month = YearMonth.from(selected),
            selected = selected,
            today = today,
            loggedDays = (1L..9L).map { today.minusDays(it * 2) }.toSet(),
            onMonthChange = {},
            onDayClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(20.dp),
        )
    }
}
