package com.example.okane.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.okane.domain.capture.CaptureChannel
import com.example.okane.domain.capture.MoneyDirection
import com.example.okane.domain.model.TransactionType

enum class SuggestionStatus { PENDING, ACCEPTED, DISMISSED }

/**
 * A notification that looks like a transaction but was not recorded on its
 * own: an unknown format, an unconfirmed package, an SMS or WhatsApp relay, or
 * a possible duplicate. The home screen lists the pending ones under
 * "Perlu konfirmasi"; nothing touches `transactions` until the user confirms.
 *
 * Resolved rows are kept rather than deleted. Their text is exactly the real
 * sample a new template is written from, and a dismissed one is a useful
 * "this is not a transaction" example.
 *
 * [type], [walletId] and [toWalletId] are the proposal; the user can change
 * all three before confirming. [direction] is what the text itself said,
 * seen from [sourceApp]'s wallet — it tells which half of a transfer this was.
 */
@Entity(
    tableName = "capture_suggestions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["sourceApp", "postedAt"]),
    ]
)
data class CaptureSuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** One of `Sources`, e.g. "Livin". */
    val sourceApp: String,

    val packageName: String,

    val channel: CaptureChannel,

    val title: String?,

    val text: String,

    /** Whole rupiah, always positive. */
    val amount: Long,

    val direction: MoneyDirection?,

    /** Proposed type; null when the words did not say which way money went. */
    val type: TransactionType?,

    val walletId: Long?,

    /** Proposed destination; set only when [type] is TRANSFER. */
    val toWalletId: Long?,

    val counterparty: String?,

    /** When the notification was posted; becomes the transaction's occurredAt. */
    val postedAt: Long,

    val createdAt: Long,

    val status: SuggestionStatus = SuggestionStatus.PENDING,

    /** The row it became, once accepted. */
    val transactionId: Long? = null,

    /** A recorded row this may repeat; the review sheet warns about it. */
    val duplicateOfId: Long? = null,

    val resolvedAt: Long? = null,
)
