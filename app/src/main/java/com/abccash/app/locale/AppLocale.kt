package com.abccash.app.locale

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object AppLocale {

    fun current(): Locale = Locale.getDefault()

    fun monthYear(date: LocalDate, locale: Locale = current()): String =
        date.format(monthYearFormatter(locale)).replaceFirstChar { it.uppercase(locale) }

    fun monthYear(month: YearMonth, locale: Locale = current()): String =
        monthYear(month.atDay(1), locale)

    fun shortMonth(date: LocalDate, locale: Locale = current()): String =
        date.format(shortMonthFormatter(locale)).replaceFirstChar { it.uppercase(locale) }

    fun dayMonthYear(date: LocalDate, locale: Locale = current()): String =
        date.format(dayMonthYearFormatter(locale))

    fun dayMonth(date: LocalDate, locale: Locale = current()): String =
        date.format(dayMonthFormatter(locale))

    fun shortDayMonthYear(date: LocalDate, locale: Locale = current()): String =
        date.format(shortDayMonthYearFormatter(locale))

    fun shortDayMonthYearFormatter(locale: Locale = current()): DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    fun monthYearFormatter(locale: Locale = current()): DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)

    fun shortMonthFormatter(locale: Locale = current()): DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM", locale)

    private fun dayMonthYearFormatter(locale: Locale = current()): DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", locale)

    private fun dayMonthFormatter(locale: Locale = current()): DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM", locale)
}
