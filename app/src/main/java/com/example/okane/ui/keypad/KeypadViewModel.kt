package com.example.okane.ui.keypad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.data.repository.CategoryRepository
import com.example.okane.data.repository.TransactionRepository
import com.example.okane.data.repository.WalletRepository
import com.example.okane.domain.keypad.KeypadKey
import com.example.okane.domain.keypad.KeypadState
import com.example.okane.domain.keypad.categoryIdForCommit
import com.example.okane.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The calculator screen's state container. The other half of the same engine
 * is [KeypadState], which is Android-free so the deferred widget can persist
 * it to DataStore instead.
 *
 * Note what is NOT here: no digit handling, no leading-zero rule, no cap, no
 * transfer validity rule. All of that is [KeypadState]. This class only
 * decides where the result is stored and what happens on commit.
 */
@HiltViewModel
class KeypadViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(KeypadState())
    val state: StateFlow<KeypadState> = _state.asStateFlow()

    val wallets: StateFlow<List<WalletEntity>> =
        walletRepository.active
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Chips show the expense side by default.
     *
     * Direction is only known when a commit button is pressed, so the row has
     * to guess; expense is by far the more frequent side. A category the user
     * picked from the full list is appended when it is not already in the
     * most-used set, so an income category chosen deliberately stays visible
     * instead of silently vanishing from the row.
     */
    val chipCategories: StateFlow<List<CategoryEntity>> =
        combine(
            categoryRepository.chips(TransactionType.OUT),
            categoryRepository.all,
            _state,
        ) { mostUsed, all, current ->
            val selected = current.categoryId?.let { id -> all.firstOrNull { it.id == id } }
            if (selected == null || mostUsed.any { it.id == selected.id }) mostUsed
            else mostUsed + selected
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Full list behind the "Semua" affordance, both sides, archived hidden. */
    val allCategories: StateFlow<List<CategoryEntity>> =
        combine(
            categoryRepository.activeByType(TransactionType.OUT),
            categoryRepository.activeByType(TransactionType.IN),
        ) { expense, income -> expense + income }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pendingUndo = MutableStateFlow<UndoEvent?>(null)
    val pendingUndo: StateFlow<UndoEvent?> = _pendingUndo.asStateFlow()

    /**
     * Double-tap guard. The commit buttons are large targets under the thumb
     * and will be pressed twice. Clearing the amount on success also blocks a
     * second commit, but only once the insert has returned — this covers the
     * window before that.
     *
     * A plain Boolean is enough: ViewModel methods are called from the main
     * thread and viewModelScope is main-dispatched, so the check-and-set and
     * the reset never interleave.
     */
    private var committing = false

    init {
        // Guarantees a wallet exists even if the user skipped all onboarding,
        // and preselects it so the keypad is usable on first open.
        viewModelScope.launch {
            val defaultId = walletRepository.ensureDefaultWallet()
            _state.update { if (it.walletId == null) it.selectWallet(defaultId) else it }
        }
    }

    fun press(key: KeypadKey) = _state.update { it.press(key) }

    fun selectWallet(walletId: Long) = _state.update { it.selectWallet(walletId) }

    fun selectToWallet(walletId: Long) = _state.update { it.selectToWallet(walletId) }

    fun toggleCategory(categoryId: Long) = _state.update { it.toggleCategory(categoryId) }

    /** null restores "Hari ini". */
    fun setOccurredAt(epochMillis: Long?) = _state.update { it.withOccurredAt(epochMillis) }

    fun setTransfer(enabled: Boolean) = _state.update { it.setTransfer(enabled) }

    /**
     * Creates a wallet from the picker and selects it immediately.
     *
     * Onboarding runs once, so without this the wallet list is frozen for the
     * life of the install. Uses createIfAbsent, so typing a name that already
     * exists selects that wallet rather than making a confusing twin.
     */
    fun createWallet(name: String, asDestination: Boolean = false) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = walletRepository.createIfAbsent(trimmed)
            _state.update { if (asDestination) it.selectToWallet(id) else it.selectWallet(id) }
        }
    }

    /**
     * Creates a category from the picker and selects it immediately.
     *
     * This is the recovery path for a user who unticked every category during
     * onboarding: without it, an empty category list could never be refilled.
     * The side comes from which group's add row was used, so a category is
     * never created without a direction.
     */
    fun createCategory(name: String, type: TransactionType) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = categoryRepository.create(trimmed, type)
            _state.update { it.copy(categoryId = id) }
        }
    }

    /** Keluar / Masuk. Transfers go through [commitTransfer]. */
    fun commit(type: TransactionType) {
        val entry = _state.value
        val walletId = entry.walletId
        // Never write a zero-amount transaction, and never without a wallet.
        if (!entry.canCommit || entry.isTransfer || walletId == null || committing) return
        committing = true

        viewModelScope.launch {
            try {
                // The chip row shows expense categories while the direction is
                // only decided here, so a mismatch is reachable. Storing it
                // would corrupt every per-category report built on it.
                val categoryType = entry.categoryId?.let { categoryRepository.typeOf(it) }
                val resolvedCategory = entry.categoryIdForCommit(type, categoryType)

                val id = transactionRepository.addManual(
                    amount = entry.amount,
                    type = type,
                    walletId = walletId,
                    categoryId = resolvedCategory,
                    occurredAt = entry.occurredAt,
                )
                _state.update { it.cleared() }
                _pendingUndo.value = UndoEvent(
                    transactionId = id,
                    amount = entry.amount,
                    type = type,
                    categoryWasDropped = entry.categoryId != null && resolvedCategory == null,
                )
            } finally {
                committing = false
            }
        }
    }

    /** Pindahkan — one row, not a paired Keluar + Masuk. */
    fun commitTransfer() {
        val entry = _state.value
        val from = entry.walletId
        val to = entry.toWalletId
        if (!entry.canCommit || !entry.isTransfer || from == null || to == null || committing) return
        committing = true

        viewModelScope.launch {
            try {
                val id = transactionRepository.addTransfer(
                    amount = entry.amount,
                    fromWalletId = from,
                    toWalletId = to,
                    occurredAt = entry.occurredAt,
                )
                _state.update { it.cleared() }
                _pendingUndo.value = UndoEvent(
                    transactionId = id,
                    amount = entry.amount,
                    type = TransactionType.TRANSFER,
                    categoryWasDropped = false,
                )
            } finally {
                committing = false
            }
        }
    }

    /** Deletes the row just inserted, by id. */
    fun undo() {
        val event = _pendingUndo.value ?: return
        _pendingUndo.value = null
        viewModelScope.launch { transactionRepository.delete(event.transactionId) }
    }

    /** The undo window elapsed or was dismissed; the row stays. */
    fun dismissUndo() {
        _pendingUndo.value = null
    }
}

/** What the confirmation snackbar needs to describe and reverse a commit. */
data class UndoEvent(
    val transactionId: Long,
    val amount: Long,
    val type: TransactionType,
    /**
     * True when the selected category belonged to the other direction and was
     * cleared. The confirmation says so rather than letting the user believe
     * the transaction was filed where they intended.
     */
    val categoryWasDropped: Boolean = false,
)
