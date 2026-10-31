package com.example.caltrack.data.local.dao

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
