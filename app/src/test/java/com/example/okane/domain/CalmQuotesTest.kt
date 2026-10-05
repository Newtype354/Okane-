package com.example.okane.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmQuotesTest {

    @Test
    fun sameDayGivesSameQuote() {
        val day = LocalDate.of(2026, 10, 5)
        assertEquals(CalmQuotes.forDay(day), CalmQuotes.forDay(day))
    }

    @Test
    fun consecutiveDaysRotate() {
        val day = LocalDate.of(2026, 10, 5)
        assertNotEquals(CalmQuotes.forDay(day), CalmQuotes.forDay(day.plusDays(1)))
    }

    @Test
    fun everyQuoteIsAttributedToAProverbOrOkane() {
        assertTrue(CalmQuotes.ALL.all { it.source == "Peribahasa" || it.source == "Okane" })
    }

    @Test
    fun datesBeforeTheEpochDoNotCrash() {
        CalmQuotes.forDay(LocalDate.of(1960, 1, 1))
    }
}
