package com.example.caltrack.data.local

import androidx.room.TypeConverter
import com.example.caltrack.data.local.entity.FoodQuantityUnit
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun foodQuantityUnitToCode(unit: FoodQuantityUnit): String = unit.code

    @TypeConverter
    fun foodQuantityUnitFromCode(code: String): FoodQuantityUnit = FoodQuantityUnit.fromCode(code)
}
