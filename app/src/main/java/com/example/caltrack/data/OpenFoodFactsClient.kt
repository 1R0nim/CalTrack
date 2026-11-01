package com.example.caltrack.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale

private const val KJ_PER_KCAL = 4.184
private const val OPEN_FOOD_FACTS_USER_AGENT =
    "CalTrack/1.0 (Android local-first nutrition tracker)"
private const val PRODUCT_FIELDS =
    "code,product_name,product_name_en,brands,quantity,serving_size,serving_quantity,image_front_url,nutrition,nutriments,nutrition_data_per"

/** Raw values from one Open Food Facts nutrition set before unit normalization. */
data class RawBarcodeNutrition(
    val per: String?,
    val perQuantity: Double?,
    val perUnit: String?,
    val kcal: Double?,
    val energyKj: Double?,
    val energy: Double?,
    val protein: Double?,
    val carbohydrates: Double?,
    val fat: Double?,
)

/**
 * Normalize one API nutrient set to a per-100g or per-100ml basis.
 * Values with an unknown serving basis remain unavailable instead of being guessed.
 */
fun normalizeBarcodeNutrition(raw: RawBarcodeNutrition): BarcodeNutrition {
    val per = raw.per?.trim()?.lowercase(Locale.ROOT)
    val unitAndScale: Pair<FoodQuantityUnit, Double> = when (per) {
        "100g", "100 g" -> FoodQuantityUnit.GRAMS to 1.0
        "100ml", "100 ml" -> FoodQuantityUnit.MILLILITERS to 1.0
        "serving" -> {
            val unit = unitFromCode(raw.perUnit)
            val quantity = raw.perQuantity?.takeIf { it > 0.0 && it.isFinite() }
            if (unit == null || quantity == null) null else unit to (100.0 / quantity)
        }
        else -> null
    } ?: return BarcodeNutrition(
        quantityUnit = null,
        kcalPer100 = null,
        proteinPer100 = null,
        carbsPer100 = null,
        fatPer100 = null,
    )

    val (unit, factor) = unitAndScale
    val kcal = raw.kcal ?: raw.energyKj?.div(KJ_PER_KCAL) ?: raw.energy?.div(KJ_PER_KCAL)
    return BarcodeNutrition(
        quantityUnit = unit,
        kcalPer100 = kcal?.scaled(factor),
        proteinPer100 = raw.protein?.scaled(factor),
        carbsPer100 = raw.carbohydrates?.scaled(factor),
        fatPer100 = raw.fat?.scaled(factor),
    )
}

private fun Double.scaled(factor: Double): Double? =
    (this * factor).takeIf { it.isFinite() && it >= 0.0 }

private fun unitFromCode(code: String?): FoodQuantityUnit? =
    when (code?.trim()?.lowercase(Locale.ROOT)) {
        "g", "gram", "grams" -> FoodQuantityUnit.GRAMS
        "ml", "milliliter", "milliliters" -> FoodQuantityUnit.MILLILITERS
        else -> null
    }

/** Accept scanner output for EAN-8, EAN-13, UPC-A and UPC-E (UPC-E and EAN-8 are both 8 digits). */
fun normalizeFoodBarcode(raw: String): String? {
    val barcode = raw.trim()
    return barcode.takeIf { it.all(Char::isDigit) && it.length in setOf(8, 12, 13) }
}

sealed interface BarcodeLookupResult {
    data class Found(val product: BarcodeProduct) : BarcodeLookupResult
    data class NotFound(val barcode: String) : BarcodeLookupResult
    data class Failed(val reason: BarcodeLookupFailure) : BarcodeLookupResult
}

enum class BarcodeLookupFailure {
    INVALID_BARCODE,
    NETWORK_UNAVAILABLE,
    TIMEOUT,
    RATE_LIMITED,
    SERVER_UNAVAILABLE,
    UNKNOWN,
}

/** One-request, no-retry Open Food Facts v3 client. */
class OpenFoodFactsClient(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 8_000,
) {
    suspend fun lookup(rawBarcode: String): BarcodeLookupResult {
        val barcode = normalizeFoodBarcode(rawBarcode)
            ?: return BarcodeLookupResult.Failed(BarcodeLookupFailure.INVALID_BARCODE)
        return try {
            withContext(Dispatchers.IO) { getProduct(barcode) }
        } catch (_: SocketTimeoutException) {
            BarcodeLookupResult.Failed(BarcodeLookupFailure.TIMEOUT)
        } catch (_: UnknownHostException) {
            BarcodeLookupResult.Failed(BarcodeLookupFailure.NETWORK_UNAVAILABLE)
        } catch (_: IOException) {
            BarcodeLookupResult.Failed(BarcodeLookupFailure.NETWORK_UNAVAILABLE)
        } catch (_: Exception) {
            BarcodeLookupResult.Failed(BarcodeLookupFailure.UNKNOWN)
        }
    }

    private fun getProduct(barcode: String): BarcodeLookupResult {
        val url = URL(
            "https://world.openfoodfacts.org/api/v3/product/$barcode.json?fields=$PRODUCT_FIELDS"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("User-Agent", OPEN_FOOD_FACTS_USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> BarcodeLookupResult.NotFound(barcode)
                429 -> BarcodeLookupResult.Failed(BarcodeLookupFailure.RATE_LIMITED)
                in 500..599 -> BarcodeLookupResult.Failed(BarcodeLookupFailure.SERVER_UNAVAILABLE)
                in 200..299 -> connection.inputStream.bufferedReader().use { reader ->
                    parseProductResponse(reader.readText(), barcode)
                }
                else -> BarcodeLookupResult.Failed(BarcodeLookupFailure.UNKNOWN)
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun parseProductResponse(body: String, requestedBarcode: String): BarcodeLookupResult {
    val root = JSONObject(body)
    val product = root.optJSONObject("product")
    val status = root.opt("status")?.toString()?.lowercase(Locale.ROOT)
    val failedStatus = status in setOf("0", "false", "failure", "not found")
    if (product == null || failedStatus) return BarcodeLookupResult.NotFound(requestedBarcode)

    val name = product.stringOrNull("product_name")
        ?: product.stringOrNull("product_name_en")
        ?: product.stringOrNull("generic_name")
    val brand = product.stringOrNull("brands")
    val servingSize = product.stringOrNull("serving_size")
    val productQuantity = product.stringOrNull("quantity")
    val unitFromLabels = unitFromProductText(product.stringOrNull("nutrition_data_per"))
        ?: unitFromProductText(servingSize)
        ?: unitFromProductText(productQuantity)
    val servingQuantity = product.numberOrNull("serving_quantity")

    val v3Nutrition = parseV3Nutrition(product)
    val nutrition = v3Nutrition?.takeIf { it.quantityUnit != null }
        ?: parseLegacyNutrition(product, unitFromLabels, servingQuantity)
        ?: v3Nutrition

    return BarcodeLookupResult.Found(
        BarcodeProduct(
            barcode = product.stringOrNull("code") ?: requestedBarcode,
            name = name,
            brand = brand,
            productQuantity = productQuantity,
            servingSize = servingSize,
            servingQuantity = servingQuantity,
            imageUrl = product.stringOrNull("image_front_url"),
            nutrition = nutrition ?: BarcodeNutrition(
                quantityUnit = unitFromLabels,
                kcalPer100 = null,
                proteinPer100 = null,
                carbsPer100 = null,
                fatPer100 = null,
            ),
        )
    )
}

private fun parseV3Nutrition(product: JSONObject): BarcodeNutrition? {
    val nutrition = product.optJSONObject("nutrition") ?: return null
    val aggregated = nutrition.optJSONObject("aggregated_set")
    if (aggregated != null) parseV3NutritionSet(aggregated)?.let { return it }
    val inputs = nutrition.optJSONArray("input_sets") ?: return null
    return inputs.asSequence().mapNotNull { it as? JSONObject }
        .mapNotNull(::parseV3NutritionSet)
        .firstOrNull()
}

private fun parseV3NutritionSet(set: JSONObject): BarcodeNutrition? {
    val nutrients = set.optJSONObject("nutrients") ?: return null
    return normalizeBarcodeNutrition(
        RawBarcodeNutrition(
            per = set.stringOrNull("per"),
            perQuantity = set.numberOrNull("per_quantity"),
            perUnit = set.stringOrNull("per_unit"),
            kcal = nutrients.nutrientValue("energy-kcal"),
            energyKj = nutrients.nutrientValue("energy-kj"),
            energy = nutrients.nutrientValue("energy"),
            protein = nutrients.nutrientValue("proteins"),
            carbohydrates = nutrients.nutrientValue("carbohydrates"),
            fat = nutrients.nutrientValue("fat"),
        )
    )
}

private fun parseLegacyNutrition(
    product: JSONObject,
    unitHint: FoodQuantityUnit?,
    servingQuantity: Double?,
): BarcodeNutrition? {
    val nutrients = product.optJSONObject("nutriments") ?: return null
    fun hasBasis(suffix: String) =
        listOf("energy-kcal", "energy-kj", "energy", "proteins", "carbohydrates", "fat")
            .any { nutrients.has(it + suffix) && !nutrients.isNull(it + suffix) }

    val hasGrams = hasBasis("_100g")
    val hasMilliliters = hasBasis("_100ml")
    val useMilliliters = hasMilliliters &&
        (!hasGrams || unitHint == FoodQuantityUnit.MILLILITERS)
    val explicitUnit = when {
        useMilliliters -> FoodQuantityUnit.MILLILITERS
        hasGrams -> FoodQuantityUnit.GRAMS
        else -> null
    }

    if (explicitUnit != null) {
        val suffix = if (explicitUnit == FoodQuantityUnit.MILLILITERS) "_100ml" else "_100g"
        return BarcodeNutrition(
            quantityUnit = explicitUnit,
            kcalPer100 = nutrients.numberOrNull("energy-kcal$suffix")
                ?: nutrients.numberOrNull("energy-kj$suffix")?.div(KJ_PER_KCAL)
                ?: nutrients.numberOrNull("energy$suffix")?.div(KJ_PER_KCAL),
            proteinPer100 = nutrients.numberOrNull("proteins$suffix"),
            carbsPer100 = nutrients.numberOrNull("carbohydrates$suffix"),
            fatPer100 = nutrients.numberOrNull("fat$suffix"),
        )
    }

    val quantity = servingQuantity?.takeIf { it > 0.0 } ?: return null
    val unit = unitHint ?: return null
    return normalizeBarcodeNutrition(
        RawBarcodeNutrition(
            per = "serving",
            perQuantity = quantity,
            perUnit = unit.code,
            kcal = nutrients.numberOrNull("energy-kcal_serving"),
            energyKj = nutrients.numberOrNull("energy-kj_serving"),
            energy = nutrients.numberOrNull("energy_serving"),
            protein = nutrients.numberOrNull("proteins_serving"),
            carbohydrates = nutrients.numberOrNull("carbohydrates_serving"),
            fat = nutrients.numberOrNull("fat_serving"),
        )
    )
}

private fun JSONArray.asSequence(): Sequence<Any?> = sequence {
    for (index in 0 until length()) yield(opt(index))
}

private fun JSONObject.stringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return opt(name)?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
}

private fun JSONObject.numberOrNull(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    val value = when (val raw = opt(name)) {
        is Number -> raw.toDouble()
        is String -> raw.trim().replace(',', '.').toDoubleOrNull()
        else -> null
    }
    return value?.takeIf { it.isFinite() && it >= 0.0 }
}

private fun JSONObject.nutrientValue(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return when (val raw = opt(name)) {
        is JSONObject -> raw.numberOrNull("value") ?: raw.numberOrNull("value_computed")
        is Number -> raw.toDouble().takeIf { it.isFinite() && it >= 0.0 }
        else -> null
    }
}

private fun unitFromProductText(text: String?): FoodQuantityUnit? {
    val lower = text?.lowercase(Locale.ROOT).orEmpty()
    return when {
        Regex("""\d\s*ml\b|\d\s*l\b""").containsMatchIn(lower) -> FoodQuantityUnit.MILLILITERS
        Regex("""\d\s*g\b|\d\s*kg\b""").containsMatchIn(lower) -> FoodQuantityUnit.GRAMS
        else -> null
    }
}
