package com.abccash.app.treasury.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val FLEXIBLE_DATE_FORMATTERS = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    DateTimeFormatter.ofPattern("d/M/yyyy"),
    DateTimeFormatter.ofPattern("dd/MM/yy"),
    DateTimeFormatter.ofPattern("d/M/yy"),
    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    DateTimeFormatter.ofPattern("d-M-yyyy")
)

fun parseFlexibleLocalDate(raw: String): LocalDate? {
    val value = raw.trim().substringBefore(' ').substringBefore('T')
    if (value.isBlank()) return null
    return FLEXIBLE_DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
        runCatching { LocalDate.parse(value, formatter) }.getOrNull()
    }
}

fun defaultDateForMonth(month: YearMonth): LocalDate {
    val today = LocalDate.now()
    return if (YearMonth.from(today) == month) today else month.atDay(1)
}
