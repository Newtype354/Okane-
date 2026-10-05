package com.example.okane.ui.dashboard

import com.example.okane.data.local.TransactionEntity
import com.example.okane.domain.CalmQuote
import com.example.okane.domain.CalmQuotes
import com.example.okane.domain.RecordingStreak
import com.example.okane.domain.SummaryCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Everything Home shows besides the wallet total (which comes from the same
 * computed balance flow as Dompetku, so the two screens can never disagree).
 */
data class HomeUiState(
    val streakDays: Int = 0,
    val recordedToday: Boolean = false,
    val nextMilestone: Int = RecordingStreak.nextMilestone(0),
    val incomeThisMonth: Long = 0,
    val expenseThisMonth: Long = 0,
    val transactionsThisMonth: Int = 0,
    val quote: CalmQuote = CalmQuotes.ALL.first(),
) {
    /** 0..1 toward [nextMilestone], for the progress ring. */
    val streakProgress: Float
        get() = (streakDays.toFloat() / nextMilestone).coerceIn(0f, 1f)
}

/** Pure mapping, run on Flow emissions rather than during composition. */
object HomePresenter {

    fun present(
        transactions: List<TransactionEntity>,
        now: Instant,
        zone: ZoneId,
    ): HomeUiState {
        val today = now.atZone(zone).toLocalDate()
        val month = YearMonth.from(today)
        val days = HashSet<LocalDate>(transactions.size)
        var thisMonth = 0
        for (transaction in transactions) {
            val day = Instant.ofEpochMilli(transaction.occurredAt).atZone(zone).toLocalDate()
            days += day
            if (YearMonth.from(day) == month) thisMonth++
        }
        val streak = RecordingStreak.current(days, today)
        // Income and expense reuse the dashboard's own arithmetic, so transfers
        // stay excluded exactly as they are everywhere else.
        val summary = SummaryCalculator.calculate(transactions, month, zone)
        return HomeUiState(
            streakDays = streak,
            recordedToday = today in days,
            nextMilestone = RecordingStreak.nextMilestone(streak),
            incomeThisMonth = summary.incomeThisMonth,
            expenseThisMonth = summary.expensesThisMonth,
            transactionsThisMonth = thisMonth,
            quote = CalmQuotes.forDay(today),
        )
    }
}
