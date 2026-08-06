package com.example.okane.domain

import java.time.LocalTime
import java.time.ZoneId

/**
 * The home screen greeting, chosen from the local time of day.
 *
 * Pure and hour-driven so it is testable without freezing a clock: the only
 * Android-free input that matters is which hour it is.
 *
 * Boundaries follow ordinary Japanese usage rather than an even three-way
 * split — `Ohayou` belongs to the early morning, `Konbanwa` covers the whole
 * evening and night, and `Konnichiwa` takes the middle of the day.
 */
object Greeting {

    fun forHour(hour: Int): String {
        require(hour in 0..23) { "Hour must be 0..23, was $hour" }
        return when (hour) {
            in 4..10 -> OHAYOU
            in 11..17 -> KONNICHIWA
            else -> KONBANWA      // 18..23 and 0..3
        }
    }

    /**
     * Read at composition time rather than cached, so opening the app in the
     * evening after leaving it open all afternoon shows the right greeting.
     * It will not change while the screen sits open — not worth a ticking
     * clock for a word.
     */
    fun now(zone: ZoneId = ZoneId.systemDefault()): String =
        forHour(LocalTime.now(zone).hour)

    const val OHAYOU = "Ohayou"
    const val KONNICHIWA = "Konnichiwa"
    const val KONBANWA = "Konbanwa"
}
