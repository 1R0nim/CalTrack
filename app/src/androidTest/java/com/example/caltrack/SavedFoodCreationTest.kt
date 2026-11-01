package com.example.caltrack

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.caltrack.data.local.CalTrackDatabase
import com.example.caltrack.data.local.entity.SavedFood
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedFoodCreationTest {
    @Test
    fun insertNewRejectsDuplicateBarcodeAndNameWithoutLoggingFood() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, CalTrackDatabase::class.java).build()
        try {
            val dao = database.savedFoodDao()
            val food = SavedFood(
                name = "Brânză",
                kcalPer100 = 250.0,
                proteinPer100 = 17.0,
                carbsPer100 = 1.0,
                fatPer100 = 20.0,
                defaultAmount = 100.0,
                barcode = "12345678",
            )

            assertTrue(dao.insertNew(food))
            assertFalse(dao.insertNew(food.copy(name = "Alt produs")))
            assertFalse(dao.insertNew(food.copy(name = "BRÂNZĂ", barcode = "87654321")))

            val savedFoods = dao.observeAll().first()
            assertEquals(1, savedFoods.size)
            assertEquals(0, savedFoods.single().useCount)
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM food_entries").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }
}
