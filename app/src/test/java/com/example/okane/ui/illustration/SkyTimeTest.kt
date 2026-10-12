package com.example.okane.ui.illustration

import com.example.okane.domain.Greeting
import org.junit.Assert.assertEquals
import org.junit.Test

class SkyTimeTest {

    @Test
    fun `the sky always matches the greeting`() {
        val expected = mapOf(
            Greeting.OHAYOU to SkyTime.MORNING,
            Greeting.KONNICHIWA to SkyTime.DAY,
            Greeting.KONBANWA to SkyTime.NIGHT,
        )
        for (hour in 0..23) {
            assertEquals("hour $hour", expected.getValue(Greeting.forHour(hour)), SkyTime.forHour(hour))
        }
    }
}
