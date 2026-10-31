package com.example.caltrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/**
 * A food remembered for one-tap re-logging. Values are per 100 g (same as Open Food Facts),
 * so any portion size can be derived from them.
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
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    /** Last logged portion, pre-filled when re-selecting the food. */
    val defaultGrams: Double,
    val barcode: String? = null,
    val useCount: Int = 0,
    val lastUsedAt: Long = System.currentTimeMillis(),
    /**
     * [name] trimmed and lowercased, unique. NOCASE alone only folds ASCII, so it would keep
     * "BRÂNZĂ" and "brânză" apart. Kept in sync by [SavedFoodDao][com.example.caltrack.data.local.dao.SavedFoodDao].
     */
    @ColumnInfo(defaultValue = "") val nameKey: String = nameKeyOf(name),
) {
    fun toEntry(grams: Double = defaultGrams, date: LocalDate = LocalDate.now()): FoodEntry {
        val factor = grams / 100.0
        return FoodEntry(
            name = name,
            grams = grams,
            kcal = kcalPer100g * factor,
            protein = proteinPer100g * factor,
            carbs = carbsPer100g * factor,
            fat = fatPer100g * factor,
            date = date,
        )
    }

    /** Trimmed name with a matching [nameKey]; every write goes through this. */
    fun normalized(): SavedFood = copy(name = name.trim(), nameKey = nameKeyOf(name))

    companion object {
        /** Derives per-100 g values from a logged portion; null when grams can't be scaled from. */
        fun fromEntry(entry: FoodEntry, barcode: String? = null): SavedFood? {
            if (entry.grams <= 0.0) return null
            val factor = 100.0 / entry.grams
            return SavedFood(
                name = entry.name,
                kcalPer100g = entry.kcal * factor,
                proteinPer100g = entry.protein * factor,
                carbsPer100g = entry.carbs * factor,
                fatPer100g = entry.fat * factor,
                defaultGrams = entry.grams,
                barcode = barcode,
            )
        }

        fun nameKeyOf(name: String): String = name.trim().lowercase(Locale.ROOT)

        /** For search: also ignores diacritics, so "branza" finds "Brânză". */
        fun searchKeyOf(text: String): String =
            Normalizer.normalize(nameKeyOf(text), Normalizer.Form.NFD).replace(CombiningMarks, "")

        private val CombiningMarks = Regex("\\p{Mn}+")
    }
}
