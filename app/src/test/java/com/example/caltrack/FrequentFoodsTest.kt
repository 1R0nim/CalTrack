package com.example.caltrack.data

import com.example.caltrack.data.local.dao.FoodUsageAggregate
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import com.example.caltrack.data.local.entity.SavedFood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FrequentFoodsTest {

    @Test
    fun mergesCaseAndWhitespaceVariantsUsingActualOccurrenceCounts() {
        val results = buildFrequentFoods(
            usage = listOf(
                usage(name = "OATS", count = 2, lastId = 2, lastLoggedAt = 200, grams = 100.0),
                usage(name = " oats ", count = 3, lastId = 8, lastLoggedAt = 400, grams = 180.0),
            ),
            savedFoods = listOf(savedFood(id = 12, name = "Oats")),
            fallbackFoods = emptyList(),
            limit = 8,
        )

        assertEquals(1, results.size)
        assertEquals(5, results.single().occurrenceCount)
        assertEquals(12L, results.single().food.id)
        assertEquals(180.0, results.single().food.defaultAmount, 0.0)
    }

    @Test
    fun breaksFrequencyTiesByRecentUseThenNormalizedName() {
        val results = buildFrequentFoods(
            usage = listOf(
                usage("zucchini", 2, 3, 500, 100.0),
                usage("Beta", 2, 2, 500, 100.0),
                usage("alpha", 2, 1, 500, 100.0),
                usage("Older but more frequent", 3, 4, 100, 100.0),
            ),
            savedFoods = emptyList(),
            fallbackFoods = emptyList(),
            limit = 8,
        )

        assertEquals(
            listOf("Older but more frequent", "alpha", "Beta", "zucchini"),
            results.map { it.food.name },
        )
    }

    @Test
    fun recreatesDeletedSavedFoodFromLatestLoggedSnapshot() {
        val result = buildFrequentFoods(
            usage = listOf(
                usage(
                    name = "Removed food",
                    count = 4,
                    lastId = 9,
                    lastLoggedAt = 900,
                    grams = 125.0,
                    kcal = 250.0,
                    protein = 20.0,
                    carbs = 30.0,
                    fat = 5.0,
                ),
            ),
            savedFoods = emptyList(),
            fallbackFoods = emptyList(),
            limit = 8,
        ).single()

        assertEquals(4, result.occurrenceCount)
        assertEquals(0L, result.food.id)
        assertEquals(125.0, result.food.defaultAmount, 0.0)
        assertEquals(200.0, result.food.kcalPer100, 0.0)
        assertEquals(16.0, result.food.proteinPer100, 0.0)
        assertEquals(24.0, result.food.carbsPer100, 0.0)
        assertEquals(4.0, result.food.fatPer100, 0.0)
    }

    @Test
    fun fallsBackToRecentSavedFoodsOnlyWhenJournalHasNoSelectableHistory() {
        val fallback = savedFood(id = 4, name = "Saved fallback")
        val results = buildFrequentFoods(
            usage = emptyList(),
            savedFoods = listOf(fallback),
            fallbackFoods = listOf(fallback),
            limit = 8,
        )

        assertEquals(1, results.size)
        assertEquals(fallback, results.single().food)
        assertNull(results.single().occurrenceCount)
    }

    @Test
    fun searchTrimsWhitespaceAndIgnoresCaseAndDiacritics() {
        val foods = listOf(
            savedFood(1, "Brânză telemea"),
            savedFood(2, "Măr"),
        )

        assertEquals(
            listOf("Brânză telemea"),
            filterSavedFoodsForSearch(foods, "  BRANZA  ").map { it.name },
        )
        assertEquals(foods, filterSavedFoodsForSearch(foods, "   "))
    }

    private fun usage(
        name: String,
        count: Int,
        lastId: Long,
        lastLoggedAt: Long,
        grams: Double,
        kcal: Double = 200.0,
        protein: Double = 10.0,
        carbs: Double = 20.0,
        fat: Double = 5.0,
    ) = FoodUsageAggregate(
        name = name,
        occurrenceCount = count,
        lastEntryId = lastId,
        lastLoggedAt = lastLoggedAt,
        lastAmount = grams,
        lastQuantityUnit = FoodQuantityUnit.GRAMS,
        lastKcal = kcal,
        lastProtein = protein,
        lastCarbs = carbs,
        lastFat = fat,
        lastDate = LocalDate.of(2026, 11, 1),
    )

    private fun savedFood(id: Long, name: String) = SavedFood(
        id = id,
        name = name,
        kcalPer100 = 200.0,
        proteinPer100 = 10.0,
        carbsPer100 = 20.0,
        fatPer100 = 5.0,
        defaultAmount = 100.0,
        lastUsedAt = id * 100,
    )
}
