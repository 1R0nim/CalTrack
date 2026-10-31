package com.example.caltrack.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.caltrack.data.local.entity.SavedFood
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SavedFoodDao {

    // Raw writes; the public ones below normalize the name first so nameKey always matches it
    @Insert
    protected abstract suspend fun insertRow(food: SavedFood): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertRowOrIgnore(food: SavedFood): Long

    @Update
    protected abstract suspend fun updateRow(food: SavedFood)

    @Delete
    abstract suspend fun delete(food: SavedFood)

    @Query("SELECT * FROM saved_foods WHERE id = :id")
    abstract suspend fun getById(id: Long): SavedFood?

    @Query("SELECT * FROM saved_foods WHERE nameKey = :nameKey LIMIT 1")
    protected abstract suspend fun findByKey(nameKey: String): SavedFood?

    /** Checked before hitting Open Food Facts after a barcode scan. */
    @Query("SELECT * FROM saved_foods WHERE barcode = :barcode ORDER BY lastUsedAt DESC LIMIT 1")
    abstract suspend fun findByBarcode(barcode: String): SavedFood?

    /** The Mese list: most used first, then most recently used. */
    @Query("SELECT * FROM saved_foods ORDER BY useCount DESC, lastUsedAt DESC")
    abstract fun observeAll(): Flow<List<SavedFood>>

    @Query("SELECT * FROM saved_foods ORDER BY lastUsedAt DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<SavedFood>>

    /** Saves edits; false when the new name already belongs to another food. */
    suspend fun update(food: SavedFood): Boolean =
        try {
            updateRow(food.normalized())
            true
        } catch (_: SQLiteConstraintException) {
            false
        }

    /** Undo for [delete]: puts the food back with its id, unless its name was taken meanwhile. */
    suspend fun restore(food: SavedFood) {
        insertRowOrIgnore(food.normalized())
    }

    /**
     * Called whenever a food is logged: inserts it, or refreshes the existing food with the
     * same name (ignoring case and surrounding spaces) and bumps its usage stats.
     */
    @Transaction
    open suspend fun recordUse(food: SavedFood) {
        val now = System.currentTimeMillis()
        val normalized = food.normalized()
        val existing = findByKey(normalized.nameKey)
        if (existing == null) {
            insertRow(normalized.copy(id = 0, useCount = 1, lastUsedAt = now))
        } else {
            updateRow(
                normalized.copy(
                    id = existing.id,
                    barcode = food.barcode ?: existing.barcode,
                    useCount = existing.useCount + 1,
                    lastUsedAt = now,
                )
            )
        }
    }
}
