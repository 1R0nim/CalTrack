package com.example.caltrack.data.local.entity

/** Unit used for both the logged amount and the saved food's per-100 nutrition basis. */
enum class FoodQuantityUnit(val code: String) {
    GRAMS("g"),
    MILLILITERS("ml");

    companion object {
        fun fromCode(code: String): FoodQuantityUnit =
            entries.firstOrNull { it.code == code } ?: GRAMS
    }
}
