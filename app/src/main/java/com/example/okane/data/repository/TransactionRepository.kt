package com.example.okane.data.repository

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
 * Single access point for transaction data from the UI. The notification
 * listener writes through `data/capture/CaptureProcessor` instead, which owns
 * the duplicate and transfer rules for that path.
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
