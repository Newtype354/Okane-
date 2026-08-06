package com.example.okane.domain.keypad

import com.example.okane.domain.model.TransactionType

/**
 * The whole input engine for transaction entry.
 *
 * NO ANDROID IMPORTS BELONG IN THIS FILE. The in-app screen holds this state
 * in a ViewModel; the deferred home screen widget will persist it to DataStore,
 * because its process can be killed between two taps. Same rules, different
 * storage — which only works if the rules live somewhere neither surface owns.
 *
 * Keeping this seam while the widget is deferred costs almost nothing;
 * retrofitting it later would mean writing the input rules a second time and
 * then reconciling the two.
 *
 * If input logic starts appearing in a composable or a ViewModel, it belongs
 * here instead.
 *
 * "Calculator" describes the interaction shape, not arithmetic. There is no
 * chained addition and there are no + or − keys: the commit buttons
 * (Keluar / Masuk / Pindahkan) are the only operators in the product.
 */
data class KeypadState(
    val digits: String = "",
    val walletId: Long? = null,
    val toWalletId: Long? = null,     // TRANSFER only
    val categoryId: Long? = null,     // always null for TRANSFER
    val occurredAt: Long? = null,     // null means "now"
    val isTransfer: Boolean = false,
) {
    /** Whole rupiah, always positive. Direction is the commit button's job. */
    val amount: Long get() = digits.toLongOrNull() ?: 0L

    /**
     * A transfer additionally needs a destination that differs from the
     * source. Selecting the same wallet on both sides is allowed to exist as
     * a transient state — the commit button simply stays disabled until it is
     * resolved, rather than the selection being silently rewritten under the
     * user's finger.
     */
    val canCommit: Boolean
        get() = when {
            amount <= 0 -> false          // never write a zero-amount transaction
            walletId == null -> false     // every transaction belongs to a wallet
            isTransfer -> toWalletId != null && toWalletId != walletId
            else -> true                  // category stays optional
        }

    fun press(key: KeypadKey): KeypadState = when (key) {
        is KeypadKey.Digit -> append(key.value.toString())
        KeypadKey.TripleZero -> append("000")
        KeypadKey.Backspace -> copy(digits = digits.dropLast(1))
    }

    /**
     * Appending is refused in two cases, both deliberate:
     *
     * 1. Zeros onto an empty entry. Leading zeros must never accumulate, so
     *    "0" and "000" are no-ops until a non-zero digit starts the number.
     *    An entry of "000" would read as a nonzero-looking display worth zero.
     *
     * 2. Anything that would cross [MAX_DIGITS]. The cap is enforced
     *    all-or-nothing: at 10 digits, "000" appends nothing rather than the
     *    two zeros that happen to fit. A partial append silently multiplies
     *    the amount by 100 while the user pressed a key meaning 1000, which is
     *    exactly the kind of error nobody notices before committing.
     */
    private fun append(toAppend: String): KeypadState {
        if (digits.isEmpty() && toAppend.all { it == '0' }) return this
        if (digits.length + toAppend.length > MAX_DIGITS) return this
        return copy(digits = digits + toAppend)
    }

    fun selectWallet(id: Long): KeypadState = copy(walletId = id)

    fun selectToWallet(id: Long): KeypadState = copy(toWalletId = id)

    /** Category is optional; tapping the selected chip clears the selection. */
    fun toggleCategory(id: Long): KeypadState =
        copy(categoryId = if (categoryId == id) null else id)

    /** null restores "now". */
    fun withOccurredAt(epochMillis: Long?): KeypadState = copy(occurredAt = epochMillis)

    /**
     * Entering transfer mode drops any chosen category, because a transfer has
     * no category by definition. Leaving it drops the destination wallet, so a
     * stale destination cannot leak into the next entry.
     */
    fun setTransfer(enabled: Boolean): KeypadState =
        if (enabled) copy(isTransfer = true, categoryId = null)
        else copy(isTransfer = false, toWalletId = null)

    /**
     * Post-commit reset: amount and category cleared, date back to today,
     * transfer mode left — but the selected wallet is kept, because the next
     * entry is usually from the same wallet and re-picking it every time is
     * exactly the friction this product exists to remove.
     */
    fun cleared(): KeypadState = KeypadState(walletId = walletId)

    companion object {
        /** Caps overflow and layout breakage. 999.999.999.999 is plenty. */
        const val MAX_DIGITS = 12
    }
}

sealed interface KeypadKey {
    data class Digit(val value: Int) : KeypadKey {
        init { require(value in 0..9) { "Digit must be 0..9, was $value" } }
    }

    /**
     * "000" — appends three zeros, and behaves identically in every state.
     *
     * Not a "rb" (×1000) key: multiply-by-thousand pressed twice produces
     * unintended millions, whereas appending is what the user sees happen.
     */
    data object TripleZero : KeypadKey

    data object Backspace : KeypadKey
}

/**
 * Resolves the category to store, given the direction actually committed.
 *
 * The chip row shows one direction's categories while the direction itself is
 * only known when a commit button is pressed, so a mismatch is reachable: pick
 * an expense category, then press `Masuk`. Storing that would put an expense
 * category on an income row and quietly corrupt every per-category report
 * built on it. Clearing is lossy but honest, and the row still shows up in the
 * home screen's uncategorised inbox to be tidied up.
 *
 * @param committedType the direction of the button the user pressed
 * @param selectedCategoryType the side the selected category belongs to, or
 *   null when no category is selected or it could not be resolved
 */
fun KeypadState.categoryIdForCommit(
    committedType: TransactionType,
    selectedCategoryType: TransactionType?,
): Long? = when {
    categoryId == null -> null
    committedType == TransactionType.TRANSFER -> null   // transfers have no category
    selectedCategoryType == committedType -> categoryId
    else -> null
}
