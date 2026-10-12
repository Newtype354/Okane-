package com.example.okane.ui.capture

import com.example.okane.data.local.CaptureSuggestionEntity
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.capture.CaptureChannel
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.format.Formatters
import java.time.Instant
import java.time.ZoneId

/** One notification waiting in "Perlu konfirmasi". */
data class SuggestionCard(
    val id: Long,
    val sourceApp: String,
    /** "SMS", "WhatsApp" or "Email" when relayed; null from the app itself. */
    val channelLabel: String?,
    val title: String?,
    val text: String,
    val amount: Long,
    val counterparty: String?,
    val timeLabel: String,
    val proposedType: TransactionType?,
    val proposedWalletId: Long?,
    val proposedToWalletId: Long?,
    /** Why this may already be recorded; shown above the buttons. */
    val duplicateNote: String?,
)

data class WalletChoice(val id: Long, val name: String)

/** The choices the user has made for the card under review. */
data class ReviewSelection(
    val type: TransactionType?,
    val walletId: Long?,
    val toWalletId: Long?,
) {
    val complete: Boolean
        get() = type != null && walletId != null &&
            (type != TransactionType.TRANSFER || (toWalletId != null && toWalletId != walletId))
}

data class CaptureInboxUiState(
    val cards: List<SuggestionCard> = emptyList(),
    val wallets: List<WalletChoice> = emptyList(),
    val sheetOpen: Boolean = false,
    /** Selection for the first card; the sheet reviews one card at a time. */
    val selection: ReviewSelection? = null,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val current: SuggestionCard? get() = cards.firstOrNull()
    val pendingCount: Int get() = cards.size
}

/** Everything the inbox can do, so the composables stay stateless. */
data class CaptureInboxActions(
    val onOpen: () -> Unit,
    val onClose: () -> Unit,
    val onSelectType: (TransactionType) -> Unit,
    val onSelectWallet: (Long) -> Unit,
    val onSelectToWallet: (Long) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
) {
    companion object {
        val None = CaptureInboxActions({}, {}, {}, {}, {}, {}, {})
    }
}

/** Pure mapping from stored rows to what the inbox shows. */
object CaptureInboxPresenter {

    fun cards(
        suggestions: List<CaptureSuggestionEntity>,
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>,
        now: Instant,
        zone: ZoneId,
    ): List<SuggestionCard> {
        val names = wallets.associate { it.id to it.name }
        val byId = transactions.associateBy { it.id }
        val today = now.atZone(zone).toLocalDate()
        return suggestions
            .sortedByDescending { it.postedAt }
            .map { s ->
                val at = Instant.ofEpochMilli(s.postedAt).atZone(zone)
                SuggestionCard(
                    id = s.id,
                    sourceApp = s.sourceApp,
                    channelLabel = when (s.channel) {
                        CaptureChannel.APP -> null
                        CaptureChannel.SMS -> "SMS"
                        CaptureChannel.WHATSAPP -> "WhatsApp"
                        CaptureChannel.EMAIL -> "Email"
                    },
                    title = s.title,
                    text = s.text,
                    amount = s.amount,
                    counterparty = s.counterparty,
                    timeLabel = Formatters.historyDateLabel(at.toLocalDate(), today) +
                        " · " + Formatters.time(s.postedAt, zone),
                    proposedType = s.type,
                    proposedWalletId = s.walletId?.takeIf { it in names },
                    proposedToWalletId = s.toWalletId?.takeIf { it in names },
                    duplicateNote = s.duplicateOfId
                        ?.let(byId::get)
                        ?.let { duplicateNote(it, s.type, s.amount, names, zone) },
                )
            }
    }

    fun choices(wallets: List<WalletEntity>): List<WalletChoice> = wallets
        .filter { !it.isArchived }
        .sortedWith(compareBy({ it.sortOrder }, { it.name }))
        .map { WalletChoice(it.id, it.name) }

    /** The proposal, with the default wallet filled in when none was guessed. */
    fun initialSelection(card: SuggestionCard, wallets: List<WalletEntity>): ReviewSelection {
        val default = wallets.firstOrNull { it.isDefault && !it.isArchived }?.id
            ?: choices(wallets).firstOrNull()?.id
        return ReviewSelection(
            type = card.proposedType,
            walletId = card.proposedWalletId ?: default,
            toWalletId = card.proposedToWalletId,
        )
    }

    private fun duplicateNote(
        row: TransactionEntity,
        proposed: TransactionType?,
        suggestedAmount: Long,
        names: Map<Long, String>,
        zone: ZoneId,
    ): String {
        val amount = Formatters.rupiah(row.amount)
        val wallet = names[row.walletId] ?: "dompet lain"
        val fee = suggestedAmount - row.amount
        return when {
            // The other half of a transfer already recorded: confirming
            // completes it (CaptureProcessor adds only the fee), never doubles it.
            row.type == TransactionType.TRANSFER && proposed == TransactionType.TRANSFER ->
                "Pindah saldo $wallet → ${names[row.toWalletId] ?: "dompet lain"} $amount sudah " +
                    "tercatat dari ${row.sourceApp ?: "notifikasi lain"}. " +
                    if (fee > 0) {
                        "Catat untuk melengkapinya: selisih ${Formatters.rupiah(fee)} dicatat sebagai biaya admin."
                    } else {
                        "Catat tidak akan menggandakannya."
                    }
            row.type == TransactionType.TRANSFER ->
                "Sudah tercatat sebagai pindah saldo $wallet → ${names[row.toWalletId] ?: "dompet lain"} " +
                    "$amount. Catat hanya jika ini transaksi lain."
            proposed == TransactionType.TRANSFER && row.source == TransactionSource.NOTIFICATION ->
                "Catatan ${row.sourceApp ?: "notifikasi"} $amount di $wallet akan digabung menjadi " +
                    "satu pindah saldo, jadi tidak terhitung dua kali."
            else -> {
                val kind = if (row.type == TransactionType.IN) "Masuk" else "Keluar"
                val origin = if (row.source == TransactionSource.MANUAL) "yang kamu catat sendiri" else "dari ${row.sourceApp}"
                "Mirip catatan $kind $amount di $wallet $origin pukul ${Formatters.time(row.occurredAt, zone)}. " +
                    "Catat hanya jika ini transaksi lain."
            }
        }
    }
}
