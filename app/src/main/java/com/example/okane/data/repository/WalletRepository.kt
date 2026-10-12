package com.example.okane.data.repository

import com.example.okane.data.local.DefaultWallets
import com.example.okane.data.local.WalletBalance
import com.example.okane.data.local.WalletDao
import com.example.okane.data.local.WalletEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of an archive attempt, so the UI can explain a refusal. */
enum class ArchiveResult { ARCHIVED, REFUSED_LAST_WALLET, NOT_FOUND }

@Singleton
class WalletRepository @Inject constructor(
    private val dao: WalletDao
) {
    /** Everything a picker should offer. */
    val active: Flow<List<WalletEntity>> = dao.observeActive()

    /** Includes archived, for a management screen that can restore them. */
    val all: Flow<List<WalletEntity>> = dao.observeAll()

    /** Computed for active and archived wallets, never stored — see WalletEntity. */
    val balances: Flow<List<WalletBalance>> = dao.observeBalances()

    val default: Flow<WalletEntity?> = dao.observeDefault()

    fun balance(walletId: Long): Flow<Long?> = dao.observeBalance(walletId)

    /**
     * The very first wallet is always the default, whatever the caller asks
     * for — "exactly one default" cannot be satisfied any other way, and
     * onboarding creating several in sequence gets the brief's rule (the first
     * one wins) for free.
     */
    suspend fun create(
        name: String,
        initialBalance: Long = 0,
        monthlyFee: Long = 0,
        makeDefault: Boolean = false,
    ): Long {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Nama dompet wajib diisi." }
        require(initialBalance >= 0) { "Saldo awal tidak boleh negatif." }
        require(monthlyFee >= 0) { "Biaya admin bulanan tidak boleh negatif." }
        val existing = dao.count()
        val shouldBeDefault = makeDefault || dao.activeCount() == 0
        val id = dao.insert(
            WalletEntity(
                name = trimmedName,
                initialBalance = initialBalance,
                monthlyFee = monthlyFee,
                isDefault = shouldBeDefault && existing == 0,
                sortOrder = existing,
            )
        )
        if (shouldBeDefault && existing > 0) dao.replaceDefault(id)
        return id
    }

    /**
     * Onboarding's create: a no-op when a wallet with this name already
     * exists. Needed because onboarding also runs for users upgrading from an
     * older version, who already have the `Tunai` that MIGRATION_3_4 created —
     * without this, ticking `Tunai` on the checklist would produce a duplicate.
     *
     * Deliberately NOT the behaviour of [create]: someone who deliberately
     * adds a second wallet with a repeated name later should get one.
     */
    suspend fun createIfAbsent(
        name: String,
        initialBalance: Long = 0,
        monthlyFee: Long = 0,
    ): Long {
        val trimmed = name.trim()
        dao.findActiveByName(trimmed)?.let { return it.id }
        return create(trimmed, initialBalance, monthlyFee)
    }

    suspend fun updateDetails(
        id: Long,
        name: String,
        initialBalance: Long,
        monthlyFee: Long,
    ): Boolean {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Nama dompet wajib diisi." }
        require(initialBalance >= 0) { "Saldo awal tidak boleh negatif." }
        require(monthlyFee >= 0) { "Biaya admin bulanan tidak boleh negatif." }
        if (dao.findById(id) == null) return false
        dao.updateDetails(id, trimmedName, initialBalance, monthlyFee)
        return true
    }

    suspend fun setDefault(id: Long): Boolean {
        val wallet = dao.findById(id) ?: return false
        if (wallet.isArchived) return false
        dao.replaceDefault(id)
        return true
    }

    /**
     * Archive, never delete: deleting would orphan every historical
     * transaction referencing this wallet, and a slightly cluttered list beats
     * corrupted history.
     *
     * Two invariants enforced here: the last active wallet cannot be archived
     * (transactions.walletId is NOT NULL, so zero wallets means nothing can be
     * recorded), and archiving the default promotes another one.
     */
    suspend fun archive(id: Long): ArchiveResult {
        val wallet = dao.findById(id) ?: return ArchiveResult.NOT_FOUND
        if (wallet.isArchived) return ArchiveResult.ARCHIVED
        if (dao.activeCount() <= 1) return ArchiveResult.REFUSED_LAST_WALLET

        if (wallet.isDefault) {
            dao.archiveDefaultAndPromote(id)
        } else {
            dao.setArchived(id, true)
        }
        return ArchiveResult.ARCHIVED
    }

    suspend fun unarchive(id: Long): Boolean {
        val wallet = dao.findById(id) ?: return false
        if (!wallet.isArchived) return true
        dao.setArchived(id, false)
        // Restoring into an app that somehow has no default fixes it.
        if (dao.findDefault() == null) setDefault(id)
        return true
    }

    /**
     * Guarantees the app can always record a transaction, and returns the id
     * to record it against. Safe to call repeatedly.
     *
     * This is the skip-everything path's safety net: a user who skips all of
     * onboarding still ends up with a working `Tunai` wallet.
     */
    suspend fun ensureDefaultWallet(): Long {
        dao.findDefault()?.let { return it.id }

        // Wallets exist but none is flagged default (e.g. the default was
        // archived by an older build) — promote rather than create.
        dao.findFirstActive()?.let { existing ->
            setDefault(existing.id)
            return existing.id
        }

        return create(DefaultWallets.FALLBACK_NAME, makeDefault = true)
    }
}
