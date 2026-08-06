package com.example.okane.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okane.data.local.DefaultCategories
import com.example.okane.data.local.DefaultWallets
import com.example.okane.data.prefs.OnboardingPreferences
import com.example.okane.data.repository.CategoryRepository
import com.example.okane.data.repository.WalletRepository
import com.example.okane.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row of the wallet checklist. Nothing is written until finish. */
data class WalletDraft(
    val name: String,
    val selected: Boolean = false,
    val initialBalance: Long = 0,
    val monthlyFee: Long = 0,
)

/** One row of the category checklist. */
data class CategoryDraft(
    val name: String,
    val type: TransactionType,
    val selected: Boolean = true,
)

data class OnboardingUiState(
    val slide: Int = 0,
    val wallets: List<WalletDraft> = DefaultWallets.SUGGESTIONS.map { WalletDraft(it) },
    // Pre-ticked, so the user's job is deletion rather than invention — and so
    // that skipping this slide means "accept the defaults" with no special case.
    val categories: List<CategoryDraft> = DefaultCategories.ALL.map {
        CategoryDraft(it.name, it.type)
    },
    val isFinishing: Boolean = false,
) {
    val isLastSlide: Boolean get() = slide == LAST_SLIDE

    // --- pure transitions --------------------------------------------------
    // Kept on the state rather than in the ViewModel so they are JVM-testable
    // without a database, the same split as KeypadState.

    fun atNextSlide(): OnboardingUiState = copy(slide = (slide + 1).coerceAtMost(LAST_SLIDE))

    fun atPreviousSlide(): OnboardingUiState = copy(slide = (slide - 1).coerceAtLeast(0))

    fun toggleWallet(name: String): OnboardingUiState = copy(
        wallets = wallets.map { if (it.name == name) it.copy(selected = !it.selected) else it }
    )

    fun withWalletInitialBalance(name: String, amount: Long): OnboardingUiState = copy(
        wallets = wallets.map { if (it.name == name) it.copy(initialBalance = amount) else it }
    )

    fun withWalletMonthlyFee(name: String, amount: Long): OnboardingUiState = copy(
        wallets = wallets.map { if (it.name == name) it.copy(monthlyFee = amount) else it }
    )

    /**
     * Adds a custom name, already ticked. Blanks are ignored, and a name that
     * is already on the list is ticked rather than duplicated — otherwise
     * typing "BCA" would produce two BCA rows and, later, two BCA wallets.
     */
    fun withCustomWallet(name: String): OnboardingUiState {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this
        val existing = wallets.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        return if (existing != null) {
            copy(wallets = wallets.map { if (it.name == existing.name) it.copy(selected = true) else it })
        } else {
            copy(wallets = wallets + WalletDraft(trimmed, selected = true))
        }
    }

    fun toggleCategory(name: String, type: TransactionType): OnboardingUiState = copy(
        categories = categories.map {
            if (it.name == name && it.type == type) it.copy(selected = !it.selected) else it
        }
    )

    /**
     * A category belongs to exactly one side, so the same name on the other
     * side is a different category — "Lainnya" legitimately exists on both.
     */
    fun withCustomCategory(name: String, type: TransactionType): OnboardingUiState {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return this
        val existing = categories.firstOrNull {
            it.name.equals(trimmed, ignoreCase = true) && it.type == type
        }
        return if (existing != null) {
            copy(
                categories = categories.map {
                    if (it.name == existing.name && it.type == type) it.copy(selected = true) else it
                }
            )
        } else {
            copy(categories = categories + CategoryDraft(trimmed, type))
        }
    }

    companion object {
        const val LAST_SLIDE = 2
    }
}

/**
 * Onboarding runs once on first launch. Every slide is skippable, and skipping
 * all of them must leave a fully working app.
 *
 * That requirement shapes the defaults rather than the finish logic:
 *
 * - Categories start **ticked**, so skipping slide 3 writes the full default
 *   set — the same outcome as tapping through it, with no special case.
 * - Wallets start **unticked** (the brief says selecting one creates it), so
 *   skipping slide 2 writes none and [WalletRepository.ensureDefaultWallet]
 *   creates `Tunai`. That call is unconditional, so the app can always record
 *   a transaction no matter which path the user took.
 *
 * Nothing is written to the database until [finish]. A user who backs out
 * mid-flow leaves no half-built wallets behind.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Read synchronously so the first frame shows the right screen instead of
    // flashing onboarding at a returning user.
    private val _completed = MutableStateFlow(onboardingPreferences.isCompleted())
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    // --- navigation --------------------------------------------------------

    /** Advances, or finishes on the last slide. Used by both Lanjut and Lewati. */
    fun next() {
        if (_uiState.value.isLastSlide) finish() else _uiState.update { it.atNextSlide() }
    }

    fun back() = _uiState.update { it.atPreviousSlide() }

    // --- slide 2: wallets --------------------------------------------------

    fun toggleWallet(name: String) = _uiState.update { it.toggleWallet(name) }

    /** Optional, revealed on tap — never required inline. */
    fun setWalletInitialBalance(name: String, amount: Long) =
        _uiState.update { it.withWalletInitialBalance(name, amount) }

    /** Stored on the wallet; nothing schedules it (out of scope this wave). */
    fun setWalletMonthlyFee(name: String, amount: Long) =
        _uiState.update { it.withWalletMonthlyFee(name, amount) }

    fun addCustomWallet(name: String) = _uiState.update { it.withCustomWallet(name) }

    // --- slide 3: categories -----------------------------------------------

    fun toggleCategory(name: String, type: TransactionType) =
        _uiState.update { it.toggleCategory(name, type) }

    fun addCustomCategory(name: String, type: TransactionType) =
        _uiState.update { it.withCustomCategory(name, type) }

    // --- finish ------------------------------------------------------------

    fun finish() {
        if (_uiState.value.isFinishing) return   // double-tap on Selesai
        _uiState.update { it.copy(isFinishing = true) }

        viewModelScope.launch {
            try {
                val state = _uiState.value

                // In list order, so the first one the user sees ticked becomes
                // the default (WalletRepository makes the first wallet default).
                state.wallets.filter { it.selected }.forEach { draft ->
                    walletRepository.createIfAbsent(
                        name = draft.name,
                        initialBalance = draft.initialBalance,
                        monthlyFee = draft.monthlyFee,
                    )
                }
                // Unconditional safety net: guarantees a wallet and a default
                // exist however the user got here, including skipping it all.
                walletRepository.ensureDefaultWallet()

                // create() is idempotent by (name, type).
                state.categories.filter { it.selected }.forEach { draft ->
                    categoryRepository.create(draft.name, draft.type)
                }

                onboardingPreferences.markCompleted()
                _completed.value = true
            } finally {
                _uiState.update { it.copy(isFinishing = false) }
            }
        }
    }
}
