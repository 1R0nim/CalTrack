package com.example.caltrack

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.caltrack.data.local.CalTrackDatabase
import com.example.caltrack.data.local.MIGRATION_1_2
import com.example.caltrack.data.local.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodDatabaseMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migration2To3PreservesExistingEntriesAndSavedFoods() {
        val databaseName = "food-migration-v2-test"
        createLegacyDatabase(databaseName, version = 2)
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                """INSERT INTO food_entries
                    (id, name, grams, kcal, protein, carbs, fat, date, loggedAt)
                    VALUES (1, 'Juice', 200, 90, 1, 22, 0, 100, 1000)"""
            )
            database.execSQL(
                """INSERT INTO saved_foods
                    (id, name, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g,
                     defaultGrams, barcode, useCount, lastUsedAt, nameKey)
                    VALUES (1, 'Juice', 45, 0.5, 11, 0, 200, '12345678', 3, 1000, 'juice')"""
            )
        }

        val migrated = openRoomDatabase(databaseName, MIGRATION_1_2, MIGRATION_2_3)
        migrated.openHelper.writableDatabase.query(
            "SELECT quantityUnit, kcal FROM food_entries WHERE id = 1",
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("g", cursor.getString(0))
            assertEquals(90.0, cursor.getDouble(1), 0.0)
        }
        migrated.openHelper.writableDatabase.query(
            "SELECT quantityUnit, dataSource, barcode, kcalPer100g FROM saved_foods WHERE id = 1",
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("g", cursor.getString(0))
            assertEquals("manual", cursor.getString(1))
            assertEquals("12345678", cursor.getString(2))
            assertEquals(45.0, cursor.getDouble(3), 0.0)
        }
        migrated.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromVersion1RetainsLegacyNameVariants() {
        val databaseName = "food-migration-v1-test"
        createLegacyDatabase(databaseName, version = 1)
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                """INSERT INTO saved_foods
                    (id, name, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g,
                     defaultGrams, barcode, useCount, lastUsedAt)
                    VALUES
                    (1, 'Brânză', 250, 17, 2, 20, 100, '11111111', 2, 1000),
                    (2, 'Brânză ', 260, 18, 3, 21, 100, '22222222', 4, 2000)"""
            )
        }

        val migrated = openRoomDatabase(databaseName, MIGRATION_1_2, MIGRATION_2_3)
        migrated.openHelper.writableDatabase.query(
            "SELECT id, name, kcalPer100g, nameKey FROM saved_foods ORDER BY id",
        ).use { cursor ->
            assertEquals(2, cursor.count)
            check(cursor.moveToFirst())
            assertEquals("Brânză", cursor.getString(1))
            assertEquals(250.0, cursor.getDouble(2), 0.0)
            val firstKey = cursor.getString(3)
            check(cursor.moveToNext())
            assertEquals("Brânză ", cursor.getString(1))
            assertEquals(260.0, cursor.getDouble(2), 0.0)
            assertNotEquals(firstKey, cursor.getString(3))
        }
        migrated.close()
        context.deleteDatabase(databaseName)
    }

    private fun openRoomDatabase(
        name: String,
        vararg migrations: androidx.room.migration.Migration,
    ): CalTrackDatabase =
        Room.databaseBuilder(context, CalTrackDatabase::class.java, name)
            .addMigrations(*migrations)
            .build()

    private fun createLegacyDatabase(name: String, version: Int) {
        context.deleteDatabase(name)
        val database = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        database.execSQL(
            """CREATE TABLE food_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                grams REAL NOT NULL,
                kcal REAL NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                date INTEGER NOT NULL,
                loggedAt INTEGER NOT NULL
            )""",
        )
        database.execSQL(
            "CREATE INDEX index_food_entries_date ON food_entries(date)",
        )
        val nameKeyColumn = if (version >= 2) ", nameKey TEXT NOT NULL DEFAULT ''" else ""
        database.execSQL(
            """CREATE TABLE saved_foods (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL COLLATE NOCASE,
                kcalPer100g REAL NOT NULL,
                proteinPer100g REAL NOT NULL,
                carbsPer100g REAL NOT NULL,
                fatPer100g REAL NOT NULL,
                defaultGrams REAL NOT NULL,
                barcode TEXT,
                useCount INTEGER NOT NULL,
                lastUsedAt INTEGER NOT NULL$nameKeyColumn
            )""",
        )
        database.execSQL("CREATE UNIQUE INDEX index_saved_foods_name ON saved_foods(name)")
        database.execSQL("CREATE INDEX index_saved_foods_barcode ON saved_foods(barcode)")
        if (version >= 2) {
            database.execSQL("CREATE UNIQUE INDEX index_saved_foods_nameKey ON saved_foods(nameKey)")
        }
        database.version = version
        database.close()
    }
}
