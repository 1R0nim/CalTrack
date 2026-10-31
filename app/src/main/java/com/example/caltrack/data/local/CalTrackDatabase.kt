package com.example.caltrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.caltrack.data.local.dao.FoodEntryDao
import com.example.caltrack.data.local.dao.SavedFoodDao
import com.example.caltrack.data.local.entity.FoodEntry
import com.example.caltrack.data.local.entity.SavedFood

@Database(
    entities = [FoodEntry::class, SavedFood::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CalTrackDatabase : RoomDatabase() {

    abstract fun foodEntryDao(): FoodEntryDao

    abstract fun savedFoodDao(): SavedFoodDao

    companion object {
        @Volatile
        private var instance: CalTrackDatabase? = null

        // No destructive migration fallback on purpose: schema changes must ship a Migration
        fun getInstance(context: Context): CalTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalTrackDatabase::class.java,
                    "caltrack.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
