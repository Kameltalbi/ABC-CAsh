package com.abccash.app.treasury.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FlexibleDateParserTest {

    @Test
    fun `parseFlexibleLocalDate accepts French format`() {
        assertEquals(LocalDate.of(2026, 3, 15), parseFlexibleLocalDate("15/03/2026"))
    }

    @Test
    fun `defaultDateForMonth uses first day for other months`() {
        val march = YearMonth.of(2026, 3)
        assertEquals(LocalDate.of(2026, 3, 1), defaultDateForMonth(march))
    }

    @Test
    fun `parseFlexibleLocalDate rejects invalid input`() {
        assertNull(parseFlexibleLocalDate("not-a-date"))
    }
}
