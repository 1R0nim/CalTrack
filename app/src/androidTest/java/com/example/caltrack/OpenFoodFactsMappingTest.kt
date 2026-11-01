package com.example.caltrack

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.caltrack.data.BarcodeLookupResult
import com.example.caltrack.data.parseProductResponse
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenFoodFactsMappingTest {

    @Test
    fun mapsV3ProductMetadataAndNutritionWithoutTreatingMillilitersAsGrams() {
        val result = parseProductResponse(
            """{"status":1,"product":{"code":"12345678","product_name":"Orange juice","brands":"Example","quantity":"1 l","serving_size":"250 ml","serving_quantity":250,"image_front_url":"https://example.test/juice.jpg","nutrition":{"aggregated_set":{"per":"100ml","nutrients":{"energy-kj":{"value":418.4},"proteins":{"value":2.0},"carbohydrates":{"value":10.0},"fat":{"value":1.0}}}}}}""",
            "12345678",
        )

        val product = (result as BarcodeLookupResult.Found).product
        assertEquals("Orange juice", product.name)
        assertEquals("Example", product.brand)
        assertEquals("1 l", product.productQuantity)
        assertEquals("250 ml", product.servingSize)
        assertEquals("https://example.test/juice.jpg", product.imageUrl)
        assertEquals(FoodQuantityUnit.MILLILITERS, product.nutrition.quantityUnit)
        assertEquals(100.0, product.nutrition.kcalPer100!!, 0.0001)
        assertEquals(2.0, product.nutrition.proteinPer100!!, 0.0)
        assertEquals(250.0, product.defaultAmount(FoodQuantityUnit.MILLILITERS), 0.0)
    }

    @Test
    fun mapsLegacyPer100MilliliterValuesAndKeepsMissingNutrientsNull() {
        val result = parseProductResponse(
            """{"status":1,"product":{"code":"12345678","product_name":"Drink","nutrition_data_per":"100ml","nutriments":{"energy-kj_100ml":209.2,"carbohydrates_100ml":5.0}}}""",
            "12345678",
        )

        val product = (result as BarcodeLookupResult.Found).product
        assertEquals(FoodQuantityUnit.MILLILITERS, product.nutrition.quantityUnit)
        assertEquals(50.0, product.nutrition.kcalPer100!!, 0.0001)
        assertNull(product.nutrition.proteinPer100)
        assertEquals(5.0, product.nutrition.carbsPer100!!, 0.0)
        assertNull(product.nutrition.fatPer100)
    }
}
