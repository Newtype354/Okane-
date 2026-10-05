package com.example.okane.ui.dashboard

import com.example.okane.data.local.TransactionEntity
import com.example.okane.domain.CalmQuotes
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePresenterTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 10, 3)
    private val now = today.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant()
    private var nextId = 1L

    private fun tx(day: LocalDate, amount: Long, type: TransactionType): TransactionEntity {
        val at = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        return TransactionEntity(
            id = nextId++,
            amount = amount,
            type = type,
            walletId = 1,
            toWalletId = if (type == TransactionType.TRANSFER) 2 else null,
            occurredAt = at,
            createdAt = at,
            source = TransactionSource.MANUAL,
        )
    }

    @Test
    fun emptyDatabaseIsAQuietStart() {
        val state = HomePresenter.present(emptyList(), now, zone)
        assertEquals(0, state.streakDays)
        assertFalse(state.recordedToday)
        assertEquals(3, state.nextMilestone)
        assertEquals(0L, state.incomeThisMonth)
        assertEquals(0L, state.expenseThisMonth)
        assertEquals(0, state.transactionsThisMonth)
        assertEquals(0f, state.streakProgress, 0.0001f)
    }

    @Test
    fun streakCrossesMonthBoundaryButTotalsDoNot() {
        val rows = listOf(
            tx(today, 20_000, TransactionType.OUT),                 // 3 Oct
            tx(today.minusDays(1), 5_000_000, TransactionType.IN),  // 2 Oct
            tx(today.minusDays(2), 300_000, TransactionType.TRANSFER), // 1 Oct
            tx(today.minusDays(3), 50_000, TransactionType.OUT),    // 30 Sep
        )
        val state = HomePresenter.present(rows, now, zone)

        assertTrue(state.recordedToday)
        // Four consecutive local days, September included.
        assertEquals(4, state.streakDays)
        assertEquals(7, state.nextMilestone)
        // October only, and the transfer counts as neither side.
        assertEquals(5_000_000L, state.incomeThisMonth)
        assertEquals(20_000L, state.expenseThisMonth)
        // The transfer is still an October record.
        assertEquals(3, state.transactionsThisMonth)
    }

    @Test
    fun quoteFollowsTheLocalDay() {
        val state = HomePresenter.present(emptyList(), now, zone)
        assertEquals(CalmQuotes.forDay(today), state.quote)
    }

    @Test
    fun lateNightEntryBelongsToItsLocalDay() {
        // 00:30 in Jakarta is still the previous day in UTC. Days are local,
        // so this entry must count for today.
        val earlyMorning = today.atTime(0, 30).atZone(zone).toInstant().toEpochMilli()
        val row = TransactionEntity(
            id = 99,
            amount = 10_000,
            type = TransactionType.OUT,
            walletId = 1,
            occurredAt = earlyMorning,
            createdAt = earlyMorning,
            source = TransactionSource.MANUAL,
        )
        val state = HomePresenter.present(listOf(row), now, zone)
        assertTrue(state.recordedToday)
        assertEquals(1, state.streakDays)
    }
}
