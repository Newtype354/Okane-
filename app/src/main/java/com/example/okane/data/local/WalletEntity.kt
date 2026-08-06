package com.example.okane.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An account the user holds money in: cash, a bank account, an e-wallet.
 *
 * There is deliberately **no balance column**. A stored balance drifts the
 * moment any write path forgets to update it, and tracing the discrepancy
 * afterwards is far more painful than recomputing. Balance is derived from
 * [initialBalance] plus the transaction rows — see `WalletDao.observeBalances`.
 *
 * [monthlyFee] is stored but nothing schedules it. Recurring generation is its
 * own subsystem (WorkManager, month-length edge cases, catch-up after device
 * downtime, deduplication) and is explicitly out of scope for this wave. The
 * column exists now so adding the scheduler later needs no migration.
 *
 * Wallets are archived, never deleted: deleting one would orphan every
 * historical transaction that references it. [isArchived] hides it from
 * pickers while keeping it resolvable for old rows.
 */
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /** Whole rupiah the wallet already held before Okane started tracking. */
    val initialBalance: Long = 0,

    /** Whole rupiah. Stored only; no scheduler exists yet. */
    val monthlyFee: Long = 0,

    /** Exactly one wallet is the default. Enforced in WalletRepository. */
    val isDefault: Boolean = false,

    val sortOrder: Int = 0,

    val isArchived: Boolean = false,
)

/**
 * The checklist offered during onboarding — common Indonesian cash, bank and
 * e-wallet accounts. Presenting a checklist rather than an empty form is the
 * difference between a task the user finishes and one they abandon.
 */
object DefaultWallets {
    /** Seeded when the user skips onboarding entirely. */
    const val FALLBACK_NAME = "Tunai"

    val SUGGESTIONS: List<String> = listOf(
        FALLBACK_NAME, "BCA", "Mandiri", "BRI", "BNI",
        "GoPay", "OVO", "DANA", "ShopeePay",
    )
}
