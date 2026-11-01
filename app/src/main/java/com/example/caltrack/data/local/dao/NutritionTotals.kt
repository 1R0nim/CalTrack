package com.example.caltrack.data.local.dao

import com.example.caltrack.data.local.entity.FoodQuantityUnit
import java.time.LocalDate

/** Sum of all entries logged on [date]. */
data class DailyTotals(
    val date: LocalDate,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val entryCount: Int,
)

/** One exact-name group from the journal frequency query, with its latest entry snapshot. */
data class FoodUsageAggregate(
    val name: String,
    val occurrenceCount: Int,
    val lastEntryId: Long,
    val lastLoggedAt: Long,
    val lastAmount: Double,
    val lastQuantityUnit: FoodQuantityUnit,
    val lastKcal: Double,
    val lastProtein: Double,
    val lastCarbs: Double,
    val lastFat: Double,
    val lastDate: LocalDate,
)

/**
 * Sums over a date range. Averages divide by days that actually have entries,
 * so days where nothing was logged don't drag the average down.
 */
data class PeriodStats(
    val totalKcal: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val loggedDays: Int,
) {
    val avgKcal: Double get() = perDay(totalKcal)
    val avgProtein: Double get() = perDay(totalProtein)
    val avgCarbs: Double get() = perDay(totalCarbs)
    val avgFat: Double get() = perDay(totalFat)

    private fun perDay(total: Double) = if (loggedDays == 0) 0.0 else total / loggedDays
}
