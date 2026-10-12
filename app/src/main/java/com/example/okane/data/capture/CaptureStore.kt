package com.example.okane.data.capture

import com.example.okane.data.local.CaptureDao
import com.example.okane.data.local.CaptureSuggestionEntity
import com.example.okane.data.local.SuggestionStatus
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletDao
import com.example.okane.data.local.WalletEntity
import com.example.okane.data.repository.WalletRepository
import com.example.okane.domain.capture.CaptureMatching
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The storage [CaptureProcessor] needs, and nothing more. An interface so the
 * processor's rules run in plain JVM tests against an in-memory fake.
 */
interface CaptureStore {
    suspend fun activeWallets(): List<WalletEntity>

    /** The wallet to use when no wallet belongs to the source; created if needed. */
    suspend fun defaultWalletId(): Long

    /** Rows from either table that carry exactly this notification. */
    suspend fun countSameNotification(
        sourceApp: String,
        title: String?,
        text: String,
        from: Long,
        to: Long,
    ): Int

    suspend fun transactionsBetween(from: Long, to: Long): List<TransactionEntity>

    /** -1 when the unique index rejected it as a repeat. */
    suspend fun insertTransaction(row: TransactionEntity): Long

    suspend fun convertToTransfer(id: Long, fromWalletId: Long, toWalletId: Long, amount: Long): Boolean

    suspend fun setTransferAmount(id: Long, amount: Long): Boolean

    suspend fun insertSuggestion(row: CaptureSuggestionEntity): Long

    suspend fun suggestion(id: Long): CaptureSuggestionEntity?

    suspend fun pendingSuggestionsBetween(from: Long, to: Long): List<CaptureSuggestionEntity>

    suspend fun markSuggestionDuplicate(id: Long, transactionId: Long)

    /** Inserts [row] and marks the suggestion accepted, atomically. -1 on a repeat. */
    suspend fun acceptSuggestion(id: Long, row: TransactionEntity, at: Long): Long

    /** Marks a pending suggestion accepted or dismissed; false if it was not pending. */
    suspend fun resolveSuggestion(id: Long, status: SuggestionStatus, transactionId: Long?, at: Long): Boolean
}

@Singleton
class RoomCaptureStore @Inject constructor(
    private val dao: CaptureDao,
    private val walletDao: WalletDao,
    private val walletRepository: WalletRepository,
) : CaptureStore {

    override suspend fun activeWallets(): List<WalletEntity> = walletDao.getActive()

    override suspend fun defaultWalletId(): Long = walletRepository.ensureDefaultWallet()

    override suspend fun countSameNotification(
        sourceApp: String,
        title: String?,
        text: String,
        from: Long,
        to: Long,
    ): Int {
        val note = CaptureMatching.noteFor(title, text)
        return dao.countSameNote(sourceApp, note, from, to) +
            dao.countSameSuggestion(sourceApp, title, text, from, to)
    }

    override suspend fun transactionsBetween(from: Long, to: Long): List<TransactionEntity> =
        dao.transactionsBetween(from, to)

    override suspend fun insertTransaction(row: TransactionEntity): Long = dao.insertTransaction(row)

    override suspend fun convertToTransfer(id: Long, fromWalletId: Long, toWalletId: Long, amount: Long): Boolean =
        dao.convertToTransfer(id, fromWalletId, toWalletId, amount) > 0

    override suspend fun setTransferAmount(id: Long, amount: Long): Boolean =
        dao.setTransferAmount(id, amount) > 0

    override suspend fun insertSuggestion(row: CaptureSuggestionEntity): Long = dao.insertSuggestion(row)

    override suspend fun suggestion(id: Long): CaptureSuggestionEntity? = dao.findSuggestion(id)

    override suspend fun pendingSuggestionsBetween(from: Long, to: Long): List<CaptureSuggestionEntity> =
        dao.pendingBetween(from, to)

    override suspend fun markSuggestionDuplicate(id: Long, transactionId: Long) =
        dao.markDuplicateOf(id, transactionId)

    override suspend fun acceptSuggestion(id: Long, row: TransactionEntity, at: Long): Long =
        dao.recordSuggestion(id, row, at)

    override suspend fun resolveSuggestion(
        id: Long,
        status: SuggestionStatus,
        transactionId: Long?,
        at: Long,
    ): Boolean = dao.resolve(id, status.name, transactionId, at) > 0
}
