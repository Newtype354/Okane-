package com.example.okane.domain.capture

import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import kotlin.math.abs

/** The time and money tolerances the capture rules share. */
object CaptureRules {
    const val MINUTE = 60_000L

    /** The same notification re-posted (an update, a reboot, a backfill). */
    const val SAME_NOTIFICATION_WINDOW = 10 * MINUTE

    /** The same purchase reported twice, e.g. by the app and by SMS. */
    const val SIMILAR_WINDOW = 30 * MINUTE

    /** Both halves of one transfer between own wallets. */
    const val TRANSFER_WINDOW = 15 * MINUTE

    /**
     * The most a transfer may lose on the way and still be the same transfer.
     * Covers the usual Indonesian fees: e-wallet top-up admin (Rp1.000–2.500),
     * BI-FAST (Rp2.500) and online transfer (Rp6.500).
     */
    const val MAX_TRANSFER_FEE = 6_500L

    /** Merchant of the extra row that records what a transfer cost. */
    const val FEE_LABEL = "Biaya admin"
}

/**
 * Pure matching between a notification and what is already recorded.
 * Everything here takes plain lists so it can be tested without a database.
 */
object CaptureMatching {

    /** The text stored in `note` for rows from notifications; also the re-post fingerprint. */
    fun noteFor(title: String?, text: String): String = "title=$title | text=$text"

    /**
     * The active wallet that belongs to [source]: one named exactly like it
     * ("Livin"), else one named by an alias ("Mandiri", "Bank BTN"). Null when
     * the user keeps no wallet for it.
     */
    fun walletFor(source: FinanceSource, wallets: List<WalletEntity>): WalletEntity? {
        val active = wallets.filter { !it.isArchived }.sortedWith(compareBy({ it.sortOrder }, { it.name }))
        return active.firstOrNull { it.name.trim().equals(source.name, ignoreCase = true) }
            ?: active.firstOrNull { SourceCatalog.walletMatches(source, it.name) }
    }

    /**
     * A transfer already recorded between the same two wallets, close in time
     * and amount: the other half of this notification's transfer.
     */
    fun coveringTransfer(
        rows: List<TransactionEntity>,
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        at: Long,
    ): TransactionEntity? = rows
        .filter {
            it.type == TransactionType.TRANSFER &&
                it.walletId == fromWalletId &&
                it.toWalletId == toWalletId &&
                abs(it.amount - amount) <= CaptureRules.MAX_TRANSFER_FEE &&
                abs(it.occurredAt - at) <= CaptureRules.TRANSFER_WINDOW
        }
        .minByOrNull { abs(it.occurredAt - at) }

    /**
     * The other app's notification of the same transfer, recorded as plain
     * income or spending before this one arrived, and still safe to turn into
     * the transfer it really was: from a notification (never a hand-typed
     * row), from a different app, and not yet given a category.
     *
     * [sendingSide] is true when this notification is the paying side; the
     * paying side's amount is the larger one, by at most the fee.
     *
     * With [mergeableOnly] false, any row in that place counts — a hand-typed
     * or categorised one included — for warning rather than merging.
     */
    fun otherHalf(
        rows: List<TransactionEntity>,
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        sendingSide: Boolean,
        sourceApp: String,
        at: Long,
        mergeableOnly: Boolean = true,
    ): TransactionEntity? = rows
        .filter { row ->
            val wantType = if (sendingSide) TransactionType.IN else TransactionType.OUT
            val wantWallet = if (sendingSide) toWalletId else fromWalletId
            val fee = if (sendingSide) amount - row.amount else row.amount - amount
            val mergeable = row.source == TransactionSource.NOTIFICATION &&
                row.sourceApp != sourceApp &&
                row.categoryId == null
            (mergeable || !mergeableOnly) &&
                row.type == wantType &&
                row.walletId == wantWallet &&
                fee in 0..CaptureRules.MAX_TRANSFER_FEE &&
                abs(row.occurredAt - at) <= CaptureRules.TRANSFER_WINDOW
        }
        .minByOrNull { abs(it.occurredAt - at) }

    /**
     * Rows that may already be this income or spending: same wallet,
     * direction and amount, close in time, from any source — a hand-typed
     * entry for the same coffee counts.
     */
    fun similar(
        rows: List<TransactionEntity>,
        walletId: Long,
        type: TransactionType,
        amount: Long,
        at: Long,
    ): TransactionEntity? = rows
        .filter {
            it.type == type &&
                it.walletId == walletId &&
                it.amount == amount &&
                abs(it.occurredAt - at) <= CaptureRules.SIMILAR_WINDOW
        }
        .minByOrNull { abs(it.occurredAt - at) }

    /**
     * A transfer that already accounts for this income or spending: money
     * that arrived in (or left) [walletId] as one half of a recorded transfer.
     */
    fun transferTouching(
        rows: List<TransactionEntity>,
        walletId: Long,
        direction: MoneyDirection,
        amount: Long,
        at: Long,
    ): TransactionEntity? = rows
        .filter {
            it.type == TransactionType.TRANSFER &&
                (if (direction == MoneyDirection.IN) it.toWalletId == walletId else it.walletId == walletId) &&
                abs(it.amount - amount) <= CaptureRules.MAX_TRANSFER_FEE &&
                abs(it.occurredAt - at) <= CaptureRules.TRANSFER_WINDOW
        }
        .minByOrNull { abs(it.occurredAt - at) }

    /**
     * How to settle a transfer recorded from one side when the other side
     * reports a different amount. The transfer keeps the smaller (arrived)
     * amount and the difference becomes a fee paid from the source wallet, so
     * both wallets end up right.
     */
    fun reconcile(transferAmount: Long, reportedAmount: Long): Reconciliation {
        val arrived = minOf(transferAmount, reportedAmount)
        return Reconciliation(newAmount = arrived, fee = abs(transferAmount - reportedAmount))
    }

    /** True when a fee row for this transfer was already written. */
    fun hasFeeRow(rows: List<TransactionEntity>, fromWalletId: Long, fee: Long, at: Long): Boolean =
        rows.any {
            it.type == TransactionType.OUT &&
                it.walletId == fromWalletId &&
                it.amount == fee &&
                it.merchant == CaptureRules.FEE_LABEL &&
                abs(it.occurredAt - at) <= CaptureRules.TRANSFER_WINDOW
        }
}

data class Reconciliation(val newAmount: Long, val fee: Long)
