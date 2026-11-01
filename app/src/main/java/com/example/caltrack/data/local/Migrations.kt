package com.example.caltrack.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.caltrack.data.local.entity.SavedFood

/**
 * Adds saved_foods.nameKey (trimmed, lowercased name) with a unique index. Legacy names that
 * normalize to the same key are retained with a row-specific suffix; no saved food or journal
 * entry is deleted or merged.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `nameKey` TEXT NOT NULL DEFAULT ''")

        class Row(val id: Long, val name: String, val lastUsedAt: Long)
        val rows = buildList {
            db.query("SELECT id, name, lastUsedAt FROM saved_foods").use { cursor ->
                while (cursor.moveToNext()) {
                    add(Row(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
                }
            }
        }

        rows.groupBy { SavedFood.nameKeyOf(it.name) }.forEach { (key, group) ->
            val ordered = group.sortedWith(
                compareByDescending<Row> { it.lastUsedAt }.thenBy { it.id },
            )
            ordered.forEachIndexed { index, row ->
                val storedKey = if (index == 0) key else "$key#legacy-${row.id}"
                db.execSQL(
                    "UPDATE saved_foods SET nameKey = ? WHERE id = ?",
                    arrayOf<Any>(storedKey, row.id),
                )
            }
        }

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_foods_nameKey` ON `saved_foods` (`nameKey`)")
    }
}

/** Adds explicit quantity units and retained Open Food Facts product metadata without dropping data. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `food_entries` ADD COLUMN `quantityUnit` TEXT NOT NULL DEFAULT 'g'")
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `quantityUnit` TEXT NOT NULL DEFAULT 'g'")
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `dataSource` TEXT NOT NULL DEFAULT 'manual'")
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `brand` TEXT")
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `productQuantity` TEXT")
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `servingSize` TEXT")
        db.execSQL("ALTER TABLE `saved_foods` ADD COLUMN `imageUrl` TEXT")
    }
}
