package com.example.caltrack.ui.stats

import com.example.caltrack.data.local.dao.DailyTotals
import com.example.caltrack.data.local.dao.PeriodStats
import java.time.LocalDate

enum class StatisticsPeriod(val dayCount: Int) {
    SevenDays(7),
    ThirtyDays(30),
    NinetyDays(90),
}

data class StatisticsDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val calendarDays: Int,
)

fun statisticsDateRange(period: StatisticsPeriod, endDate: LocalDate): StatisticsDateRange =
    StatisticsDateRange(
        startDate = endDate.minusDays(period.dayCount - 1L),
        endDate = endDate,
        calendarDays = period.dayCount,
    )

data class DailyCaloriesPoint(
    val date: LocalDate,
    /** null means no food was logged on this calendar day; 0.0 is a recorded zero. */
    val kcal: Double?,
)

data class MacroCalorieContribution(
    /** Fractions of calories from each macro, in the range 0..1 when data is non-negative. */
    val carbsShare: Double,
    val fatShare: Double,
    val proteinShare: Double,
    val totalMacroKcal: Double,
)

data class StatisticsSummary(
    val period: StatisticsPeriod,
    val range: StatisticsDateRange,
    val totals: PeriodStats,
    val dailyCalories: List<DailyCaloriesPoint>,
    val highestCalorieDay: DailyTotals?,
    val lowestCalorieDay: DailyTotals?,
    val macroContribution: MacroCalorieContribution,
)

fun calculateStatisticsSummary(
    period: StatisticsPeriod,
    endDate: LocalDate,
    totals: PeriodStats,
    dailyTotals: List<DailyTotals>,
): StatisticsSummary {
    val range = statisticsDateRange(period, endDate)
    val loggedByDate = dailyTotals
        .asSequence()
        .filter { it.date >= range.startDate && it.date <= range.endDate }
        .associateBy { it.date }

    val loggedDays = loggedByDate.values.toList()
    val dailyCalories = List(range.calendarDays) { offset ->
        val date = range.startDate.plusDays(offset.toLong())
        DailyCaloriesPoint(date = date, kcal = loggedByDate[date]?.kcal)
    }

    val carbsKcal = totals.totalCarbs * 4.0
    val fatKcal = totals.totalFat * 9.0
    val proteinKcal = totals.totalProtein * 4.0
    val macroKcal = carbsKcal + fatKcal + proteinKcal
    val contribution = if (macroKcal > 0.0) {
        MacroCalorieContribution(
            carbsShare = carbsKcal / macroKcal,
            fatShare = fatKcal / macroKcal,
            proteinShare = proteinKcal / macroKcal,
            totalMacroKcal = macroKcal,
        )
    } else {
        MacroCalorieContribution(
            carbsShare = 0.0,
            fatShare = 0.0,
            proteinShare = 0.0,
            totalMacroKcal = macroKcal,
        )
    }

    return StatisticsSummary(
        period = period,
        range = range,
        totals = totals,
        dailyCalories = dailyCalories,
        highestCalorieDay = loggedDays.maxWithOrNull(
            compareBy<DailyTotals> { it.kcal }.thenBy { it.date },
        ),
        lowestCalorieDay = loggedDays.minWithOrNull(
            compareBy<DailyTotals> { it.kcal }.thenByDescending { it.date },
        ),
        macroContribution = contribution,
    )
}
