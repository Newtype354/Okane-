package com.example.okane.domain.model

/**
 * Everything the home screen's headline shows, precomputed. All values in
 * whole rupiah, transfers excluded.
 *
 * [recordedNet] remains the all-time transaction net used by reporting tests
 * and source breakdowns. The Home headline is sourced from WalletRepository's
 * computed all-wallet balances instead, so it also includes initial balances
 * and archived wallets without duplicating balance arithmetic.
 */
data class DashboardSummary(
    val recordedNet: Long,
    val incomeThisMonth: Long,
    val expensesThisMonth: Long,
    // Net flow per source, e.g. "GoPay" -> -125000.
    val bySource: Map<String, Long>
) {
    /** Income minus expenses for the current month — the headline figure. */
    val netThisMonth: Long get() = incomeThisMonth - expensesThisMonth

    companion object {
        val EMPTY = DashboardSummary(
            recordedNet = 0,
            incomeThisMonth = 0,
            expensesThisMonth = 0,
            bySource = emptyMap()
        )
    }
}
