package com.example.okane.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType

/**
 * One recorded transaction — the single table both entry paths write to:
 * the keypad (MANUAL) and the notification listener (NOTIFICATION).
 *
 * Invariants worth keeping:
 *
 * - **Positive [amount], direction in [type].** Reporting is
 *   `WHERE type = 'OUT'` instead of `WHERE amount < 0`, per-category totals
 *   need no `ABS()`, and sign-flip bugs on edit cannot occur. Critically, a
 *   signed amount cannot express a transfer at all: the same row is negative
 *   for one wallet and positive for another.
 * - **[Long] in whole rupiah.** No floating point anywhere in the money path.
 * - **[source] exists before it is fully used.** Cheaper now than a migration.
 * - **[occurredAt] and [createdAt] are separate.** People log a lunch expense
 *   at night. Collapsing them makes daily reports wrong and backdating
 *   impossible.
 *
 * [toWalletId] is non-null only for [TransactionType.TRANSFER]; [categoryId]
 * is null for transfers and for anything the user left uncategorised.
 *
 * [sourceApp] and [merchant] are NOT in the brief's model. They are additive
 * and nullable, and they exist because the notification listener already
 * ships: its per-app Settings toggles and the dashboard's per-source breakdown
 * need to know a row came from GoPay rather than Livin, which [source]
 * (MANUAL vs NOTIFICATION) cannot express. Manual entries leave both null.
 *
 * The unique index is the listener's duplicate protection — Android can
 * deliver the same notification more than once. It does not constrain manual
 * entry, because SQLite treats NULLs as distinct in a unique index and
 * [sourceApp] is null for every keypad row, so a user may legitimately log the
 * same amount twice in the same millisecond.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(
            value = ["sourceApp", "type", "amount", "occurredAt"],
            unique = true
        ),
        // The balance query sums over these two columns per wallet.
        Index(value = ["walletId"]),
        Index(value = ["toWalletId"]),
        // Category ranking joins on this, and the inbox filters on it.
        Index(value = ["categoryId"]),
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Always positive, whole rupiah, no decimals. */
    val amount: Long,

    val type: TransactionType,

    /** Source wallet. Never null — every transaction belongs to a wallet. */
    val walletId: Long,

    /** Destination wallet; non-null only for TRANSFER. */
    val toWalletId: Long? = null,

    /** Null for TRANSFER and for uncategorised entries. */
    val categoryId: Long? = null,

    /** When the transaction happened (epoch millis). Backdatable. */
    val occurredAt: Long,

    /** When this row was written (epoch millis). Never backdated. */
    val createdAt: Long,

    val source: TransactionSource,

    val note: String? = null,

    /** Which financial app produced this, e.g. "GoPay". Null for manual. */
    val sourceApp: String? = null,

    /** Store or recipient parsed from a notification. Null for manual. */
    val merchant: String? = null,
)

/** Row shape for the grouped monthly totals query. */
data class TypeTotal(
    val type: TransactionType,
    val total: Long,
)

/** Row shape for the computed per-wallet balance query. */
data class WalletBalance(
    val walletId: Long,
    val name: String,
    val balance: Long,
    val isDefault: Boolean,
    val initialBalance: Long,
    val monthlyFee: Long,
    val isArchived: Boolean,
)
