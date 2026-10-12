package com.example.okane.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Everything automatic capture reads and writes. One DAO across both tables
 * so that confirming a suggestion — insert the transaction, mark the
 * suggestion — happens in one database transaction.
 */
@Dao
interface CaptureDao {

    // --- suggestions -----------------------------------------------------

    @Insert
    suspend fun insertSuggestion(suggestion: CaptureSuggestionEntity): Long

    @Query("SELECT * FROM capture_suggestions WHERE id = :id")
    suspend fun findSuggestion(id: Long): CaptureSuggestionEntity?

    /** The "Perlu konfirmasi" inbox, newest first. */
    @Query("SELECT * FROM capture_suggestions WHERE status = 'PENDING' ORDER BY postedAt DESC")
    fun observePending(): Flow<List<CaptureSuggestionEntity>>

    @Query(
        """
        SELECT * FROM capture_suggestions
        WHERE status = 'PENDING' AND postedAt >= :startMillis AND postedAt <= :endMillis
        """
    )
    suspend fun pendingBetween(startMillis: Long, endMillis: Long): List<CaptureSuggestionEntity>

    /** `IS` rather than `=` so a missing title matches a missing title. */
    @Query(
        """
        SELECT COUNT(*) FROM capture_suggestions
        WHERE sourceApp = :sourceApp AND title IS :title AND text = :text
          AND postedAt >= :startMillis AND postedAt <= :endMillis
        """
    )
    suspend fun countSameSuggestion(
        sourceApp: String,
        title: String?,
        text: String,
        startMillis: Long,
        endMillis: Long,
    ): Int

    @Query("UPDATE capture_suggestions SET duplicateOfId = :transactionId WHERE id = :id")
    suspend fun markDuplicateOf(id: Long, transactionId: Long)

    /** [status] is a [SuggestionStatus] name; only a pending row changes. */
    @Query(
        """
        UPDATE capture_suggestions
        SET status = :status, transactionId = :transactionId, resolvedAt = :resolvedAt
        WHERE id = :id AND status = 'PENDING'
        """
    )
    suspend fun resolve(id: Long, status: String, transactionId: Long?, resolvedAt: Long): Int

    /** Newest first; the source of "Bagikan sampel". */
    @Query("SELECT * FROM capture_suggestions ORDER BY postedAt DESC LIMIT :limit")
    suspend fun recentSuggestions(limit: Int): List<CaptureSuggestionEntity>

    // --- transactions written by capture ---------------------------------

    /** Every row near a moment, for duplicate and transfer matching in Kotlin. */
    @Query("SELECT * FROM transactions WHERE occurredAt >= :startMillis AND occurredAt <= :endMillis")
    suspend fun transactionsBetween(startMillis: Long, endMillis: Long): List<TransactionEntity>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE sourceApp = :sourceApp AND note = :note
          AND occurredAt >= :startMillis AND occurredAt <= :endMillis
        """
    )
    suspend fun countSameNote(sourceApp: String, note: String, startMillis: Long, endMillis: Long): Int

    /** Returns -1 when the unique index rejected it as a repeat delivery. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    /**
     * Turns one half of a transfer, recorded earlier as income or spending,
     * into the transfer itself. Guarded to notification rows: a hand-typed
     * entry is never rewritten.
     */
    @Query(
        """
        UPDATE transactions
        SET type = 'TRANSFER', walletId = :fromWalletId, toWalletId = :toWalletId,
            amount = :amount, categoryId = NULL
        WHERE id = :id AND source = 'NOTIFICATION'
        """
    )
    suspend fun convertToTransfer(id: Long, fromWalletId: Long, toWalletId: Long, amount: Long): Int

    @Query(
        """
        UPDATE transactions SET amount = :amount
        WHERE id = :id AND type = 'TRANSFER' AND source = 'NOTIFICATION'
        """
    )
    suspend fun setTransferAmount(id: Long, amount: Long): Int

    /** Confirming a suggestion: both writes land, or neither does. */
    @Transaction
    suspend fun recordSuggestion(
        suggestionId: Long,
        transaction: TransactionEntity,
        resolvedAt: Long,
    ): Long {
        val id = insertTransaction(transaction)
        if (id > 0) resolve(suggestionId, SuggestionStatus.ACCEPTED.name, id, resolvedAt)
        return id
    }
}
