package com.example.caltrack.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodNutritionCalculationTest {

    @Test
    fun calculatesEveryNutrientIndependentlyForEnteredQuantity() {
        val totals = FoodNutritionPer100(
            kcal = 200.0,
            protein = 10.0,
            carbs = 20.0,
            fat = 5.0,
        ).forQuantity(150.0)

        assertEquals(300.0, totals.kcal, 0.0)
        assertEquals(15.0, totals.protein, 0.0)
        assertEquals(30.0, totals.carbs, 0.0)
        assertEquals(7.5, totals.fat, 0.0)
    }

    @Test
    fun preservesFractionalReferenceValuesAndQuantitiesWithoutPrematureRounding() {
        val totals = FoodNutritionPer100(
            kcal = 83.7,
            protein = 1.25,
            carbs = 17.35,
            fat = 0.333,
        ).forQuantity(12.5)

        assertEquals(10.4625, totals.kcal, 1e-12)
        assertEquals(0.15625, totals.protein, 1e-12)
        assertEquals(2.16875, totals.carbs, 1e-12)
        assertEquals(0.041625, totals.fat, 1e-12)
    }

    @Test
    fun savedFoodEntryUsesScaledTotalsAndRetainsReferenceValues() {
        val food = SavedFood(
            name = "Test food",
            kcalPer100 = 200.0,
            proteinPer100 = 10.0,
            carbsPer100 = 20.0,
            fatPer100 = 5.0,
            defaultAmount = 100.0,
        )

        val entry = food.toEntry(amount = 150.0)

        assertEquals(300.0, entry.kcal, 0.0)
        assertEquals(15.0, entry.protein, 0.0)
        assertEquals(30.0, entry.carbs, 0.0)
        assertEquals(7.5, entry.fat, 0.0)
        assertEquals(200.0, food.kcalPer100, 0.0)
        assertEquals(10.0, food.proteinPer100, 0.0)
        assertEquals(20.0, food.carbsPer100, 0.0)
        assertEquals(5.0, food.fatPer100, 0.0)
    }
}
