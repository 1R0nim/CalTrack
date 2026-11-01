package com.example.caltrack.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.ui.formatShortDate
import java.time.LocalDate

/** Arrows around a date pill (tap opens the calendar), centered. */
@Composable
fun DayHeader(
    date: LocalDate,
    today: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onPreviousDay, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.previous_day),
                )
            }
            DatePill(label = dayLabel(date, today), onClick = onDateClick)
            IconButton(
                onClick = onNextDay,
                shapes = IconButtonDefaults.shapes(),
                enabled = date < today,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.next_day),
                )
            }
        }
    }
}

@Composable
private fun DatePill(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.isDarkCalTrackTheme()
    val motionScheme = MaterialTheme.motionScheme
    val pickDay = stringResource(R.string.pick_day)
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (darkTheme) TodayPalette.distributionCard else colors.secondaryContainer,
            contentColor = if (darkTheme) colors.primary else colors.onSecondaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 128.dp)
            .semantics { onClick(label = pickDay) { onClick(); true } },
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically(motionScheme.fastSpatialSpec()) { it / 2 } + fadeIn(motionScheme.fastEffectsSpec())) togetherWith
                    (slideOutVertically(motionScheme.fastSpatialSpec()) { -it / 2 } + fadeOut(motionScheme.fastEffectsSpec()))
            },
            label = "dayLabel",
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.minusDays(1) -> stringResource(R.string.yesterday)
    else -> formatShortDate(date, withYear = date.year != today.year)
}
