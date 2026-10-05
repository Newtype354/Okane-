package com.example.okane.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingStreakTest {

    private val today = LocalDate.of(2026, 10, 5)

    private fun days(vararg offsets: Long): Set<LocalDate> =
        offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun noRecordsIsZero() {
        assertEquals(0, RecordingStreak.current(emptySet(), today))
    }

    @Test
    fun countsConsecutiveDaysEndingToday() {
        assertEquals(3, RecordingStreak.current(days(0, 1, 2), today))
    }

    @Test
    fun aGapEndsTheStreak() {
        assertEquals(2, RecordingStreak.current(days(0, 1, 3, 4, 5), today))
    }

    @Test
    fun nothingYetTodayStillCountsThroughYesterday() {
        // The day is not over, so yesterday's run is not broken yet.
        assertEquals(4, RecordingStreak.current(days(1, 2, 3, 4), today))
    }

    @Test
    fun aMissedYesterdayResetsToZeroWhenTodayIsEmpty() {
        assertEquals(0, RecordingStreak.current(days(2, 3), today))
    }

    @Test
    fun milestonesStepUpAndContinueYearly() {
        assertEquals(3, RecordingStreak.nextMilestone(0))
        assertEquals(7, RecordingStreak.nextMilestone(3))
        assertEquals(14, RecordingStreak.nextMilestone(12))
        assertEquals(730, RecordingStreak.nextMilestone(365))
        assertEquals(0.5f, RecordingStreak.progress(7), 0.0001f)
        assertEquals(0f, RecordingStreak.progress(0), 0.0001f)
    }
}
