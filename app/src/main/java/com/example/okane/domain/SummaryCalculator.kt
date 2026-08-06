package com.example.okane.domain

import com.example.okane.data.local.TransactionEntity
import com.example.okane.domain.model.DashboardSummary
import com.example.okane.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Pure function: transaction list in, dashboard numbers out.
 * month/zone are parameters (instead of "now"/system defaults) so unit
 * tests are deterministic.
 */
object SummaryCalculator {

    /**
     * Bucket label for keypad entries, which have no originating app.
     * Manual rows must still appear in the per-source breakdown, or the
     * breakdown would silently disagree with the totals above it.
     */
    const val MANUAL_LABEL = "Manual"

    fun calculate(
        transactions: List<TransactionEntity>,
        month: YearMonth,
        zone: ZoneId
    ): DashboardSummary {
        var recorded = 0L
        var income = 0L
        var expenses = 0L
        val bySource = mutableMapOf<String, Long>()

        for (t in transactions) {
            // Transfers move money between the user's own wallets. They are
            // net-zero overall, so they contribute to neither the running
            // total nor either monthly figure — counting them would inflate
            // both sides for money that never left the user's control.
            // Per-wallet effects are handled by the balance query instead.
            if (t.type == TransactionType.TRANSFER) continue

            val isIncome = t.type == TransactionType.IN
            val signed = if (isIncome) t.amount else -t.amount
            recorded += signed

            val bucket = t.sourceApp ?: MANUAL_LABEL
            bySource[bucket] = (bySource[bucket] ?: 0L) + signed

            val txMonth = YearMonth.from(Instant.ofEpochMilli(t.occurredAt).atZone(zone))
            if (txMonth == month) {
                if (isIncome) income += t.amount else expenses += t.amount
            }
        }

        return DashboardSummary(
            recordedNet = recorded,
            incomeThisMonth = income,
            expensesThisMonth = expenses,
            bySource = bySource
        )
    }
}
