package com.example.caltrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One logged item. Nutrition values are absolute for the eaten portion (not per 100 g),
 * so editing or deleting a [SavedFood] never changes history.
 */
@Entity(
    tableName = "food_entries",
    indices = [Index("date")],
)
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    /** Day the food counts towards; stored as epoch day. */
    val date: LocalDate,
    /** Epoch millis, used to order entries within a day. */
    val loggedAt: Long = System.currentTimeMillis(),
)
