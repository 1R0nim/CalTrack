package com.example.caltrack.ui.today

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.caltrack.R
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.ui.components.FoodAvatar
import com.example.caltrack.ui.components.SwipeToDeleteBox
import com.example.caltrack.ui.formatAmount
import com.example.caltrack.ui.formatRounded
import com.example.caltrack.ui.theme.CalTrackTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

internal fun foodEntryRowShape(index: Int, count: Int): RoundedCornerShape {
    val outerCorner = 14.dp
    val innerCorner = 4.dp
    return when {
        count <= 1 -> RoundedCornerShape(outerCorner)
        index <= 0 -> RoundedCornerShape(
            topStart = outerCorner,
            topEnd = outerCorner,
            bottomEnd = innerCorner,
            bottomStart = innerCorner,
        )
        index >= count - 1 -> RoundedCornerShape(
            topStart = innerCorner,
            topEnd = innerCorner,
            bottomEnd = outerCorner,
            bottomStart = outerCorner,
        )
        else -> RoundedCornerShape(innerCorner)
    }
}

/**
 * @param restored the row is coming back through Undo: it expands into place instead of the usual
 *   entrance, and [onRestoredShown] is called once that has started.
 */
@Composable
fun SwipeToDeleteEntry(
    entry: FoodEntry,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    restored: Boolean = false,
    onRestoredShown: () -> Unit = {},
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val deleteLabel = stringResource(R.string.delete)

    // Short staggered rise-and-fade when the row first appears
    val appear = remember { Animatable(if (restored) 1f else 0f) }
    val inPreview = LocalInspectionMode.current
    val appearSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        if (restored) {
            onRestoredShown()
            return@LaunchedEffect
        }
        delay(index.coerceAtMost(6) * 40L)
        appear.animateTo(1f, appearSpec)
    }

    SwipeToDeleteBox(
        onDelete = { currentOnDelete() },
        cornerRadius = 4.dp,
        restingShape = shape,
        expandOnEnter = restored,
        modifier = modifier.graphicsLayer {
            // Previews only render the first frame, so rows are shown fully there
            val progress = if (inPreview) 1f else appear.value
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * 32.dp.toPx()
        },
    ) {
        EntryRow(
            entry = entry,
            onClick = onClick,
            // Swiping isn't available to screen readers
            modifier = Modifier.semantics {
                customActions = listOf(CustomAccessibilityAction(deleteLabel) { currentOnDelete(); true })
            },
            shape = shape,
        )
    }
}

@Composable
private fun EntryRow(
    entry: FoodEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.isDarkCalTrackTheme()
    val secondaryTextColor = if (darkTheme) TodayPalette.secondaryText else colors.onSurfaceVariant
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) TodayPalette.foodCard else colors.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodAvatar(entry.name, expressiveTodayPalette = true)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.quantity_amount, formatAmount(entry.amount), entry.quantityUnit.code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatRounded(entry.kcal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (darkTheme) TodayPalette.carbsAccent else colors.primary,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = stringResource(R.string.kcal),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                )
            }
        }
    }
}

@Composable
internal fun FoodEntryGroupRow(
    group: FoodEntryGroup,
    expanded: Boolean,
    index: Int,
    onToggle: () -> Unit,
    onEntryClick: (FoodEntry) -> Unit,
    onDeleteEntry: (FoodEntry) -> Unit,
    restoredId: Long?,
    onRestoredShown: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
) {
    val colors = MaterialTheme.colorScheme
    val darkTheme = colors.isDarkCalTrackTheme()
    val secondaryTextColor = if (darkTheme) TodayPalette.secondaryText else colors.onSurfaceVariant
    val calorieTextColor = if (darkTheme) TodayPalette.carbsAccent else colors.primary
    val representative = group.representative
    val quantity = group.combinedQuantity()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "food-group-arrow",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (darkTheme) TodayPalette.foodCard else colors.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoodAvatar(representative.name, expressiveTodayPalette = true)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = representative.name,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.12f),
                            contentColor = colors.primary,
                        ) {
                            Text(
                                text = stringResource(R.string.food_group_multiplier, group.count),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = quantity?.let { (amount, unit) ->
                            stringResource(
                                R.string.quantity_amount,
                                formatAmount(amount),
                                unit.code,
                            )
                        } ?: stringResource(R.string.food_quantity_units_differ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRounded(group.totalKcal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = calorieTextColor,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = stringResource(R.string.kcal),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = stringResource(
                        if (expanded) R.string.collapse_food_group else R.string.expand_food_group,
                    ),
                    tint = secondaryTextColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = arrowRotation },
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(durationMillis = 180)) + fadeIn(tween(durationMillis = 140)),
            exit = shrinkVertically(tween(durationMillis = 160)) + fadeOut(tween(durationMillis = 120)),
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.entries.forEachIndexed { entryIndex, entry ->
                    SwipeToDeleteEntry(
                        entry = entry,
                        index = index + entryIndex,
                        onClick = { onEntryClick(entry) },
                        onDelete = { onDeleteEntry(entry) },
                        restored = entry.id == restoredId,
                        onRestoredShown = onRestoredShown,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyDay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_restaurant),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.empty_day_title), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_day_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "Light")
@Composable
private fun EntryRowPreview() {
    CalTrackTheme {
        Surface {
            EntryRow(
                entry = FoodEntry(
                    id = 1,
                    name = "Ovăz cu lapte și banană",
                    amount = 320.0,
                    kcal = 410.0,
                    protein = 14.0,
                    carbs = 68.0,
                    fat = 8.0,
                    date = LocalDate.now(),
                ),
                onClick = {},
            )
        }
    }
}
