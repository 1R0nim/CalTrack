package com.example.caltrack.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.caltrack.ui.theme.CalTrackTheme
import com.example.caltrack.ui.today.TodayPalette
import com.example.caltrack.ui.today.isDarkCalTrackTheme
import kotlin.math.roundToInt

val FloatingNavBarHeight = 60.dp

/** Gap between the bar and the system navigation bar. */
val FloatingNavBarBottomGap = 12.dp

/** Bottom space (on top of the system bar inset) content leaves so its last item clears the bar. */
val FloatingNavBarClearance = FloatingNavBarHeight + FloatingNavBarBottomGap + 16.dp

private val NavItemInset = 4.dp
private val IndicatorInset = 4.dp

/** Floating blue-grey navigation pill with one equal-width slot per [AppSection]. */
@Composable
fun FloatingNavBar(
    selected: AppSection,
    onSelect: (AppSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = AppSection.entries
    val haptics = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = colorScheme.isDarkCalTrackTheme()
    val containerColor = if (darkTheme) {
        TodayPalette.navigationSurface
    } else {
        colorScheme.surfaceContainerHighest.copy(alpha = 0.94f)
    }
    val selectedSurfaceColor = if (darkTheme) TodayPalette.navigationSelected else colorScheme.secondaryContainer
    val borderColor = if (darkTheme) {
        Color.White.copy(alpha = 0.06f)
    } else {
        colorScheme.outlineVariant.copy(alpha = 0.32f)
    }
    val inactiveTint = if (darkTheme) TodayPalette.secondaryText else colorScheme.onSurfaceVariant
    val selectedIconTint = if (darkTheme) TodayPalette.primaryText else colorScheme.onSecondaryContainer
    val selectedLabelTint = if (darkTheme) TodayPalette.primaryText else colorScheme.onSecondaryContainer
    val indicator = remember { Animatable(selected.ordinal.toFloat()) }
    LaunchedEffect(selected) {
        indicator.animateTo(
            targetValue = selected.ordinal.toFloat(),
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(0.74f)
            .widthIn(max = 300.dp)
            .height(FloatingNavBarHeight),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val slotWidth = maxWidth / sections.size
            Box(
                Modifier
                    .width(slotWidth)
                    .fillMaxHeight()
                    .offset { IntOffset((indicator.value * slotWidth.toPx()).roundToInt(), 0) }
                    .padding(IndicatorInset)
                    .background(selectedSurfaceColor, CircleShape),
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .selectableGroup(),
            ) {
                sections.forEach { section ->
                    NavBarItem(
                        section = section,
                        selected = section == selected,
                        iconTint = inactiveTint,
                        selectedIconTint = selectedIconTint,
                        labelTint = inactiveTint,
                        selectedLabelTint = selectedLabelTint,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(section)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    section: AppSection,
    selected: Boolean,
    iconTint: Color,
    selectedIconTint: Color,
    labelTint: Color,
    selectedLabelTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(section.label)
    // 0 = inactive look, 1 = active look; crossfades two pre-styled layers instead of recoloring
    val progress = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        progress.animateTo(if (selected) 1f else 0f, tween(durationMillis = 200))
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(NavItemInset)
            // Keep ripple feedback within this tab's pill-shaped touch target
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        NavBarItemLayer(
            icon = section.icon,
            label = label,
            iconTint = iconTint,
            labelColor = labelTint,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.graphicsLayer { alpha = 1f - progress.value },
        )
        NavBarItemLayer(
            icon = section.selectedIcon,
            label = label,
            iconTint = selectedIconTint,
            labelColor = selectedLabelTint,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.graphicsLayer { alpha = progress.value },
        )
    }
}

@Composable
private fun NavBarItemLayer(
    @DrawableRes icon: Int,
    label: String,
    iconTint: Color,
    labelColor: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(2.dp))
        // Shrinks instead of cutting off when a narrow screen or a large font leaves too little room
        BasicText(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = labelColor,
                fontWeight = fontWeight,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun FloatingNavBarPreview() {
    CalTrackTheme {
        var selected by remember { mutableStateOf(AppSection.Stats) }
        Box(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            FloatingNavBar(selected = selected, onSelect = { selected = it })
        }
    }
}
