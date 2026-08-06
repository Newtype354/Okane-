package com.example.okane.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {

    @Test
    fun `early morning through mid-morning is Ohayou`() {
        assertEquals(Greeting.OHAYOU, Greeting.forHour(4))
        assertEquals(Greeting.OHAYOU, Greeting.forHour(7))
        assertEquals(Greeting.OHAYOU, Greeting.forHour(10))
    }

    @Test
    fun `late morning through late afternoon is Konnichiwa`() {
        assertEquals(Greeting.KONNICHIWA, Greeting.forHour(11))
        assertEquals(Greeting.KONNICHIWA, Greeting.forHour(14))
        assertEquals(Greeting.KONNICHIWA, Greeting.forHour(17))
    }

    @Test
    fun `evening and the small hours are Konbanwa`() {
        assertEquals(Greeting.KONBANWA, Greeting.forHour(18))
        assertEquals(Greeting.KONBANWA, Greeting.forHour(23))
        assertEquals(Greeting.KONBANWA, Greeting.forHour(0))
        assertEquals(Greeting.KONBANWA, Greeting.forHour(3))
    }

    @Test
    fun `every hour of the day maps to a greeting`() {
        // A static greeting is wrong for most of the day; this pins that no
        // hour falls through a gap in the ranges.
        val greetings = (0..23).map { Greeting.forHour(it) }
        assertEquals(24, greetings.size)
        assertEquals(
            setOf(Greeting.OHAYOU, Greeting.KONNICHIWA, Greeting.KONBANWA),
            greetings.toSet()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an impossible hour is rejected`() {
        Greeting.forHour(24)
    }
}
