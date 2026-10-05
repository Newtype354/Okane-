package com.example.okane.domain

import java.time.LocalDate

/**
 * How many days in a row the user has recorded at least one transaction.
 *
 * The product premise is that logging must become a habit, so Home shows this
 * the way a meditation app shows a practice streak: as encouragement, never as
 * a penalty. Two rules keep it kind:
 *
 * - A streak is not broken until the day is over. With nothing recorded yet
 *   today, the count still runs through yesterday, and Home nudges instead.
 * - Days are local calendar days of `occurredAt`, so a lunch logged at night
 *   or a backdated entry fills the day it actually belongs to.
 *
 * Pure and clock-free: callers pass `today`, so it is deterministic in tests.
 */
object RecordingStreak {

    /** Gentle targets for the progress ring; past the last, every 365 days. */
    val MILESTONES: List<Int> = listOf(3, 7, 14, 30, 60, 100, 180, 365)

    fun current(recordedDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in recordedDays) today else today.minusDays(1)
        var count = 0
        while (day in recordedDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** The next target strictly above [streak]. */
    fun nextMilestone(streak: Int): Int =
        MILESTONES.firstOrNull { it > streak } ?: ((streak / 365) + 1) * 365

    /** 0..1 progress from zero toward [nextMilestone]. */
    fun progress(streak: Int): Float =
        (streak.toFloat() / nextMilestone(streak)).coerceIn(0f, 1f)
}
