package com.example.okane.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Insert
    suspend fun insert(wallet: WalletEntity): Long

    @Insert
    suspend fun insertAll(wallets: List<WalletEntity>)

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM wallets WHERE isArchived = 0")
    suspend fun activeCount(): Int

    /** Pickers only ever show these. */
    @Query("SELECT * FROM wallets WHERE isArchived = 0 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<WalletEntity>>

    /** One-shot read of [observeActive], for the notification listener. */
    @Query("SELECT * FROM wallets WHERE isArchived = 0 ORDER BY sortOrder, name")
    suspend fun getActive(): List<WalletEntity>

    /** Management and historical reference resolution include archived rows. */
    @Query("SELECT * FROM wallets ORDER BY isArchived, sortOrder, name")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun findById(id: Long): WalletEntity?

    /**
     * Onboarding's "already have one with this name?" check. COLLATE NOCASE
     * because people type "gopay" for the wallet the list calls "GoPay".
     */
    @Query("SELECT * FROM wallets WHERE isArchived = 0 AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findActiveByName(name: String): WalletEntity?

    @Query("SELECT * FROM wallets WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
    suspend fun findDefault(): WalletEntity?

    @Query("SELECT * FROM wallets WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
    fun observeDefault(): Flow<WalletEntity?>

    /** Fallback when the default was archived and another must be promoted. */
    @Query("SELECT * FROM wallets WHERE isArchived = 0 ORDER BY sortOrder, name LIMIT 1")
    suspend fun findFirstActive(): WalletEntity?

    @Query("UPDATE wallets SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE wallets SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: Long)

    /** Prevent observers from seeing an intermediate state with no default. */
    @Transaction
    suspend fun replaceDefault(id: Long) {
        clearDefaultFlags()
        markDefault(id)
    }

    @Query("UPDATE wallets SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    /** Archive and default promotion emit as one database transaction. */
    @Transaction
    suspend fun archiveDefaultAndPromote(id: Long) {
        setArchived(id, true)
        findFirstActive()?.let { promoted ->
            clearDefaultFlags()
            markDefault(promoted.id)
        }
    }

    @Query("UPDATE wallets SET name = :name, initialBalance = :initialBalance, monthlyFee = :monthlyFee WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, initialBalance: Long, monthlyFee: Long)

    /**
     * Balance, computed — never stored. See [WalletEntity] for why.
     *
     *   initialBalance
     *     + IN      into this wallet
     *     - OUT     out of this wallet
     *     + TRANSFER arriving  (toWalletId = this)
     *     - TRANSFER leaving   (walletId   = this)
     *
     * Correlated subqueries rather than joins: a single LEFT JOIN over four
     * different match conditions would multiply rows and double-count. Room
     * still sees `transactions` inside the subqueries, so the Flow re-emits
     * when a transaction changes, not only when a wallet does.
     *
     * IFNULL wraps every SUM because SUM over no rows is NULL, not 0 — without
     * it a wallet with no transactions of some type would compute a NULL
     * balance and fail to map onto the non-null Long.
     */
    @Query(
        """
        SELECT
            w.id AS walletId,
            w.name AS name,
            w.isDefault AS isDefault,
            w.initialBalance AS initialBalance,
            w.monthlyFee AS monthlyFee,
            w.isArchived AS isArchived,
            w.initialBalance
              + IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.walletId = w.id AND t.type = 'IN'), 0)
              - IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.walletId = w.id AND t.type = 'OUT'), 0)
              + IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.toWalletId = w.id AND t.type = 'TRANSFER'), 0)
              - IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.walletId = w.id AND t.type = 'TRANSFER'), 0)
              AS balance
        FROM wallets w
        ORDER BY w.isArchived, w.sortOrder, w.name
        """
    )
    fun observeBalances(): Flow<List<WalletBalance>>

    /** Same arithmetic for one wallet; used by the calculator header. */
    @Query(
        """
        SELECT
            w.initialBalance
              + IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.walletId = w.id AND t.type = 'IN'), 0)
              - IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.walletId = w.id AND t.type = 'OUT'), 0)
              + IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.toWalletId = w.id AND t.type = 'TRANSFER'), 0)
              - IFNULL((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.walletId = w.id AND t.type = 'TRANSFER'), 0)
        FROM wallets w
        WHERE w.id = :walletId
        """
    )
    fun observeBalance(walletId: Long): Flow<Long?>
}
