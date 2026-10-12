package com.example.okane.data.capture

import com.example.okane.data.local.CaptureSuggestionEntity
import com.example.okane.data.local.SuggestionStatus
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.capture.CaptureMatching
import com.example.okane.domain.model.TransactionType

/** In-memory [CaptureStore] with the same semantics as the Room queries. */
class FakeCaptureStore(vararg wallets: WalletEntity) : CaptureStore {
    val wallets = wallets.toMutableList()
    val transactions = mutableListOf<TransactionEntity>()
    val suggestions = mutableListOf<CaptureSuggestionEntity>()
    private var nextTransactionId = 1L
    private var nextSuggestionId = 1L

    override suspend fun activeWallets() = wallets.filter { !it.isArchived }

    override suspend fun defaultWalletId(): Long = wallets.first { it.isDefault }.id

    override suspend fun countSameNotification(sourceApp: String, title: String?, text: String, from: Long, to: Long): Int {
        val note = CaptureMatching.noteFor(title, text)
        return transactions.count { it.sourceApp == sourceApp && it.note == note && it.occurredAt in from..to } +
            suggestions.count { it.sourceApp == sourceApp && it.title == title && it.text == text && it.postedAt in from..to }
    }

    override suspend fun transactionsBetween(from: Long, to: Long) = transactions.filter { it.occurredAt in from..to }

    override suspend fun insertTransaction(row: TransactionEntity): Long {
        // The unique index: (sourceApp, type, amount, occurredAt), NULL sourceApp never collides.
        val clash = row.sourceApp != null && transactions.any {
            it.sourceApp == row.sourceApp && it.type == row.type &&
                it.amount == row.amount && it.occurredAt == row.occurredAt
        }
        if (clash) return -1
        val id = nextTransactionId++
        transactions += row.copy(id = id)
        return id
    }

    override suspend fun convertToTransfer(id: Long, fromWalletId: Long, toWalletId: Long, amount: Long): Boolean =
        update(id, { it.source.name == "NOTIFICATION" }) {
            it.copy(type = TransactionType.TRANSFER, walletId = fromWalletId, toWalletId = toWalletId, amount = amount, categoryId = null)
        }

    override suspend fun setTransferAmount(id: Long, amount: Long): Boolean =
        update(id, { it.type == TransactionType.TRANSFER }) { it.copy(amount = amount) }

    override suspend fun insertSuggestion(row: CaptureSuggestionEntity): Long {
        val id = nextSuggestionId++
        suggestions += row.copy(id = id)
        return id
    }

    override suspend fun suggestion(id: Long) = suggestions.firstOrNull { it.id == id }

    override suspend fun pendingSuggestionsBetween(from: Long, to: Long) =
        suggestions.filter { it.status == SuggestionStatus.PENDING && it.postedAt in from..to }

    override suspend fun markSuggestionDuplicate(id: Long, transactionId: Long) {
        replaceSuggestion(id) { it.copy(duplicateOfId = transactionId) }
    }

    override suspend fun acceptSuggestion(id: Long, row: TransactionEntity, at: Long): Long {
        val inserted = insertTransaction(row)
        if (inserted > 0) resolveSuggestion(id, SuggestionStatus.ACCEPTED, inserted, at)
        return inserted
    }

    override suspend fun resolveSuggestion(id: Long, status: SuggestionStatus, transactionId: Long?, at: Long): Boolean {
        val current = suggestion(id) ?: return false
        if (current.status != SuggestionStatus.PENDING) return false
        replaceSuggestion(id) { it.copy(status = status, transactionId = transactionId, resolvedAt = at) }
        return true
    }

    /** Balance exactly as WalletDao.observeBalances computes it. */
    fun balance(walletId: Long): Long {
        val initial = wallets.first { it.id == walletId }.initialBalance
        return initial +
            transactions.filter { it.walletId == walletId && it.type == TransactionType.IN }.sumOf { it.amount } -
            transactions.filter { it.walletId == walletId && it.type == TransactionType.OUT }.sumOf { it.amount } +
            transactions.filter { it.toWalletId == walletId && it.type == TransactionType.TRANSFER }.sumOf { it.amount } -
            transactions.filter { it.walletId == walletId && it.type == TransactionType.TRANSFER }.sumOf { it.amount }
    }

    private fun update(id: Long, guard: (TransactionEntity) -> Boolean, change: (TransactionEntity) -> TransactionEntity): Boolean {
        val index = transactions.indexOfFirst { it.id == id && guard(it) }
        if (index < 0) return false
        transactions[index] = change(transactions[index])
        return true
    }

    private fun replaceSuggestion(id: Long, change: (CaptureSuggestionEntity) -> CaptureSuggestionEntity) {
        val index = suggestions.indexOfFirst { it.id == id }
        if (index >= 0) suggestions[index] = change(suggestions[index])
    }
}
