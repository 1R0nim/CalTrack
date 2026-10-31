package com.example.caltrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.caltrack.data.local.entity.FoodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FoodEntryDao {

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun getById(id: Long): FoodEntry?

    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY loggedAt ASC")
    fun observeEntriesForDay(date: LocalDate): Flow<List<FoodEntry>>

    /** Every day that has at least one entry; marks the calendar. Uses the index on date. */
    @Query("SELECT DISTINCT date FROM food_entries")
    fun observeLoggedDays(): Flow<List<LocalDate>>

    /** Per-day totals for the history list, newest day first. Days without entries are absent. */
    @Query(
        """
        SELECT date,
               SUM(kcal) AS kcal,
               SUM(protein) AS protein,
               SUM(carbs) AS carbs,
               SUM(fat) AS fat,
               COUNT(*) AS entryCount
        FROM food_entries
        WHERE date BETWEEN :from AND :to
        GROUP BY date
        ORDER BY date DESC
        """
    )
    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyTotals>>

    /** Aggregates for the 7/30/90-day averages; always emits exactly one row. */
    @Query(
        """
        SELECT COALESCE(SUM(kcal), 0) AS totalKcal,
               COALESCE(SUM(protein), 0) AS totalProtein,
               COALESCE(SUM(carbs), 0) AS totalCarbs,
               COALESCE(SUM(fat), 0) AS totalFat,
               COUNT(DISTINCT date) AS loggedDays
        FROM food_entries
        WHERE date BETWEEN :from AND :to
        """
    )
    fun observePeriodStats(from: LocalDate, to: LocalDate): Flow<PeriodStats>
}
