package com.example.okane.ui.wallets

import com.example.okane.data.local.WalletBalance
import com.example.okane.domain.keypad.KeypadState

enum class WalletSurface { ROOT, DETAIL, ARCHIVED }

enum class WalletEditorMode { ADD, EDIT }

data class WalletEditorState(
    val mode: WalletEditorMode,
    val walletId: Long? = null,
    val name: String = "",
    val initialBalanceInput: String = "",
    val monthlyFeeInput: String = "",
    val makeDefault: Boolean = false,
    val defaultLocked: Boolean = false,
    val errorMessage: String? = null,
) {
    val initialBalance: Long get() = initialBalanceInput.toLongOrNull() ?: 0L
    val monthlyFee: Long get() = monthlyFeeInput.toLongOrNull() ?: 0L
}

data class WalletsUiState(
    val isLoading: Boolean = true,
    val totalMoney: Long = 0,
    val activeWallets: List<WalletBalance> = emptyList(),
    val archivedWallets: List<WalletBalance> = emptyList(),
    val archivedContributesToTotal: Boolean = false,
    val surface: WalletSurface = WalletSurface.ROOT,
    val selectedWallet: WalletBalance? = null,
    val editor: WalletEditorState? = null,
    val archiveCandidate: WalletBalance? = null,
    val operationInProgress: Boolean = false,
    val feedbackMessage: String? = null,
) {
    val hasOpenSecondarySurface: Boolean
        get() = surface != WalletSurface.ROOT || editor != null || archiveCandidate != null
}

/** Pure state mapping shared by the ViewModel and JVM tests. */
object WalletsPresenter {
    fun active(rows: List<WalletBalance>): List<WalletBalance> =
        rows.filterNot(WalletBalance::isArchived)

    fun archived(rows: List<WalletBalance>): List<WalletBalance> =
        rows.filter(WalletBalance::isArchived)

    fun total(rows: List<WalletBalance>): Long = rows.fold(0L) { total, row ->
        total + row.balance
    }

    fun archivedContributes(rows: List<WalletBalance>): Boolean =
        rows.any { it.isArchived && it.balance != 0L }
}

/** Whole-rupiah, non-negative input consistent with onboarding and keypad limits. */
fun sanitizeWalletAmountInput(raw: String): String =
    raw.filter(Char::isDigit).take(KeypadState.MAX_DIGITS)

