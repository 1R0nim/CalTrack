package com.example.caltrack.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caltrack.CalTrackApp
import com.example.caltrack.data.FoodRepository
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val today: LocalDate = date,
    val entries: List<FoodEntry> = emptyList(),
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val isLoading: Boolean = true,
)

class TodayViewModel(private val repository: FoodRepository) : ViewModel() {

    // null follows the calendar, so an app left open overnight doesn't keep logging into yesterday
    private val pinnedDate = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> =
        combine(currentDate(), pinnedDate) { today, pinned -> today to (pinned ?: today) }
            .flatMapLatest { (today, date) ->
                repository.observeEntries(date).map { entries ->
                    TodayUiState(
                        date = date,
                        today = today,
                        entries = entries,
                        kcal = entries.sumOf { it.kcal },
                        protein = entries.sumOf { it.protein },
                        carbs = entries.sumOf { it.carbs },
                        fat = entries.sumOf { it.fat },
                        isLoading = false,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Days with at least one entry, for the calendar markers. */
    val loggedDays: StateFlow<Set<LocalDate>> = repository.observeLoggedDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Every saved food, for the name suggestions in the add form. */
    val savedFoods: StateFlow<List<SavedFood>> = repository.observeSavedFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Last used saved foods, for the one-tap chips in the add form. */
    val recentFoods: StateFlow<List<SavedFood>> = repository.observeRecentFoods(limit = 8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editingEntry = MutableStateFlow<FoodEntry?>(null)

    /** Entry open in the editor sheet (id 0 = new entry); null while the sheet is closed. */
    val editingEntry: StateFlow<FoodEntry?> = _editingEntry.asStateFlow()

    fun showPreviousDay() {
        pinnedDate.value = selectedDate().minusDays(1)
    }

    fun showNextDay() {
        selectDate(selectedDate().plusDays(1))
    }

    fun selectDate(date: LocalDate) {
        // Picking today unpins, so the screen keeps following the calendar
        pinnedDate.value = date.takeIf { it < LocalDate.now() }
    }

    fun startAdding() {
        _editingEntry.value = FoodEntry(
            name = "",
            grams = 0.0,
            kcal = 0.0,
            protein = 0.0,
            carbs = 0.0,
            fat = 0.0,
            date = selectedDate(),
        )
    }

    fun startEditing(entry: FoodEntry) {
        _editingEntry.value = entry
    }

    fun closeEditor() {
        _editingEntry.value = null
    }

    fun saveEntry(entry: FoodEntry) {
        closeEditor()
        viewModelScope.launch { repository.saveEntry(entry) }
    }

    /** One-tap add from the form's chips: its last portion, on the shown day. */
    fun logSavedFood(food: SavedFood) {
        closeEditor()
        val date = selectedDate()
        viewModelScope.launch { repository.logSavedFood(food, food.defaultGrams, date) }
    }

    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun restoreEntry(entry: FoodEntry) {
        viewModelScope.launch { repository.restoreEntry(entry) }
    }

    /** Day shown on Azi, where new entries go (also those added from Mese). */
    fun selectedDate(): LocalDate = pinnedDate.value ?: LocalDate.now()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalTrackApp
                TodayViewModel(app.repository)
            }
        }
    }
}

/** Today's date, emitted again right after each midnight. */
private fun currentDate(): Flow<LocalDate> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now.toLocalDate())
        delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1)
    }
}.distinctUntilChanged()
