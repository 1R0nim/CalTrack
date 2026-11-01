package com.example.caltrack.data

import com.example.caltrack.data.local.entity.FoodQuantityUnit
import com.example.caltrack.data.local.entity.SavedFood

data class BarcodeNutrition(
    /** Null means the response did not identify a safe per-100g/per-100ml basis. */
    val quantityUnit: FoodQuantityUnit?,
    val kcalPer100: Double?,
    val proteinPer100: Double?,
    val carbsPer100: Double?,
    val fatPer100: Double?,
) {
    val missingNutrients: List<String>
        get() = buildList {
            if (kcalPer100 == null) add("kcal")
            if (proteinPer100 == null) add("protein")
            if (carbsPer100 == null) add("carbohydrates")
            if (fatPer100 == null) add("fat")
        }
}

data class BarcodeProduct(
    val barcode: String,
    val name: String?,
    val brand: String?,
    val productQuantity: String?,
    val servingSize: String?,
    val servingQuantity: Double?,
    val imageUrl: String?,
    val nutrition: BarcodeNutrition,
    val dataSource: String = FoodDataSource.OPEN_FOOD_FACTS,
) {
    /** Include a brand in the saved journal name when it is not already part of the product name. */
    val editorName: String
        get() {
            val cleanName = name?.trim().orEmpty()
            val cleanBrand = brand?.substringBefore(',')?.trim().orEmpty()
            return when {
                cleanName.isEmpty() -> cleanBrand
                cleanBrand.isEmpty() || cleanName.contains(cleanBrand, ignoreCase = true) -> cleanName
                else -> "$cleanBrand $cleanName"
            }
        }

    /** A quantity unit inferred only from explicit package or serving text. */
    val quantityUnitHint: FoodQuantityUnit?
        get() = nutrition.quantityUnit ?: sequenceOf(servingSize, productQuantity)
            .filterNotNull()
            .mapNotNull { text ->
                when {
                    Regex("""\d\s*(ml|l)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                        FoodQuantityUnit.MILLILITERS
                    Regex("""\d\s*(g|kg)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                        FoodQuantityUnit.GRAMS
                    else -> null
                }
            }
            .firstOrNull()

    fun defaultAmount(unit: FoodQuantityUnit): Double {
        val fromServingText = servingSize?.let { parseQuantityForUnit(it, unit) }
        val servingUnit = nutrition.quantityUnit ?: quantityUnitHint
        val fromServingQuantity = servingQuantity?.takeIf { it > 0.0 && servingUnit == unit }
        return fromServingText?.takeIf { it > 0.0 } ?: fromServingQuantity ?: 100.0
    }
}

object FoodDataSource {
    const val MANUAL = "manual"
    const val OPEN_FOOD_FACTS = "open_food_facts"
}

sealed interface BarcodeLookupUiState {
    data object Idle : BarcodeLookupUiState
    data object Loading : BarcodeLookupUiState
    data class Found(val product: BarcodeProduct) : BarcodeLookupUiState
    data class NotFound(val barcode: String) : BarcodeLookupUiState
    data class Failed(val reason: BarcodeLookupFailure) : BarcodeLookupUiState
}

/** Convert a previously scanned product into an offline preview using the saved snapshot. */
fun SavedFood.toBarcodeProduct(): BarcodeProduct =
    BarcodeProduct(
        barcode = barcode.orEmpty(),
        name = name,
        brand = brand,
        productQuantity = productQuantity,
        servingSize = servingSize,
        servingQuantity = defaultAmount,
        imageUrl = imageUrl,
        nutrition = BarcodeNutrition(
            quantityUnit = quantityUnit,
            kcalPer100 = kcalPer100,
            proteinPer100 = proteinPer100,
            carbsPer100 = carbsPer100,
            fatPer100 = fatPer100,
        ),
        dataSource = dataSource,
    )

private fun parseQuantityForUnit(text: String, unit: FoodQuantityUnit): Double? {
    val match = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(kg|g|ml|l)""", RegexOption.IGNORE_CASE)
        .find(text) ?: return null
    val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val sourceUnit = match.groupValues[2].lowercase()
    return when (unit) {
        FoodQuantityUnit.GRAMS -> when (sourceUnit) {
            "g" -> amount
            "kg" -> amount * 1000.0
            else -> null
        }
        FoodQuantityUnit.MILLILITERS -> when (sourceUnit) {
            "ml" -> amount
            "l" -> amount * 1000.0
            else -> null
        }
    }
}
