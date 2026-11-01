package com.example.caltrack.data

import androidx.room.withTransaction
import com.example.caltrack.data.local.CalTrackDatabase
import com.example.caltrack.data.local.dao.DailyTotals
import com.example.caltrack.data.local.dao.PeriodStats
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** A saved food logged in one tap, with what's needed to undo it. */
class LoggedFood(val entry: FoodEntry, val foodBefore: SavedFood?)

class FoodRepository(private val db: CalTrackDatabase) {

    private val entryDao = db.foodEntryDao()
    private val savedFoodDao = db.savedFoodDao()

    fun observeEntries(date: LocalDate): Flow<List<FoodEntry>> = entryDao.observeEntriesForDay(date)

    fun observeLoggedDays(): Flow<Set<LocalDate>> = entryDao.observeLoggedDays().map { it.toSet() }

    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyTotals>> =
        entryDao.observeDailyTotals(from, to)

    fun observePeriodStats(from: LocalDate, to: LocalDate): Flow<PeriodStats> =
        entryDao.observePeriodStats(from, to)

    /** Most used first, then most recently used. */
    fun observeSavedFoods(): Flow<List<SavedFood>> = savedFoodDao.observeAll()

    suspend fun findSavedFoodByBarcode(barcode: String): SavedFood? =
        savedFoodDao.findByBarcode(barcode)

    fun observeRecentFoods(limit: Int): Flow<List<SavedFood>> = savedFoodDao.observeRecent(limit)

    /** Most common foods from current journal entries, with recent saved foods as an empty-history fallback. */
    fun observeFrequentFoods(limit: Int = 8): Flow<List<FrequentFood>> =
        combine(
            entryDao.observeFoodUsage(),
            savedFoodDao.observeAll(),
            savedFoodDao.observeRecent(limit),
        ) { usage, savedFoods, recentFoods ->
            buildFrequentFoods(usage, savedFoods, recentFoods, limit)
        }

    /**
     * Inserts or updates [entry]. New entries are also remembered as a [SavedFood] for one-tap
     * re-logging. Edits only change the entry: fixing just the amount would otherwise skew the
     * food's saved per-100-unit values.
     */
    suspend fun saveEntry(entry: FoodEntry, scannedProduct: BarcodeProduct? = null) {
        if (entry.id != 0L) {
            entryDao.update(entry)
            return
        }
        db.withTransaction {
            entryDao.insert(entry)
            SavedFood.fromEntry(
                entry = entry,
                barcode = scannedProduct?.barcode,
                dataSource = scannedProduct?.dataSource ?: "manual",
                brand = scannedProduct?.brand,
                productQuantity = scannedProduct?.productQuantity,
                servingSize = scannedProduct?.servingSize,
                imageUrl = scannedProduct?.imageUrl,
            )?.let { savedFoodDao.recordUse(it) }
        }
    }

    suspend fun deleteEntry(entry: FoodEntry) = entryDao.delete(entry)

    /** Undo for [deleteEntry]: puts the entry back with its original id and timestamp. */
    suspend fun restoreEntry(entry: FoodEntry) {
        entryDao.insert(entry)
    }

    /** Logs [amount] of a saved food on [date]; the food remembers the portion and counts the use. */
    suspend fun logSavedFood(food: SavedFood, amount: Double, date: LocalDate): LoggedFood =
        db.withTransaction {
            val foodBefore = savedFoodDao.getById(food.id)
            val entry = food.toEntry(amount, date)
            val id = entryDao.insert(entry)
            savedFoodDao.recordUse(food.copy(defaultAmount = amount))
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
