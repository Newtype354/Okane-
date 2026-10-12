package com.example.okane.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okane.data.capture.AcceptResult
import com.example.okane.data.capture.CaptureRepository
import com.example.okane.data.capture.Confirmation
import com.example.okane.data.local.WalletEntity
import com.example.okane.data.repository.TransactionRepository
import com.example.okane.data.repository.WalletRepository
import com.example.okane.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The "Perlu konfirmasi" inbox: review one captured notification at a time. */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repository: CaptureRepository,
    walletRepository: WalletRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val sheetOpen = MutableStateFlow(false)
    /** Edits per suggestion id, so a half-reviewed card keeps its choices. */
    private val edits = MutableStateFlow<Map<Long, ReviewSelection>>(emptyMap())
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val rows = combine(
        repository.pending,
        walletRepository.all,
        transactionRepository.transactions,
    ) { suggestions, wallets, transactions ->
        val cards = CaptureInboxPresenter.cards(
            suggestions, wallets, transactions, Instant.now(), ZoneId.systemDefault(),
        )
        Rows(cards, wallets)
    }

    private val ui = combine(sheetOpen, edits, busy, message) { open, edited, isBusy, msg ->
        Ui(open, edited, isBusy, msg)
    }

    val uiState: StateFlow<CaptureInboxUiState> = combine(rows, ui) { data, state ->
        val current = data.cards.firstOrNull()
        CaptureInboxUiState(
            cards = data.cards,
            wallets = CaptureInboxPresenter.choices(data.wallets),
            // Closes by itself once the last card is handled.
            sheetOpen = state.open && current != null,
            selection = current?.let {
                state.edits[it.id] ?: CaptureInboxPresenter.initialSelection(it, data.wallets)
            },
            busy = state.busy,
            message = state.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureInboxUiState())

    val actions = CaptureInboxActions(
        onOpen = { message.value = null; sheetOpen.value = true },
        onClose = { sheetOpen.value = false },
        onSelectType = { type -> edit { it.copy(type = type) } },
        onSelectWallet = { id -> edit { it.copy(walletId = id) } },
        onSelectToWallet = { id -> edit { it.copy(toWalletId = id) } },
        onConfirm = ::confirm,
        onDismiss = ::dismissCurrent,
    )

    private fun edit(change: (ReviewSelection) -> ReviewSelection) {
        val state = uiState.value
        val card = state.current ?: return
        val selection = state.selection ?: return
        message.value = null
        edits.update { it + (card.id to change(selection)) }
    }

    private fun confirm() {
        val state = uiState.value
        val card = state.current ?: return
        val selection = state.selection ?: return
        if (busy.value) return
        val type = selection.type
        val walletId = selection.walletId
        if (type == null || walletId == null || !selection.complete) {
            message.value = "Pilih jenis dan dompetnya dulu."
            return
        }
        write {
            val result = repository.accept(
                card.id,
                Confirmation(
                    type = type,
                    amount = card.amount,
                    walletId = walletId,
                    toWalletId = selection.toWalletId.takeIf { type == TransactionType.TRANSFER },
                ),
            )
            when (result) {
                is AcceptResult.Recorded, AcceptResult.NotPending -> edits.update { it - card.id }
                is AcceptResult.Invalid -> message.value = result.message
            }
        }
    }

    private fun dismissCurrent() {
        val card = uiState.value.current ?: return
        if (busy.value) return
        write {
            repository.dismiss(card.id)
            edits.update { it - card.id }
        }
    }

    private fun write(block: suspend () -> Unit) {
        busy.value = true
        message.value = null
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = "Gagal menyimpan. Coba lagi."
            } finally {
                busy.value = false
            }
        }
    }

    private data class Rows(
        val cards: List<SuggestionCard>,
        val wallets: List<WalletEntity>,
    )

    private data class Ui(
        val open: Boolean,
        val edits: Map<Long, ReviewSelection>,
        val busy: Boolean,
        val message: String?,
    )
}
