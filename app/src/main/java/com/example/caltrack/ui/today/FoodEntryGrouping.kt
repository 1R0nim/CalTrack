package com.example.caltrack.ui.today

import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToLong

/**
 * A display-only grouping of original journal rows. Every [FoodEntry] remains independently stored
 * and is still passed to its original edit/delete callbacks.
 */
internal data class FoodEntryGroup(
    val key: FoodEntryGroupingKey,
    val entries: List<FoodEntry>,
) {
    val count: Int get() = entries.size

    /** The newest spelling is the one users most recently entered. */
    val representative: FoodEntry get() = entries.last()

    val totalKcal: Double get() = entries.sumOf { it.kcal }

    /** Null means the entries use incompatible quantity units and must not be summed. */
    fun combinedQuantity(): Pair<Double, FoodQuantityUnit>? {
        val unit = entries.firstOrNull()?.quantityUnit ?: return null
        if (entries.any { it.quantityUnit != unit }) return null
        return entries.sumOf { it.amount } to unit
    }
}

/**
 * FoodEntry has no saved-food foreign key or barcode. Use the strongest identity it contains:
 * selected day, normalized name, and nutritional density per quantity unit. The quantity unit is
 * deliberately not part of the normal signature so a same-named item logged in g and ml can still
 * show a shared occurrence count, while [FoodEntryGroup.combinedQuantity] refuses to sum them.
 */
internal data class FoodEntryGroupingKey(
    val date: LocalDate,
    val normalizedName: String,
    val perUnitNutrition: List<Long>?,
    val exactFallback: List<Long>,
    val fallbackUnit: FoodQuantityUnit?,
    val distinctEntryId: Long?,
)

private const val NutritionSignaturePrecision = 100_000.0
private val Whitespace = Regex("\\s+")
private val CombiningMarks = Regex("\\p{Mn}+")

internal fun groupFoodEntries(entries: List<FoodEntry>): List<FoodEntryGroup> {
    val grouped = linkedMapOf<FoodEntryGroupingKey, MutableList<FoodEntry>>()
    entries.forEach { entry ->
        grouped.getOrPut(entry.groupingKey()) { mutableListOf() }.add(entry)
    }
    return grouped.map { (key, groupEntries) -> FoodEntryGroup(key, groupEntries.toList()) }
}

private fun FoodEntry.groupingKey(): FoodEntryGroupingKey {
    val normalizedName = Normalizer.normalize(
        name.trim().lowercase(Locale.ROOT).replace(Whitespace, " "),
        Normalizer.Form.NFD,
    ).replace(CombiningMarks, "")

    if (normalizedName.isEmpty()) {
        return FoodEntryGroupingKey(
            date = date,
            normalizedName = normalizedName,
            perUnitNutrition = null,
            exactFallback = emptyList(),
            fallbackUnit = quantityUnit,
            distinctEntryId = id,
        )
    }

    val nutrients = listOf(kcal, protein, carbs, fat)
    if (amount.isFinite() && amount > 0.0 && nutrients.all { it.isFinite() }) {
        return FoodEntryGroupingKey(
            date = date,
            normalizedName = normalizedName,
            perUnitNutrition = nutrients.map { (it / amount * NutritionSignaturePrecision).roundToLong() },
            exactFallback = emptyList(),
            fallbackUnit = null,
            distinctEntryId = null,
        )
    }

    // An invalid/non-positive amount cannot yield a meaningful per-unit signature. Only group
    // exact duplicate data in that case, including the unit, rather than guessing.
    return FoodEntryGroupingKey(
        date = date,
        normalizedName = normalizedName,
        perUnitNutrition = null,
        exactFallback = (listOf(amount) + nutrients).map { it.toBits() },
        fallbackUnit = quantityUnit,
        distinctEntryId = null,
    )
}
