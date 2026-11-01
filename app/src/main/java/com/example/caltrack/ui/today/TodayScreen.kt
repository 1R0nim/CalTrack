package com.example.caltrack.ui.today

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caltrack.R
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.ui.theme.CalTrackTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    contentPadding: PaddingValues,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    TodayScreen(
        state = state,
        contentPadding = contentPadding,
        onPreviousDay = viewModel::showPreviousDay,
        onNextDay = viewModel::showNextDay,
        onDateClick = { showCalendar = true },
        onEntryClick = viewModel::startEditing,
        onDeleteEntry = viewModel::deleteEntry,
        onRestoreEntry = viewModel::restoreEntry,
        onShowSnackbar = onShowSnackbar,
    )

    if (showCalendar) {
        // Collected only while the calendar is open
        val loggedDays by viewModel.loggedDays.collectAsStateWithLifecycle()
        CalendarSheet(
            selected = state.date,
            today = state.today,
            loggedDays = loggedDays,
            onSelect = viewModel::selectDate,
            onDismiss = { showCalendar = false },
        )
    }

    editingEntry?.let { entry ->
        // Food suggestions are collected only while the form is open
        val frequentFoods by viewModel.frequentFoods.collectAsStateWithLifecycle()
        val savedFoods by viewModel.savedFoods.collectAsStateWithLifecycle()
        EntryEditorSheet(
            entry = entry,
            frequentFoods = frequentFoods,
            savedFoods = savedFoods,
            onSave = viewModel::saveEntry,
            onDismiss = viewModel::closeEditor,
        )
    }
}

/** [contentPadding] already includes the space the floating bar and the + button cover. */
@Composable
fun TodayScreen(
    state: TodayUiState,
    contentPadding: PaddingValues,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit,
    onEntryClick: (FoodEntry) -> Unit,
    onDeleteEntry: (FoodEntry) -> Unit,
    onRestoreEntry: (FoodEntry) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val layoutDirection = LocalLayoutDirection.current
    val motionScheme = MaterialTheme.motionScheme
    val currentOnPreviousDay by rememberUpdatedState(onPreviousDay)
    val currentOnNextDay by rememberUpdatedState(onNextDay)
    // Entry brought back by Undo, so its row expands into place instead of the usual entrance
    var restoredId by remember { mutableStateOf<Long?>(null) }

    fun deleteWithUndo(entry: FoodEntry) {
        onDeleteEntry(entry)
        scope.launch {
            val undo = onShowSnackbar(
                resources.getString(R.string.entry_deleted, entry.name),
                resources.getString(R.string.undo),
            )
            if (undo) {
                restoredId = entry.id
                onRestoreEntry(entry)
            }
        }
    }

    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)

    // A newly shown day slides in from the side it comes from (later days from the right). Only one
    // list exists: the new day starts transparent and off to the side on its first frame, then
    // settles, with the offset applied in the graphics layer.
    val listState = rememberLazyListState()
    var shownDate by rememberSaveable { mutableStateOf(state.date) }
    val dayOffset = remember(state.date) { Animatable(state.date.compareTo(shownDate).sign.toFloat()) }
    LaunchedEffect(state.date) {
        if (shownDate != state.date) {
            shownDate = state.date
            listState.scrollToItem(0)
        }
        dayOffset.animateTo(0f, motionScheme.defaultSpatialSpec())
    }

    Column(modifier.fillMaxSize()) {
        DayHeader(
            date = state.date,
            today = state.today,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onDateClick = onDateClick,
            modifier = Modifier.padding(start = start, top = contentPadding.calculateTopPadding(), end = end),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Horizontal swipes change the day; swipes that start on an entry delete it instead,
                // because the row consumes the drag first
                .pointerInput(Unit) {
                    val threshold = 72.dp.toPx()
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            when {
                                dragged > threshold -> currentOnPreviousDay()
                                dragged < -threshold -> currentOnNextDay()
                            }
                        },
                    ) { change, amount ->
                        dragged += amount
                        change.consume()
                    }
                },
        ) {
            DayContent(
                state = state,
                listState = listState,
                contentPadding = PaddingValues(
                    start = start + 16.dp,
                    top = 4.dp,
                    end = end + 16.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                onEntryClick = onEntryClick,
                onDeleteEntry = ::deleteWithUndo,
                restoredId = restoredId,
                onRestoredShown = { restoredId = null },
                modifier = Modifier.graphicsLayer {
                    val offset = dayOffset.value
                    translationX = offset * size.width * 0.3f
                    alpha = (1f - abs(offset)).coerceIn(0f, 1f)
                },
            )
        }
    }
}

@Composable
private fun DayContent(
    state: TodayUiState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onEntryClick: (FoodEntry) -> Unit,
    onDeleteEntry: (FoodEntry) -> Unit,
    restoredId: Long?,
    onRestoredShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "calories") { CaloriesCard(kcal = state.kcal) }
        item(key = "macros") { MacroRow(carbs = state.carbs, fat = state.fat, protein = state.protein) }
        item(key = "split") { MacroSplitCard(carbs = state.carbs, fat = state.fat, protein = state.protein) }
        item(key = "header") {
            Text(
                text = stringResource(R.string.foods),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp),
            )
        }
        if (state.entries.isEmpty() && !state.isLoading) {
            item(key = "empty") { EmptyDay() }
        }
        itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
            SwipeToDeleteEntry(
                entry = entry,
                index = index,
                onClick = { onEntryClick(entry) },
                onDelete = { onDeleteEntry(entry) },
                restored = entry.id == restoredId,
                onRestoredShown = onRestoredShown,
                // Rows run their own entrance animation, so only placement and removal animate here
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    placementSpec = motionScheme.defaultSpatialSpec(),
                    fadeOutSpec = motionScheme.fastEffectsSpec(),
                ),
            )
        }
    }
}

private val previewEntries = listOf(
    FoodEntry(id = 1, name = "Ovăz cu lapte și banană", grams = 320.0, kcal = 410.0, protein = 14.0, carbs = 68.0, fat = 9.0, date = LocalDate.now()),
    FoodEntry(id = 2, name = "Luca traditional", grams = 120.0, kcal = 300.0, protein = 13.0, carbs = 40.0, fat = 10.0, date = LocalDate.now()),
    FoodEntry(id = 3, name = "Piept de pui la grătar", grams = 180.0, kcal = 297.0, protein = 55.8, carbs = 0.0, fat = 6.5, date = LocalDate.now()),
    FoodEntry(id = 4, name = "Iaurt grecesc", grams = 150.5, kcal = 145.5, protein = 15.0, carbs = 6.0, fat = 7.5, date = LocalDate.now()),
)

@Composable
private fun TodayScreenPreviewContent(state: TodayUiState) {
    CalTrackTheme {
        Surface {
            TodayScreen(
                state = state,
                contentPadding = PaddingValues(0.dp),
                onPreviousDay = {},
                onNextDay = {},
                onDateClick = {},
                onEntryClick = {},
                onDeleteEntry = {},
                onRestoreEntry = {},
                onShowSnackbar = { _, _ -> false },
            )
        }
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, heightDp = 1100)
@Preview(name = "Light", heightDp = 1100)
@Composable
private fun TodayScreenPreview() {
    TodayScreenPreviewContent(
        TodayUiState(
            entries = previewEntries,
            kcal = previewEntries.sumOf { it.kcal },
            protein = previewEntries.sumOf { it.protein },
            carbs = previewEntries.sumOf { it.carbs },
            fat = previewEntries.sumOf { it.fat },
            isLoading = false,
        ),
    )
}

@Preview(name = "Empty past day", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun EmptyDayPreview() {
    TodayScreenPreviewContent(
        TodayUiState(date = LocalDate.now().minusDays(3), today = LocalDate.now(), isLoading = false),
    )
}
