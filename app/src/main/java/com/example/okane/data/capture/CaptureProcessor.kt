package com.example.okane.data.capture

import com.example.okane.data.local.CaptureSuggestionEntity
import com.example.okane.data.local.SuggestionStatus
import com.example.okane.data.local.TransactionEntity
import com.example.okane.domain.capture.CaptureDecision
import com.example.okane.domain.capture.CaptureDraft
import com.example.okane.domain.capture.CaptureEngine
import com.example.okane.domain.capture.CaptureInput
import com.example.okane.domain.capture.CaptureMatching
import com.example.okane.domain.capture.CaptureRules
import com.example.okane.domain.capture.IgnoreReason
import com.example.okane.domain.capture.MoneyDirection
import com.example.okane.domain.capture.SourceCatalog
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The per-source switches in Settings. `SourcePreferences` implements it. */
interface SourceToggles {
    fun isEnabled(source: String): Boolean
}

sealed interface CaptureOutcome {
    data class Ignored(val reason: IgnoreReason) : CaptureOutcome

    /** The user switched this source off in Settings. */
    data object Disabled : CaptureOutcome

    /** The same notification was already handled. */
    data object Repeat : CaptureOutcome

    /** [merged] when it completed a row that already existed (a transfer's other half). */
    data class Recorded(val transactionId: Long, val merged: Boolean) : CaptureOutcome

    data class Suggested(val suggestionId: Long) : CaptureOutcome
}

/** What the user settled on in the review sheet. */
data class Confirmation(
    val type: TransactionType,
    val amount: Long,
    val walletId: Long,
    val toWalletId: Long? = null,
)

sealed interface AcceptResult {
    data class Recorded(val transactionId: Long) : AcceptResult

    /** Already accepted or dismissed, or gone. */
    data object NotPending : AcceptResult

    data class Invalid(val message: String) : AcceptResult
}

/**
 * Turns notifications into transactions or suggestions, and suggestions into
 * transactions once confirmed.
 *
 * Three protections sit here, on top of the engine's "only confirmed
 * templates record by themselves":
 *
 * 1. **Repeats.** The same notification re-posted within ten minutes (an
 *    update, a reboot, the backfill on reconnect) is dropped.
 * 2. **Duplicates across sources.** Money that may already be recorded — same
 *    wallet, direction and amount within half an hour, including hand-typed
 *    rows — is never recorded automatically; it becomes a suggestion that
 *    names the row it may repeat.
 * 3. **Transfers between own wallets.** A top-up names the other side
 *    ("via Mandiri"). When the user keeps wallets for both, it is recorded as
 *    one TRANSFER, and the other app's notification of the same move is folded
 *    into it rather than counted again. A difference of up to Rp6.500 between
 *    the two sides is the fee, recorded as its own expense.
 *
 * Everything runs under one lock: notifications arrive concurrently, and two
 * halves of a transfer often land within the same second.
 */
class CaptureProcessor(
    private val store: CaptureStore,
    private val toggles: SourceToggles,
    private val engine: CaptureEngine = CaptureEngine(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun process(input: CaptureInput): CaptureOutcome = mutex.withLock {
        val identified = SourceCatalog.identify(input.packageName, input.title)
            ?: return@withLock CaptureOutcome.Ignored(IgnoreReason.NOT_A_SOURCE)
        val source = identified.source
        if (!toggles.isEnabled(source.name)) return@withLock CaptureOutcome.Disabled

        val decision = engine.evaluate(input, identified)
        val draft = when (decision) {
            is CaptureDecision.Ignore -> return@withLock CaptureOutcome.Ignored(decision.reason)
            is CaptureDecision.Record -> decision.draft
            is CaptureDecision.Suggest -> decision.draft
        }
        val text = input.text.orEmpty()
        val at = input.postedAt
        val window = CaptureRules.SAME_NOTIFICATION_WINDOW
        if (store.countSameNotification(source.name, input.title, text, at - window, at + window) > 0) {
            return@withLock CaptureOutcome.Repeat
        }

        val auto = decision is CaptureDecision.Record
        val notification = Notification(input.packageName, input.title, text, at, draft)
        val wallets = store.activeWallets()
        val own = CaptureMatching.walletFor(source, wallets)
        val other = draft.counterpartSource
            ?.let { CaptureMatching.walletFor(it, wallets) }
            ?.takeIf { it.id != own?.id }
        val rows = nearby(at)

        val direction = draft.direction
        if (draft.movement && direction != null && own != null && other != null) {
            val sending = direction == MoneyDirection.OUT
            val plan = TransferPlan(
                fromWalletId = if (sending) own.id else other.id,
                toWalletId = if (sending) other.id else own.id,
                amount = draft.amount,
                sending = sending,
            )
            val completes = CaptureMatching.coveringTransfer(rows, plan.fromWalletId, plan.toWalletId, plan.amount, at)
                ?: CaptureMatching.otherHalf(
                    rows, plan.fromWalletId, plan.toWalletId, plan.amount, plan.sending, source.name, at,
                )
            // Something in the way that must not be rewritten: a hand-typed
            // entry for this top-up, or one the user already categorised.
            val conflict = if (completes != null) null else
                CaptureMatching.otherHalf(
                    rows, plan.fromWalletId, plan.toWalletId, plan.amount, plan.sending, source.name, at,
                    mergeableOnly = false,
                ) ?: CaptureMatching.similar(rows, own.id, direction.toType(), plan.amount, at)
            if (!auto || conflict != null) {
                return@withLock suggest(
                    notification, TransactionType.TRANSFER, plan.fromWalletId, plan.toWalletId,
                    (completes ?: conflict)?.id,
                )
            }
            val recorded = recordTransfer(plan, notification, rows)
            if (recorded.transactionId <= 0) return@withLock CaptureOutcome.Repeat
            flagPendingTwins(recorded.transactionId, notification, transfer = true)
            return@withLock recorded
        }

        val walletId = own?.id ?: store.defaultWalletId()
        if (direction == null) {
            return@withLock suggest(notification, null, walletId, null, null)
        }
        val type = direction.toType()
        val twin = CaptureMatching.transferTouching(rows, walletId, direction, draft.amount, at)
            ?: CaptureMatching.similar(rows, walletId, type, draft.amount, at)
        if (!auto || twin != null) {
            return@withLock suggest(notification, type, walletId, null, twin?.id)
        }
        val id = store.insertTransaction(notification.row(type, walletId, null, draft.amount, clock()))
        if (id < 0) return@withLock CaptureOutcome.Repeat
        flagPendingTwins(id, notification, transfer = false)
        CaptureOutcome.Recorded(id, merged = false)
    }

    suspend fun accept(suggestionId: Long, confirmation: Confirmation): AcceptResult = mutex.withLock {
        val suggestion = store.suggestion(suggestionId)
        if (suggestion == null || suggestion.status != SuggestionStatus.PENDING) {
            return@withLock AcceptResult.NotPending
        }
        if (confirmation.amount <= 0) return@withLock AcceptResult.Invalid("Nominal harus lebih dari nol.")
        val notification = Notification(
            packageName = suggestion.packageName,
            title = suggestion.title,
            text = suggestion.text,
            postedAt = suggestion.postedAt,
            draft = null,
            sourceApp = suggestion.sourceApp,
            merchant = suggestion.counterparty,
        )
        val now = clock()

        if (confirmation.type == TransactionType.TRANSFER) {
            val to = confirmation.toWalletId
            if (to == null || to == confirmation.walletId) {
                return@withLock AcceptResult.Invalid("Pilih dua dompet yang berbeda.")
            }
            val sending = when (suggestion.direction) {
                MoneyDirection.OUT -> true
                MoneyDirection.IN -> false
                // The text did not say; the side whose wallet is the source app's own.
                null -> SourceCatalog.byName(suggestion.sourceApp)
                    ?.let { CaptureMatching.walletFor(it, store.activeWallets()) }
                    ?.let { it.id == confirmation.walletId } ?: true
            }
            val plan = TransferPlan(confirmation.walletId, to, confirmation.amount, sending)
            val recorded = recordTransfer(plan, notification, nearby(suggestion.postedAt), suggestionId)
            if (recorded.transactionId <= 0) {
                return@withLock AcceptResult.Invalid("Transfer ini sudah tercatat.")
            }
            // A fresh row was written together with the suggestion's status.
            if (!recorded.merged) return@withLock AcceptResult.Recorded(recorded.transactionId)
            store.resolveSuggestion(suggestionId, SuggestionStatus.ACCEPTED, recorded.transactionId, now)
            return@withLock AcceptResult.Recorded(recorded.transactionId)
        }

        val row = notification.row(confirmation.type, confirmation.walletId, null, confirmation.amount, now)
        val id = store.acceptSuggestion(suggestionId, row, now)
        if (id > 0) return@withLock AcceptResult.Recorded(id)

        // The unique index says this exact row exists; point at it.
        val existing = nearby(suggestion.postedAt).firstOrNull {
            it.sourceApp == row.sourceApp && it.type == row.type &&
                it.amount == row.amount && it.occurredAt == row.occurredAt
        } ?: return@withLock AcceptResult.Invalid("Transaksi ini sudah tercatat.")
        store.resolveSuggestion(suggestionId, SuggestionStatus.ACCEPTED, existing.id, now)
        AcceptResult.Recorded(existing.id)
    }

    suspend fun dismiss(suggestionId: Long): Boolean = mutex.withLock {
        store.resolveSuggestion(suggestionId, SuggestionStatus.DISMISSED, null, clock())
    }

    /**
     * Records a transfer, or completes the one already there.
     *
     * - The same transfer recorded from the other side: keep it, settle the fee.
     * - The other side recorded as plain income/spending: turn that row into
     *   the transfer, settle the fee.
     * - Neither: insert the transfer (as part of accepting [suggestionId], if any).
     */
    private suspend fun recordTransfer(
        plan: TransferPlan,
        notification: Notification,
        rows: List<TransactionEntity>,
        suggestionId: Long? = null,
    ): CaptureOutcome.Recorded {
        val at = notification.postedAt
        val now = clock()
        CaptureMatching.coveringTransfer(rows, plan.fromWalletId, plan.toWalletId, plan.amount, at)?.let { existing ->
            settleFee(existing, plan, notification, rows)
            return CaptureOutcome.Recorded(existing.id, merged = true)
        }
        CaptureMatching.otherHalf(
            rows, plan.fromWalletId, plan.toWalletId, plan.amount, plan.sending, notification.sourceApp, at,
        )?.let { half ->
            val arrived = minOf(half.amount, plan.amount)
            if (store.convertToTransfer(half.id, plan.fromWalletId, plan.toWalletId, arrived)) {
                val fee = abs(half.amount - plan.amount)
                writeFee(plan.fromWalletId, fee, notification, rows)
                return CaptureOutcome.Recorded(half.id, merged = true)
            }
        }
        val row = notification.row(TransactionType.TRANSFER, plan.fromWalletId, plan.toWalletId, plan.amount, now)
        val id = if (suggestionId != null) {
            store.acceptSuggestion(suggestionId, row, now)
        } else {
            store.insertTransaction(row)
        }
        return CaptureOutcome.Recorded(id, merged = false)
    }

    /**
     * The two sides of one transfer report different amounts by the fee: the
     * paying side the larger, the receiving side what arrived. The transfer
     * keeps what arrived and the difference is spent from the paying wallet.
     */
    private suspend fun settleFee(
        transfer: TransactionEntity,
        plan: TransferPlan,
        notification: Notification,
        rows: List<TransactionEntity>,
    ) {
        val reported = plan.amount
        when {
            // Recorded from the paying side; this is what arrived.
            !plan.sending && reported < transfer.amount -> {
                if (store.setTransferAmount(transfer.id, reported)) {
                    writeFee(transfer.walletId, transfer.amount - reported, notification, rows)
                }
            }
            // Recorded from the receiving side; this is what was paid.
            plan.sending && reported > transfer.amount ->
                writeFee(transfer.walletId, reported - transfer.amount, notification, rows)
        }
    }

    private suspend fun writeFee(
        walletId: Long,
        fee: Long,
        notification: Notification,
        rows: List<TransactionEntity>,
    ) {
        if (fee <= 0 || fee > CaptureRules.MAX_TRANSFER_FEE) return
        if (CaptureMatching.hasFeeRow(rows, walletId, fee, notification.postedAt)) return
        store.insertTransaction(
            notification.row(TransactionType.OUT, walletId, null, fee, clock())
                .copy(merchant = CaptureRules.FEE_LABEL)
        )
    }

    /**
     * After recording automatically, pending suggestions from another app
     * that look like the same money get a duplicate warning, so confirming
     * them is a deliberate choice.
     */
    private suspend fun flagPendingTwins(transactionId: Long, notification: Notification, transfer: Boolean) {
        if (transactionId <= 0) return
        val draft = notification.draft ?: return
        val at = notification.postedAt
        val window = if (transfer) CaptureRules.TRANSFER_WINDOW else CaptureRules.SIMILAR_WINDOW
        store.pendingSuggestionsBetween(at - window, at + window)
            .filter { it.sourceApp != notification.sourceApp && it.duplicateOfId == null }
            .filter {
                if (transfer) {
                    abs(it.amount - draft.amount) <= CaptureRules.MAX_TRANSFER_FEE
                } else {
                    it.amount == draft.amount && it.direction == draft.direction
                }
            }
            .forEach { store.markSuggestionDuplicate(it.id, transactionId) }
    }

    private suspend fun suggest(
        notification: Notification,
        type: TransactionType?,
        walletId: Long?,
        toWalletId: Long?,
        duplicateOfId: Long?,
    ): CaptureOutcome.Suggested {
        val draft = requireNotNull(notification.draft)
        val id = store.insertSuggestion(
            CaptureSuggestionEntity(
                sourceApp = draft.source.name,
                packageName = notification.packageName,
                channel = draft.channel,
                title = notification.title,
                text = notification.text,
                amount = draft.amount,
                direction = draft.direction,
                type = type,
                walletId = walletId,
                toWalletId = toWalletId,
                counterparty = draft.label ?: draft.counterparty,
                postedAt = notification.postedAt,
                createdAt = clock(),
                duplicateOfId = duplicateOfId,
            )
        )
        return CaptureOutcome.Suggested(id)
    }

    private fun MoneyDirection.toType(): TransactionType =
        if (this == MoneyDirection.IN) TransactionType.IN else TransactionType.OUT

    private suspend fun nearby(at: Long): List<TransactionEntity> =
        store.transactionsBetween(at - CaptureRules.SIMILAR_WINDOW, at + CaptureRules.SIMILAR_WINDOW)

    private data class TransferPlan(
        val fromWalletId: Long,
        val toWalletId: Long,
        val amount: Long,
        /** This notification is the paying side. */
        val sending: Boolean,
    )

    /** One notification, from the listener or from a stored suggestion. */
    private class Notification(
        val packageName: String,
        val title: String?,
        val text: String,
        val postedAt: Long,
        val draft: CaptureDraft?,
        val sourceApp: String = requireNotNull(draft).source.name,
        val merchant: String? = draft?.label ?: draft?.counterparty,
    ) {
        fun row(type: TransactionType, walletId: Long, toWalletId: Long?, amount: Long, now: Long) =
            TransactionEntity(
                amount = amount,
                type = type,
                walletId = walletId,
                toWalletId = toWalletId,
                categoryId = null,
                occurredAt = postedAt,
                createdAt = now,
                source = TransactionSource.NOTIFICATION,
                note = CaptureMatching.noteFor(title, text),
                sourceApp = sourceApp,
                merchant = merchant,
            )
    }
}
