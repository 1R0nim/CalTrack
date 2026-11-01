package com.example.caltrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One logged item. Nutrition values are absolute for the eaten portion (not per 100 units),
 * so editing or deleting a [SavedFood] never changes history.
 */
@Entity(
    tableName = "food_entries",
    indices = [Index("date")],
)
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Kept in the legacy `grams` database column; interpreted according to [quantityUnit]. */
    @ColumnInfo(name = "grams") val amount: Double,
    @ColumnInfo(defaultValue = "'g'") val quantityUnit: FoodQuantityUnit = FoodQuantityUnit.GRAMS,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    /** Day the food counts towards; stored as epoch day. */
    val date: LocalDate,
    /** Epoch millis, used to order entries within a day. */
    val loggedAt: Long = System.currentTimeMillis(),
)
