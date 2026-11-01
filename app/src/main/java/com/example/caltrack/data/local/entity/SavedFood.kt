package com.example.caltrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/**
 * A food remembered for one-tap re-logging. Nutrients are per 100 units of [quantityUnit].
 */
@Entity(
    tableName = "saved_foods",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["nameKey"], unique = true),
        Index("barcode"),
    ],
)
data class SavedFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // NOCASE so "Luca traditional" and "luca Traditional" are the same food
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "kcalPer100g") val kcalPer100: Double,
    @ColumnInfo(name = "proteinPer100g") val proteinPer100: Double,
    @ColumnInfo(name = "carbsPer100g") val carbsPer100: Double,
    @ColumnInfo(name = "fatPer100g") val fatPer100: Double,
    /** Last logged amount, pre-filled when re-selecting the food. */
    @ColumnInfo(name = "defaultGrams") val defaultAmount: Double,
    val barcode: String? = null,
    val useCount: Int = 0,
    val lastUsedAt: Long = System.currentTimeMillis(),
    /**
     * [name] trimmed and lowercased, unique. NOCASE alone only folds ASCII, so it would keep
     * "BRÂNZĂ" and "brânză" apart. Kept in sync by [SavedFoodDao][com.example.caltrack.data.local.dao.SavedFoodDao].
     */
    @ColumnInfo(defaultValue = "") val nameKey: String = nameKeyOf(name),
    @ColumnInfo(defaultValue = "'g'") val quantityUnit: FoodQuantityUnit = FoodQuantityUnit.GRAMS,
    @ColumnInfo(defaultValue = "'manual'") val dataSource: String = "manual",
    val brand: String? = null,
    val productQuantity: String? = null,
    val servingSize: String? = null,
    val imageUrl: String? = null,
) {
    fun toEntry(amount: Double = defaultAmount, date: LocalDate = LocalDate.now()): FoodEntry {
        val totals = FoodNutritionPer100(
            kcal = kcalPer100,
            protein = proteinPer100,
            carbs = carbsPer100,
            fat = fatPer100,
        ).forQuantity(amount)
        return FoodEntry(
            name = name,
            amount = amount,
            quantityUnit = quantityUnit,
            kcal = totals.kcal,
            protein = totals.protein,
            carbs = totals.carbs,
            fat = totals.fat,
            date = date,
        )
    }

    /** Trimmed name with a matching [nameKey]; every write goes through this. */
    fun normalized(existingNameKey: String? = null): SavedFood = copy(
            name = name.trim(),
            nameKey = existingNameKey?.takeIf { nameKeyOf(name) == nameKeyOf(this.name) } ?: nameKeyOf(name),
        )

    companion object {
        /** Derives per-100-unit values from a logged portion; null when its quantity cannot be scaled. */
        fun fromEntry(
            entry: FoodEntry,
            barcode: String? = null,
            dataSource: String = "manual",
            brand: String? = null,
            productQuantity: String? = null,
            servingSize: String? = null,
            imageUrl: String? = null,
        ): SavedFood? {
            if (entry.amount <= 0.0) return null
            val factor = 100.0 / entry.amount
            return SavedFood(
                name = entry.name,
                kcalPer100 = entry.kcal * factor,
                proteinPer100 = entry.protein * factor,
                carbsPer100 = entry.carbs * factor,
                fatPer100 = entry.fat * factor,
                defaultAmount = entry.amount,
                barcode = barcode,
                quantityUnit = entry.quantityUnit,
                dataSource = dataSource,
                brand = brand,
                productQuantity = productQuantity,
                servingSize = servingSize,
                imageUrl = imageUrl,
            )
        }

        fun nameKeyOf(name: String): String = name.trim().lowercase(Locale.ROOT)

        /** For search: also ignores diacritics, so "branza" finds "Brânză". */
        fun searchKeyOf(text: String): String =
            Normalizer.normalize(nameKeyOf(text), Normalizer.Form.NFD).replace(CombiningMarks, "")

        private val CombiningMarks = Regex("\\p{Mn}+")
    }
}
