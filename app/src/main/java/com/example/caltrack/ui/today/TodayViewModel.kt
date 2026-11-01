package com.example.caltrack.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caltrack.CalTrackApp
import com.example.caltrack.data.BarcodeLookupFailure
import com.example.caltrack.data.BarcodeLookupResult
import com.example.caltrack.data.BarcodeLookupUiState
import com.example.caltrack.data.BarcodeProduct
import com.example.caltrack.data.FoodRepository
import com.example.caltrack.data.FrequentFood
import com.example.caltrack.data.OpenFoodFactsClient
import com.example.caltrack.data.normalizeFoodBarcode
import com.example.caltrack.data.toBarcodeProduct
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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

class TodayViewModel(
    private val repository: FoodRepository,
    private val openFoodFactsClient: OpenFoodFactsClient,
) : ViewModel() {

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

    /** Journal-backed most frequent foods, with a saved-food fallback when history is empty. */
    val frequentFoods: StateFlow<List<FrequentFood>> = repository.observeFrequentFoods(limit = 8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editingEntry = MutableStateFlow<FoodEntry?>(null)
    private val _aiAssistantOpen = MutableStateFlow(false)
    private val _barcodeLookupState = MutableStateFlow<BarcodeLookupUiState>(BarcodeLookupUiState.Idle)
    private var barcodeLookupJob: Job? = null

    val barcodeLookupState: StateFlow<BarcodeLookupUiState> = _barcodeLookupState.asStateFlow()

    /** Entry open in the editor sheet (id 0 = new entry); null while the sheet is closed. */
    val editingEntry: StateFlow<FoodEntry?> = _editingEntry.asStateFlow()

    /** Whether the floating AI Food Assistant window is shown over Azi. */
    val aiAssistantOpen: StateFlow<Boolean> = _aiAssistantOpen.asStateFlow()

    fun openAiAssistant() {
        _aiAssistantOpen.value = true
    }

    fun closeAiAssistant() {
        _aiAssistantOpen.value = false
    }

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
        clearBarcodeLookup()
        _editingEntry.value = FoodEntry(
            name = "",
            amount = 0.0,
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
        clearBarcodeLookup()
        _editingEntry.value = null
    }

    fun clearBarcodeLookup() {
        barcodeLookupJob?.cancel()
        barcodeLookupJob = null
        _barcodeLookupState.value = BarcodeLookupUiState.Idle
    }

    fun lookupBarcode(rawBarcode: String) {
        if (_barcodeLookupState.value == BarcodeLookupUiState.Loading) return
        val barcode = normalizeFoodBarcode(rawBarcode)
        if (barcode == null) {
            _barcodeLookupState.value =
                BarcodeLookupUiState.Failed(BarcodeLookupFailure.INVALID_BARCODE)
            return
        }

        barcodeLookupJob?.cancel()
        _barcodeLookupState.value = BarcodeLookupUiState.Loading
        barcodeLookupJob = viewModelScope.launch {
            val result = try {
                repository.findSavedFoodByBarcode(barcode)
                    ?.let { BarcodeLookupResult.Found(it.toBarcodeProduct()) }
                    ?: openFoodFactsClient.lookup(barcode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BarcodeLookupResult.Failed(BarcodeLookupFailure.UNKNOWN)
            }
            _barcodeLookupState.value = when (result) {
                is BarcodeLookupResult.Found -> BarcodeLookupUiState.Found(result.product)
                is BarcodeLookupResult.NotFound -> BarcodeLookupUiState.NotFound(result.barcode)
                is BarcodeLookupResult.Failed -> BarcodeLookupUiState.Failed(result.reason)
            }
        }
    }

    fun saveEntry(entry: FoodEntry, scannedProduct: BarcodeProduct? = null) {
        closeEditor()
        viewModelScope.launch { repository.saveEntry(entry, scannedProduct) }
    }

    /** AI results are already explicitly confirmed and contain locally calculated portion totals. */
    fun saveAiEntries(entries: List<FoodEntry>) {
        viewModelScope.launch {
            entries.forEach { entry -> repository.saveEntry(entry.copy(date = selectedDate())) }
        }
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
                TodayViewModel(app.repository, app.openFoodFactsClient)
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
