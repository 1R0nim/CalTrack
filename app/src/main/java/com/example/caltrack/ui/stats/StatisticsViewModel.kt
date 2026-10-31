package com.example.caltrack.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caltrack.CalTrackApp
import com.example.caltrack.data.FoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class StatisticsUiState(
    val period: StatisticsPeriod = StatisticsPeriod.SevenDays,
    val summary: StatisticsSummary? = null,
    val isLoading: Boolean = true,
)

class StatisticsViewModel(private val repository: FoodRepository) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(StatisticsPeriod.SevenDays)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatisticsUiState> =
        combine(currentLocalDate(), selectedPeriod) { today, period -> today to period }
            .flatMapLatest { (today, period) ->
                val range = statisticsDateRange(period, today)
                combine(
                    repository.observePeriodStats(range.startDate, range.endDate),
                    repository.observeDailyTotals(range.startDate, range.endDate),
                ) { totals, dailyTotals ->
                    StatisticsUiState(
                        period = period,
                        summary = calculateStatisticsSummary(period, today, totals, dailyTotals),
                        isLoading = false,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun selectPeriod(period: StatisticsPeriod) {
        selectedPeriod.value = period
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalTrackApp
                StatisticsViewModel(app.repository)
            }
        }
    }
}

/** Emits the current device-local date and refreshes the query after local midnight. */
private fun currentLocalDate(): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(now.zone)
        val untilNextDay = Duration.between(now.toInstant(), nextMidnight.toInstant()).toMillis()
        delay(untilNextDay.coerceAtLeast(1L))
    }
}.distinctUntilChanged()
