package com.example.caltrack

import com.example.caltrack.data.BarcodeNutrition
import com.example.caltrack.data.BarcodeProduct
import com.example.caltrack.data.RawBarcodeNutrition
import com.example.caltrack.data.normalizeBarcodeNutrition
import com.example.caltrack.data.normalizeFoodBarcode
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenFoodFactsClientTest {

    @Test
    fun acceptsSupportedEanAndUpcLengthsAndRejectsInvalidInput() {
        assertEquals("4006381333931", normalizeFoodBarcode("4006381333931"))
        assertEquals("96385074", normalizeFoodBarcode("96385074"))
        assertEquals("036000291452", normalizeFoodBarcode("036000291452"))
        assertEquals("01234565", normalizeFoodBarcode("01234565"))
        assertNull(normalizeFoodBarcode("12345"))
        assertNull(normalizeFoodBarcode("12 345678"))
        assertNull(normalizeFoodBarcode("1234567a"))
    }

    @Test
    fun convertsKilojoulesToKilocaloriesWithoutChangingOtherNutrients() {
        val result = normalizeBarcodeNutrition(
            RawBarcodeNutrition(
                per = "100g",
                perQuantity = null,
                perUnit = null,
                kcal = null,
                energyKj = 418.4,
                energy = null,
                protein = 4.5,
                carbohydrates = 10.0,
                fat = 2.0,
            )
        )

        assertEquals(FoodQuantityUnit.GRAMS, result.quantityUnit)
        assertEquals(100.0, result.kcalPer100!!, 0.0001)
        assertEquals(4.5, result.proteinPer100!!, 0.0)
        assertEquals(10.0, result.carbsPer100!!, 0.0)
        assertEquals(2.0, result.fatPer100!!, 0.0)
    }

    @Test
    fun convertsKnownServingValuesToPer100Grams() {
        val result = normalizeBarcodeNutrition(
            RawBarcodeNutrition(
                per = "serving",
                perQuantity = 250.0,
                perUnit = "g",
                kcal = 500.0,
                energyKj = null,
                energy = null,
                protein = 30.0,
                carbohydrates = 50.0,
                fat = 10.0,
            )
        )

        assertEquals(FoodQuantityUnit.GRAMS, result.quantityUnit)
        assertEquals(200.0, result.kcalPer100!!, 0.0)
        assertEquals(12.0, result.proteinPer100!!, 0.0)
        assertEquals(20.0, result.carbsPer100!!, 0.0)
        assertEquals(4.0, result.fatPer100!!, 0.0)
    }

    @Test
    fun preservesMilliliterBasisAndDoesNotConvertItToGrams() {
        val result = normalizeBarcodeNutrition(
            RawBarcodeNutrition(
                per = "100ml",
                perQuantity = null,
                perUnit = "ml",
                kcal = 42.0,
                energyKj = null,
                energy = null,
                protein = 1.2,
                carbohydrates = 5.0,
                fat = 1.5,
            )
        )

        assertEquals(FoodQuantityUnit.MILLILITERS, result.quantityUnit)
        assertEquals(42.0, result.kcalPer100!!, 0.0)
    }

    @Test
    fun keepsMissingValuesUnavailableAndDoesNotGuessServingUnit() {
        val noValues = normalizeBarcodeNutrition(
            RawBarcodeNutrition(
                per = "100g",
                perQuantity = null,
                perUnit = null,
                kcal = null,
                energyKj = null,
                energy = null,
                protein = null,
                carbohydrates = null,
                fat = null,
            )
        )
        val unknownServing = normalizeBarcodeNutrition(
            RawBarcodeNutrition(
                per = "serving",
                perQuantity = 250.0,
                perUnit = null,
                kcal = 100.0,
                energyKj = null,
                energy = null,
                protein = null,
                carbohydrates = null,
                fat = null,
            )
        )

        assertEquals(FoodQuantityUnit.GRAMS, noValues.quantityUnit)
        assertNull(noValues.kcalPer100)
        assertNull(noValues.proteinPer100)
        assertNull(noValues.carbsPer100)
        assertNull(noValues.fatPer100)
        assertNull(unknownServing.quantityUnit)
        assertNull(unknownServing.kcalPer100)
    }

    @Test
    fun picksAUnitMatchingTheKnownServingLabel() {
        val product = BarcodeProduct(
            barcode = "12345678",
            name = "Juice",
            brand = "Example",
            productQuantity = "1 l",
            servingSize = "250 ml",
            servingQuantity = 250.0,
            imageUrl = null,
            nutrition = BarcodeNutrition(
                quantityUnit = FoodQuantityUnit.MILLILITERS,
                kcalPer100 = 45.0,
                proteinPer100 = 0.0,
                carbsPer100 = 10.0,
                fatPer100 = 0.0,
            ),
        )

        assertEquals(250.0, product.defaultAmount(FoodQuantityUnit.MILLILITERS), 0.0)
    }
}
