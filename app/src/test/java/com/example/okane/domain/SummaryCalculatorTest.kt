package com.example.okane.domain

import com.example.okane.data.local.TransactionEntity
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryCalculatorTest {

    // Fixed zone so tests behave the same on any machine.
    private val zone = ZoneId.of("Asia/Jakarta")
    private val july = YearMonth.of(2026, 7)

    private companion object {
        const val WALLET_CASH = 1L
        const val WALLET_BANK = 2L
    }

    private fun at(year: Int, month: Int, day: Int): Long =
        LocalDateTime.of(year, month, day, 12, 0)
            .atZone(zone).toInstant().toEpochMilli()

    /** A notification-captured row from a given app. */
    private fun tx(
        sourceApp: String,
        type: TransactionType,
        amount: Long,
        occurredAt: Long
    ) = TransactionEntity(
        amount = amount,
        type = type,
        walletId = WALLET_CASH,
        categoryId = null,
        occurredAt = occurredAt,
        createdAt = 0,
        source = TransactionSource.NOTIFICATION,
        note = "n",
        sourceApp = sourceApp,
        merchant = "m"
    )

    /** A keypad row: no originating app. */
    private fun manual(
        type: TransactionType,
        amount: Long,
        occurredAt: Long
    ) = TransactionEntity(
        amount = amount,
        type = type,
        walletId = WALLET_CASH,
        categoryId = null,
        occurredAt = occurredAt,
        createdAt = 0,
        source = TransactionSource.MANUAL
    )

    private fun transfer(amount: Long, occurredAt: Long) = TransactionEntity(
        amount = amount,
        type = TransactionType.TRANSFER,
        walletId = WALLET_BANK,
        toWalletId = WALLET_CASH,
        categoryId = null,
        occurredAt = occurredAt,
        createdAt = 0,
        source = TransactionSource.MANUAL
    )

    @Test
    fun `empty list gives all zeros`() {
        val s = SummaryCalculator.calculate(emptyList(), july, zone)
        assertEquals(0L, s.recordedNet)
        assertEquals(0L, s.incomeThisMonth)
        assertEquals(0L, s.expensesThisMonth)
        assertEquals(emptyMap<String, Long>(), s.bySource)
    }

    @Test
    fun `income adds and expense subtracts`() {
        val s = SummaryCalculator.calculate(
            listOf(
                tx("GoPay", TransactionType.IN, 100_000, at(2026, 7, 1)),
                tx("GoPay", TransactionType.OUT, 30_000, at(2026, 7, 2))
            ),
            july, zone
        )
        assertEquals(70_000L, s.recordedNet)
        assertEquals(100_000L, s.incomeThisMonth)
        assertEquals(30_000L, s.expensesThisMonth)
        assertEquals(70_000L, s.netThisMonth)
    }

    @Test
    fun `only current month counts for monthly figures but recordedNet is all-time`() {
        val s = SummaryCalculator.calculate(
            listOf(
                tx("Livin", TransactionType.IN, 500_000, at(2026, 6, 30)), // June
                tx("Livin", TransactionType.OUT, 50_000, at(2026, 7, 3))   // July
            ),
            july, zone
        )
        assertEquals(450_000L, s.recordedNet)       // both months
        assertEquals(0L, s.incomeThisMonth)         // June income excluded
        assertEquals(50_000L, s.expensesThisMonth)  // July only
        assertEquals(-50_000L, s.netThisMonth)
    }

    @Test
    fun `bySource nets each source independently`() {
        val s = SummaryCalculator.calculate(
            listOf(
                tx("GoPay", TransactionType.OUT, 25_000, at(2026, 7, 1)),
                tx("GoPay", TransactionType.OUT, 10_000, at(2026, 7, 2)),
                tx("Jenius", TransactionType.IN, 1_000_000, at(2026, 7, 1))
            ),
            july, zone
        )
        assertEquals(-35_000L, s.bySource["GoPay"])
        assertEquals(1_000_000L, s.bySource["Jenius"])
        assertEquals(965_000L, s.recordedNet)
    }

    @Test
    fun `manual entries are bucketed under their own label, not dropped`() {
        val s = SummaryCalculator.calculate(
            listOf(
                manual(TransactionType.OUT, 15_000, at(2026, 7, 4)),
                manual(TransactionType.IN, 200_000, at(2026, 7, 5)),
                tx("GoPay", TransactionType.OUT, 5_000, at(2026, 7, 6))
            ),
            july, zone
        )
        assertEquals(185_000L, s.bySource[SummaryCalculator.MANUAL_LABEL])
        assertEquals(-5_000L, s.bySource["GoPay"])
        // Breakdown must sum to the net, or the screen contradicts itself.
        assertEquals(s.recordedNet, s.bySource.values.sum())
    }

    @Test
    fun `manual and notification rows share the monthly totals`() {
        val s = SummaryCalculator.calculate(
            listOf(
                manual(TransactionType.OUT, 15_000, at(2026, 7, 4)),
                tx("GoPay", TransactionType.OUT, 5_000, at(2026, 7, 6))
            ),
            july, zone
        )
        assertEquals(20_000L, s.expensesThisMonth)
    }

    // --- transfers are net-zero and must not reach any figure --------------

    @Test
    fun `a transfer alone leaves every figure at zero`() {
        val s = SummaryCalculator.calculate(
            listOf(transfer(1_000_000, at(2026, 7, 10))),
            july, zone
        )
        assertEquals(0L, s.recordedNet)
        assertEquals(0L, s.incomeThisMonth)
        assertEquals(0L, s.expensesThisMonth)
        assertEquals(emptyMap<String, Long>(), s.bySource)
    }

    @Test
    fun `a transfer does not inflate monthly income or expenses`() {
        // Topping up an e-wallet from a bank account is routine. Recorded as a
        // paired Keluar + Masuk it would add 500k to BOTH monthly totals for
        // money that never left the user's control.
        val s = SummaryCalculator.calculate(
            listOf(
                manual(TransactionType.OUT, 20_000, at(2026, 7, 4)),
                transfer(500_000, at(2026, 7, 5)),
                manual(TransactionType.IN, 100_000, at(2026, 7, 6))
            ),
            july, zone
        )
        assertEquals(100_000L, s.incomeThisMonth)
        assertEquals(20_000L, s.expensesThisMonth)
        assertEquals(80_000L, s.recordedNet)
    }
}
