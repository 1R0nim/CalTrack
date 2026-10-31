package com.example.caltrack.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// The UI is Romanian-only, so dates and numbers follow it regardless of the device language
private val Romanian: Locale = Locale.forLanguageTag("ro-RO")

/** At most one decimal: "120", "12,5". */
fun formatAmount(value: Double): String =
    DecimalFormat("0.#", DecimalFormatSymbols(Romanian)).format(value)

fun formatRounded(value: Double): String = value.roundToInt().toString()

/** Accepts both "12,5" and "12.5"; null when the text isn't a number. */
fun parseAmount(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** "28 oct", or "28 oct 2025" when [withYear]; Java's Romanian short months end in a dot. */
fun formatShortDate(date: LocalDate, withYear: Boolean): String =
    date.format(DateTimeFormatter.ofPattern(if (withYear) "d MMM yyyy" else "d MMM", Romanian))
        .replace(".", "")

/** "Octombrie 2026" */
fun formatMonthYear(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Romanian))
        .replaceFirstChar { it.titlecase(Romanian) }

/** Single-letter weekday headers, Monday first: "L", "M", "M", "J", "V", "S", "D". */
val narrowWeekdays: List<String> =
    DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, Romanian).uppercase(Romanian) }
