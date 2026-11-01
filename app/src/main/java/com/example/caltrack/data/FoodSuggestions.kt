package com.example.caltrack.data

import com.example.caltrack.data.local.dao.FoodUsageAggregate
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood

/** A selectable food ranked from journal history, or a saved-food fallback when history is empty. */
data class FrequentFood(
    val food: SavedFood,
    val occurrenceCount: Int?,
    val lastLoggedAt: Long,
)

/**
 * Merges SQLite's ASCII case-insensitive groups with Kotlin's Unicode-aware normalized food names.
 * Journal entries have no SavedFood foreign key, so deleted records are rebuilt from the latest
 * actual entry snapshot when it has a valid portion.
 */
internal fun buildFrequentFoods(
    usage: List<FoodUsageAggregate>,
    savedFoods: List<SavedFood>,
    fallbackFoods: List<SavedFood>,
    limit: Int,
): List<FrequentFood> {
    if (limit <= 0) return emptyList()

    val savedByName = savedFoods.associateBy { SavedFood.nameKeyOf(it.name) }
    val ranked = usage
        .groupBy { SavedFood.nameKeyOf(it.name) }
        .mapNotNull { (nameKey, groups) ->
            val latest = groups.maxWithOrNull(
                compareBy<FoodUsageAggregate> { it.lastLoggedAt }
                    .thenBy { it.lastEntryId }
                    .thenBy { it.name },
            ) ?: return@mapNotNull null
            val latestUsablePortion = groups
                .filter { it.lastAmount > 0.0 }
                .maxWithOrNull(
                    compareBy<FoodUsageAggregate> { it.lastLoggedAt }
                        .thenBy { it.lastEntryId }
                        .thenBy { it.name },
                )
            val savedFood = savedByName[nameKey]
            val selectableFood = when {
                savedFood != null -> savedFood.copy(
                    defaultAmount = latestUsablePortion?.lastAmount ?: savedFood.defaultAmount,
                    quantityUnit = latestUsablePortion?.lastQuantityUnit ?: savedFood.quantityUnit,
                )
                latestUsablePortion != null -> SavedFood.fromEntry(latestUsablePortion.toFoodEntry())
                else -> null
            } ?: return@mapNotNull null

            FrequentFood(
                food = selectableFood,
                occurrenceCount = groups.sumOf { it.occurrenceCount },
                lastLoggedAt = latest.lastLoggedAt,
            )
        }
        .sortedWith(frequentFoodOrder)

    if (ranked.isNotEmpty()) return ranked.take(limit)

    val fallback = fallbackFoods.ifEmpty { savedFoods }
    return fallback
        .distinctBy { SavedFood.nameKeyOf(it.name) }
        .map { FrequentFood(it, occurrenceCount = null, lastLoggedAt = it.lastUsedAt) }
        .sortedWith(frequentFoodOrder)
        .take(limit)
}

/** Saved-food name matching used by both the add-sheet search and its unit tests. */
fun filterSavedFoodsForSearch(
    foods: List<SavedFood>,
    query: String,
    initialLimit: Int = 8,
): List<SavedFood> {
    val key = SavedFood.searchKeyOf(query)
    if (key.isEmpty()) return foods.take(initialLimit.coerceAtLeast(0))
    return foods.filter { SavedFood.searchKeyOf(it.name).contains(key) }
}

private val frequentFoodOrder =
    compareByDescending<FrequentFood> { it.occurrenceCount ?: 0 }
        .thenByDescending { it.lastLoggedAt }
        .thenBy { SavedFood.nameKeyOf(it.food.name) }
        .thenBy { it.food.id }
        .thenBy { it.food.name }

private fun FoodUsageAggregate.toFoodEntry() = FoodEntry(
    id = lastEntryId,
    name = name,
    amount = lastAmount,
    quantityUnit = lastQuantityUnit,
    kcal = lastKcal,
    protein = lastProtein,
    carbs = lastCarbs,
    fat = lastFat,
    date = lastDate,
    loggedAt = lastLoggedAt,
)
