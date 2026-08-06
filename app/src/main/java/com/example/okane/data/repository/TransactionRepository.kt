package com.example.okane.data.repository

import com.example.okane.data.local.Transaction
import com.example.okane.data.local.TransactionDao
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.TypeTotal
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single access point for transaction data. Everything else in the app
 * (notification service, ViewModels) talks to this class, never to the DAO or
 * SQL directly — so storage details can change without touching them.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val walletRepository: WalletRepository,
) {
    /** Live stream of all transactions, newest first. */
    val transactions: Flow<List<TransactionEntity>> = dao.observeAll()

    /** Newest first, capped — the home screen's recent list. */
    fun recent(limit: Int = 20): Flow<List<TransactionEntity>> = dao.observeRecent(limit)

    /** Committed without a category; the home screen surfaces these as an inbox. */
    val uncategorised: Flow<List<TransactionEntity>> = dao.observeUncategorised()

    /**
     * Notification path. Takes the parser's own model and maps it to a row, so
     * OkaneNotificationListenerService and NotificationParser need no knowledge
     * of the storage schema and stay unchanged.
     *
     * Returns the new row id, or -1 when the unique index rejected it as a
     * repeat delivery of a notification already stored.
     */
    suspend fun add(transaction: Transaction): Long {
        // Land it in the wallet named after the app when one exists, so a
        // GoPay notification credits the GoPay wallet rather than the default.
        val walletId = walletRepository.resolveForNotification(transaction.source)
        return dao.insertIgnoringDuplicates(
            TransactionEntity(
                amount = transaction.amount,
                type = when (transaction.type) {
                    Transaction.TYPE_KREDIT -> TransactionType.IN
                    else -> TransactionType.OUT
                },
                walletId = walletId,
                toWalletId = null,
                categoryId = null,          // notifications carry no category
                occurredAt = transaction.timestamp,
                createdAt = transaction.createdAt,
                source = TransactionSource.NOTIFICATION,
                note = transaction.note,
                sourceApp = transaction.source,   // "GoPay", "Livin", …
                merchant = transaction.merchant,
            )
        )
    }

    /**
     * Keypad path for income and expense. Returns the new row id so the caller
     * can offer undo.
     *
     * [occurredAt] null means now — passed only when the entry is backdated.
     */
    suspend fun addManual(
        amount: Long,
        type: TransactionType,
        walletId: Long,
        categoryId: Long?,
        occurredAt: Long? = null,
        note: String? = null,
    ): Long {
        require(amount > 0) { "Refusing to store a zero or negative amount: $amount" }
        require(type != TransactionType.TRANSFER) { "Use addTransfer for transfers" }
        val now = System.currentTimeMillis()
        return dao.insert(
            TransactionEntity(
                amount = amount,
                type = type,
                walletId = walletId,
                toWalletId = null,
                categoryId = categoryId,
                occurredAt = occurredAt ?: now,
                createdAt = now,
                source = TransactionSource.MANUAL,
                note = note,
            )
        )
    }

    /**
     * Moves money between two of the user's own wallets: one row, not two.
     *
     * Recording it as a paired Keluar + Masuk would inflate both monthly
     * totals for money that never left the user's control, which is exactly
     * what the TRANSFER type exists to avoid. Category is always null.
     */
    suspend fun addTransfer(
        amount: Long,
        fromWalletId: Long,
        toWalletId: Long,
        occurredAt: Long? = null,
        note: String? = null,
    ): Long {
        require(amount > 0) { "Refusing to store a zero or negative amount: $amount" }
        require(fromWalletId != toWalletId) {
            "A transfer needs two different wallets (both were $fromWalletId)"
        }
        val now = System.currentTimeMillis()
        return dao.insert(
            TransactionEntity(
                amount = amount,
                type = TransactionType.TRANSFER,
                walletId = fromWalletId,
                toWalletId = toWalletId,
                categoryId = null,
                occurredAt = occurredAt ?: now,
                createdAt = now,
                source = TransactionSource.MANUAL,
                note = note,
            )
        )
    }

    /** Undo. True when a row was actually removed. */
    suspend fun delete(id: Long): Boolean = dao.deleteById(id) > 0

    /** Inbox tidy-up: assign (or clear, with null) a category. */
    suspend fun setCategory(id: Long, categoryId: Long?): Boolean =
        dao.setCategory(id, categoryId) > 0

    /**
     * Totals for one calendar month, grouped by direction, transfers excluded.
     * The range is half-open, so a transaction at exactly midnight on the 1st
     * belongs to the new month only.
     */
    fun totalsForMonth(
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<TypeTotal>> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeTotalsBetween(start, end)
    }
}
