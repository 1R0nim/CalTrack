package com.example.caltrack.data

import androidx.room.withTransaction
import com.example.caltrack.data.local.CalTrackDatabase
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** A saved food logged in one tap, with what's needed to undo it. */
class LoggedFood(val entry: FoodEntry, val foodBefore: SavedFood?)

class FoodRepository(private val db: CalTrackDatabase) {

    private val entryDao = db.foodEntryDao()
    private val savedFoodDao = db.savedFoodDao()

    fun observeEntries(date: LocalDate): Flow<List<FoodEntry>> = entryDao.observeEntriesForDay(date)

    fun observeLoggedDays(): Flow<Set<LocalDate>> = entryDao.observeLoggedDays().map { it.toSet() }

    /** Most used first, then most recently used. */
    fun observeSavedFoods(): Flow<List<SavedFood>> = savedFoodDao.observeAll()

    fun observeRecentFoods(limit: Int): Flow<List<SavedFood>> = savedFoodDao.observeRecent(limit)

    /**
     * Inserts or updates [entry]. New entries are also remembered as a [SavedFood] for one-tap
     * re-logging. Edits only change the entry: fixing just the grams would otherwise skew the
     * food's per-100 g values.
     */
    suspend fun saveEntry(entry: FoodEntry) {
        if (entry.id != 0L) {
            entryDao.update(entry)
            return
        }
        db.withTransaction {
            entryDao.insert(entry)
            SavedFood.fromEntry(entry)?.let { savedFoodDao.recordUse(it) }
        }
    }

    suspend fun deleteEntry(entry: FoodEntry) = entryDao.delete(entry)

    /** Undo for [deleteEntry]: puts the entry back with its original id and timestamp. */
    suspend fun restoreEntry(entry: FoodEntry) {
        entryDao.insert(entry)
    }

    /** Logs [grams] of a saved food on [date]; the food remembers the portion and counts the use. */
    suspend fun logSavedFood(food: SavedFood, grams: Double, date: LocalDate): LoggedFood =
        db.withTransaction {
            val foodBefore = savedFoodDao.getById(food.id)
            val entry = food.toEntry(grams, date)
            val id = entryDao.insert(entry)
            savedFoodDao.recordUse(food.copy(defaultGrams = grams))
            LoggedFood(entry.copy(id = id), foodBefore)
        }

    /** Undo for [logSavedFood]: removes the entry and puts back the food's previous portion and stats. */
    suspend fun undoLog(log: LoggedFood) {
        db.withTransaction {
            entryDao.delete(log.entry)
            log.foodBefore?.let { savedFoodDao.update(it) }
        }
    }

    /** Removes a food from Mese only; entries already logged with it stay as they are. */
    suspend fun deleteSavedFood(food: SavedFood) = savedFoodDao.delete(food)

    suspend fun restoreSavedFood(food: SavedFood) = savedFoodDao.restore(food)

    /** False when the new name already belongs to another saved food. */
    suspend fun updateSavedFood(food: SavedFood): Boolean = savedFoodDao.update(food)
}
