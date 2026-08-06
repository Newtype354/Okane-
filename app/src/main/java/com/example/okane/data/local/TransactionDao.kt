package com.example.okane.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The only allowed ways to touch the transactions table. Room generates the
 * implementation and validates every SQL string against the entity at build
 * time, so a typo is a compile error rather than a runtime crash.
 */
@Dao
interface TransactionDao {

    /**
     * Manual (keypad) inserts. Plain insert, no IGNORE: the unique index
     * cannot fire for these because `sourceApp` is null and SQLite treats
     * NULLs as distinct — so a user logging the same amount twice is stored
     * twice, which is correct. Double-taps are handled in the UI layer.
     */
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    /**
     * Notification inserts. Returns -1 when the unique index rejected the row
     * as a repeat delivery of a notification we already stored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(transaction: TransactionEntity): Long

    /** Undo. Returns the number of rows removed (0 if it was already gone). */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    /** Newest first. Used by the History tab. */
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /** Newest first, capped — for the home screen's recent list. */
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    /**
     * The home screen inbox: committed without a category, tidy up later.
     * Transfers are excluded — they have no category by definition, so they
     * would otherwise sit in the inbox forever with nothing to fix.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE categoryId IS NULL AND type != 'TRANSFER'
        ORDER BY occurredAt DESC
        """
    )
    fun observeUncategorised(): Flow<List<TransactionEntity>>

    /**
     * Current-month totals grouped by direction.
     *
     * TRANSFER is excluded: moving money between the user's own wallets is
     * net-zero, and counting it would inflate both monthly figures for money
     * that never left the user's control.
     *
     * The range is half-open so a transaction at exactly midnight on the 1st
     * belongs to the new month only and cannot be counted twice.
     */
    @Query(
        """
        SELECT type AS type, SUM(amount) AS total
        FROM transactions
        WHERE occurredAt >= :startInclusive AND occurredAt < :endExclusive
          AND type != 'TRANSFER'
        GROUP BY type
        """
    )
    fun observeTotalsBetween(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<TypeTotal>>

    /** Assign or clear a category — the inbox's tidy-up action. */
    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :id")
    suspend fun setCategory(id: Long, categoryId: Long?): Int

    /** Used by the migration guard and by archive checks. */
    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId OR toWalletId = :walletId")
    suspend fun countForWallet(walletId: Long): Int
}
