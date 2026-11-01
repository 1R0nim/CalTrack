package com.example.caltrack.ui.today

import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FoodEntryGroupingTest {

    private val day = LocalDate.of(2026, 11, 1)

    @Test
    fun groupsNormalizedNamesWithMatchingNutritionAndCombinesCompatibleQuantities() {
        val grouped = groupFoodEntries(
            listOf(
                entry(1, "Luca Traditional", 100.0, 250.0, 20.0, 30.0, 5.0),
                entry(2, "  LUCA   Traditional ", 200.0, 500.0, 40.0, 60.0, 10.0),
            ),
        )

        assertEquals(1, grouped.size)
        val group = grouped.single()
        assertEquals(2, group.count)
        assertEquals(2L, group.representative.id)
        assertEquals(750.0, group.totalKcal, 0.0)
        assertEquals(300.0 to FoodQuantityUnit.GRAMS, group.combinedQuantity())
    }

    @Test
    fun doesNotGroupSameNameWithDifferentNutritionProfiles() {
        val grouped = groupFoodEntries(
            listOf(
                entry(1, "Greek Yogurt", 100.0, 100.0, 10.0, 5.0, 3.0),
                entry(2, "Greek Yogurt", 100.0, 180.0, 12.0, 8.0, 9.0),
            ),
        )

        assertEquals(2, grouped.size)
        assertEquals(listOf(1, 1), grouped.map { it.count })
    }

    @Test
    fun countsMixedUnitsButDoesNotCombineTheirQuantities() {
        val grouped = groupFoodEntries(
            listOf(
                entry(1, "Milk", 100.0, 60.0, 3.0, 5.0, 3.0, FoodQuantityUnit.GRAMS),
                entry(2, "Milk", 100.0, 60.0, 3.0, 5.0, 3.0, FoodQuantityUnit.MILLILITERS),
            ),
        )

        assertEquals(1, grouped.size)
        assertEquals(2, grouped.single().count)
        assertNull(grouped.single().combinedQuantity())
        assertEquals(120.0, grouped.single().totalKcal, 0.0)
    }

    @Test
    fun neverGroupsEntriesFromDifferentDays() {
        val grouped = groupFoodEntries(
            listOf(
                entry(1, "Banana", 100.0, 89.0, 1.1, 22.8, 0.3, date = day),
                entry(2, "Banana", 100.0, 89.0, 1.1, 22.8, 0.3, date = day.minusDays(1)),
            ),
        )

        assertEquals(2, grouped.size)
    }

    @Test
    fun groupedDisplayRetainsEveryOriginalEntryAndCountsCaloriesOnce() {
        val entries = listOf(
            entry(1, "Banana", 100.0, 89.0, 1.1, 22.8, 0.3),
            entry(2, "Banana", 200.0, 178.0, 2.2, 45.6, 0.6),
            entry(3, "Yogurt", 100.0, 150.0, 9.0, 12.0, 6.0),
        )
        val groups = groupFoodEntries(entries)

        assertEquals(entries.map { it.id }.sorted(), groups.flatMap { it.entries }.map { it.id }.sorted())
        assertEquals(entries.sumOf { it.kcal }, groups.sumOf { it.totalKcal }, 0.0)
    }

    private fun entry(
        id: Long,
        name: String,
        amount: Double,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        unit: FoodQuantityUnit = FoodQuantityUnit.GRAMS,
        date: LocalDate = day,
    ) = FoodEntry(
        id = id,
        name = name,
        amount = amount,
        quantityUnit = unit,
        kcal = kcal,
        protein = protein,
        carbs = carbs,
        fat = fat,
        date = date,
    )
}
