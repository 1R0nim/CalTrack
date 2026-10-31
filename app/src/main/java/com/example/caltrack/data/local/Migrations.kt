package com.example.caltrack.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.caltrack.data.local.entity.SavedFood

/**
 * Adds saved_foods.nameKey (trimmed, lowercased name) with a unique index. The key is computed in
 * Kotlin because SQLite's lower() only folds ASCII. Saved foods whose names only differed by
 * non-ASCII case or surrounding spaces become one: the most recently used one is kept, with the
 * uses of all of them. Logged entries are never touched.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `nameKey` TEXT NOT NULL DEFAULT ''")

        class Row(val id: Long, val name: String, val useCount: Int, val lastUsedAt: Long)
        val rows = buildList {
            db.query("SELECT id, name, useCount, lastUsedAt FROM saved_foods").use { cursor ->
                while (cursor.moveToNext()) {
                    add(Row(cursor.getLong(0), cursor.getString(1), cursor.getInt(2), cursor.getLong(3)))
                }
            }
        }
        rows.groupBy { SavedFood.nameKeyOf(it.name) }.forEach { (key, group) ->
            val keep = group.maxBy { it.lastUsedAt }
            group.filter { it !== keep }.forEach { duplicate ->
                db.execSQL("DELETE FROM saved_foods WHERE id = ?", arrayOf<Any>(duplicate.id))
            }
            db.execSQL(
                "UPDATE saved_foods SET name = ?, nameKey = ?, useCount = ? WHERE id = ?",
                arrayOf<Any>(keep.name.trim(), key, group.sumOf { it.useCount }, keep.id),
            )
        }

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_foods_nameKey` ON `saved_foods` (`nameKey`)")
    }
}
