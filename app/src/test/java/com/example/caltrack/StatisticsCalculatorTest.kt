package com.example.caltrack.ui.stats

import com.example.caltrack.data.local.dao.DailyTotals
import com.example.caltrack.data.local.dao.PeriodStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatisticsCalculatorTest {

    private val endDate = LocalDate.of(2026, 3, 31)

    @Test
    fun dateRangesIncludeEndDateAndRequestedCalendarDayCount() {
        val expected = listOf(
            Triple(StatisticsPeriod.SevenDays, LocalDate.of(2026, 3, 25), 7),
            Triple(StatisticsPeriod.ThirtyDays, LocalDate.of(2026, 3, 2), 30),
            Triple(StatisticsPeriod.NinetyDays, LocalDate.of(2026, 1, 1), 90),
        )

        expected.forEach { (period, start, days) ->
            val range = statisticsDateRange(period, endDate)
            assertEquals(start, range.startDate)
            assertEquals(endDate, range.endDate)
            assertEquals(days, range.calendarDays)
        }
    }

    @Test
    fun averageCaloriesUseOnlyLoggedDays() {
        val totals = PeriodStats(
            totalKcal = 10_000.0,
            totalProtein = 0.0,
            totalCarbs = 0.0,
            totalFat = 0.0,
            loggedDays = 5,
        )
        val summary = calculateStatisticsSummary(
            StatisticsPeriod.SevenDays,
            endDate,
            totals,
            dailyTotals = (0..4).map { day ->
                daily(endDate.minusDays(day.toLong()), kcal = 2_000.0)
            },
        )

        assertEquals(2_000.0, summary.totals.avgKcal, 0.0)
        assertEquals(5, summary.totals.loggedDays)
        assertEquals(7, summary.range.calendarDays)
    }

    @Test
    fun missingDaysRemainNullChartPointsRatherThanZeroCalories() {
        val totals = PeriodStats(1_000.0, 10.0, 20.0, 5.0, loggedDays = 1)
        val summary = calculateStatisticsSummary(
            StatisticsPeriod.SevenDays,
            endDate,
            totals,
            dailyTotals = listOf(daily(endDate, kcal = 1_000.0)),
        )

        assertEquals(7, summary.dailyCalories.size)
        assertNull(summary.dailyCalories.first().kcal)
        assertEquals(1_000.0, summary.dailyCalories.last().kcal!!, 0.0)
    }

    @Test
    fun macroAveragesDivideByLoggedDays() {
        val totals = PeriodStats(
            totalKcal = 10_000.0,
            totalProtein = 250.0,
            totalCarbs = 500.0,
            totalFat = 150.0,
            loggedDays = 5,
        )
        val summary = calculateStatisticsSummary(StatisticsPeriod.SevenDays, endDate, totals, emptyList())

        assertEquals(50.0, summary.totals.avgProtein, 0.0)
        assertEquals(100.0, summary.totals.avgCarbs, 0.0)
        assertEquals(30.0, summary.totals.avgFat, 0.0)
    }

    @Test
    fun macroContributionUsesCaloriesPerGramAndHandlesZeroTotal() {
        val totals = PeriodStats(
            totalKcal = 1_000.0,
            totalProtein = 50.0,
            totalCarbs = 100.0,
            totalFat = 40.0,
            loggedDays = 1,
        )
        val summary = calculateStatisticsSummary(
            StatisticsPeriod.SevenDays,
            endDate,
            totals,
            listOf(daily(endDate, kcal = 1_000.0)),
        )

        assertEquals(400.0 / 960.0, summary.macroContribution.carbsShare, 0.000001)
        assertEquals(360.0 / 960.0, summary.macroContribution.fatShare, 0.000001)
        assertEquals(200.0 / 960.0, summary.macroContribution.proteinShare, 0.000001)
        assertEquals(960.0, summary.macroContribution.totalMacroKcal, 0.0)

        val zero = calculateStatisticsSummary(
            StatisticsPeriod.SevenDays,
            endDate,
            PeriodStats(0.0, 0.0, 0.0, 0.0, loggedDays = 0),
            emptyList(),
        )
        assertEquals(0.0, zero.macroContribution.carbsShare, 0.0)
        assertEquals(0.0, zero.macroContribution.fatShare, 0.0)
        assertEquals(0.0, zero.macroContribution.proteinShare, 0.0)
        assertEquals(0.0, zero.macroContribution.totalMacroKcal, 0.0)
    }

    @Test
    fun emptyDataHasNoExtremesAndNoLoggedDayAverage() {
        val summary = calculateStatisticsSummary(
            StatisticsPeriod.SevenDays,
            endDate,
            PeriodStats(0.0, 0.0, 0.0, 0.0, loggedDays = 0),
            emptyList(),
        )

        assertEquals(0.0, summary.totals.avgKcal, 0.0)
        assertEquals(0.0, summary.totals.avgProtein, 0.0)
        assertTrue(summary.dailyCalories.all { it.kcal == null })
        assertNull(summary.highestCalorieDay)
        assertNull(summary.lowestCalorieDay)
    }

    @Test
    fun highestAndLowestUseOnlyDaysWithEntriesIncludingZeroKcalDay() {
        val totals = PeriodStats(2_000.0, 0.0, 0.0, 0.0, loggedDays = 3)
        val summary = calculateStatisticsSummary(
            StatisticsPeriod.SevenDays,
            endDate,
            totals,
            listOf(
                daily(endDate.minusDays(2), kcal = 800.0),
                daily(endDate.minusDays(1), kcal = 0.0),
                daily(endDate, kcal = 1_200.0),
            ),
        )

        assertEquals(endDate, summary.highestCalorieDay?.date)
        assertEquals(1_200.0, summary.highestCalorieDay?.kcal ?: -1.0, 0.0)
        assertEquals(endDate.minusDays(1), summary.lowestCalorieDay?.date)
        assertEquals(0.0, summary.lowestCalorieDay?.kcal ?: -1.0, 0.0)
    }

    private fun daily(date: LocalDate, kcal: Double) = DailyTotals(
        date = date,
        kcal = kcal,
        protein = 0.0,
        carbs = 0.0,
        fat = 0.0,
        entryCount = 1,
    )
}
