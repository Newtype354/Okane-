package com.example.okane.domain.keypad

import com.example.okane.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Written before any UI wiring, per the brief. Every rule the app relies on is
 * pinned here, so a change that would let the in-app screen and the (deferred)
 * widget diverge fails on the JVM instead of on a device.
 */
class KeypadStateTest {

    private companion object {
        const val WALLET_A = 1L
        const val WALLET_B = 2L
        const val CATEGORY_FOOD = 10L
        const val CATEGORY_SALARY = 11L
    }

    /** A wallet is always selected in practice, so it is the default fixture. */
    private fun withWallet() = KeypadState(walletId = WALLET_A)

    private fun type(from: KeypadState, vararg keys: KeypadKey): KeypadState =
        keys.fold(from) { state, key -> state.press(key) }

    private fun type(vararg keys: KeypadKey): KeypadState = type(withWallet(), *keys)

    private fun digits(text: String): Array<KeypadKey> =
        text.map { KeypadKey.Digit(it - '0') as KeypadKey }.toTypedArray()

    // --- empty state -------------------------------------------------------

    @Test
    fun `fresh state is empty and cannot commit`() {
        val state = KeypadState()
        assertEquals("", state.digits)
        assertEquals(0L, state.amount)
        assertFalse(state.canCommit)
        assertNull(state.walletId)
        assertNull(state.toWalletId)
        assertNull(state.categoryId)
        assertNull(state.occurredAt)
        assertFalse(state.isTransfer)
    }

    @Test
    fun `empty entry displays as Rp 0`() {
        assertEquals("Rp 0", KeypadFormat.display(KeypadState()))
    }

    // --- digits ------------------------------------------------------------

    @Test
    fun `digits append in order`() {
        val state = type(*digits("12345"))
        assertEquals("12345", state.digits)
        assertEquals(12_345L, state.amount)
        assertTrue(state.canCommit)
    }

    @Test
    fun `zero after a non-zero digit appends normally`() {
        assertEquals(50L, type(KeypadKey.Digit(5), KeypadKey.Digit(0)).amount)
    }

    @Test
    fun `press does not mutate the receiver`() {
        val original = KeypadState(digits = "7", walletId = WALLET_A)
        original.press(KeypadKey.Digit(9))
        assertEquals("7", original.digits)
    }

    // --- leading zeros -----------------------------------------------------

    @Test
    fun `zero on an empty entry is a no-op`() {
        val state = type(KeypadKey.Digit(0))
        assertEquals("", state.digits)
        assertEquals(0L, state.amount)
        assertFalse(state.canCommit)
    }

    @Test
    fun `repeated zeros on an empty entry never accumulate`() {
        val state = type(KeypadKey.Digit(0), KeypadKey.Digit(0), KeypadKey.Digit(0))
        assertEquals("", state.digits)
    }

    @Test
    fun `triple zero on an empty entry produces no value`() {
        val state = type(KeypadKey.TripleZero)
        assertEquals("", state.digits)
        assertEquals(0L, state.amount)
        assertFalse(state.canCommit)
    }

    @Test
    fun `triple zero repeated on an empty entry stays empty`() {
        val state = type(KeypadKey.TripleZero, KeypadKey.TripleZero)
        assertEquals("", state.digits)
    }

    // --- triple zero -------------------------------------------------------

    @Test
    fun `triple zero appends three zeros`() {
        val state = type(KeypadKey.Digit(5), KeypadKey.TripleZero)
        assertEquals("5000", state.digits)
        assertEquals(5_000L, state.amount)
    }

    @Test
    fun `triple zero twice appends six zeros, not a squared multiplier`() {
        val state = type(KeypadKey.Digit(5), KeypadKey.TripleZero, KeypadKey.TripleZero)
        assertEquals("5000000", state.digits)
        assertEquals(5_000_000L, state.amount)
    }

    // --- backspace ---------------------------------------------------------

    @Test
    fun `backspace removes the last digit`() {
        assertEquals("12", type(*digits("123"), KeypadKey.Backspace).digits)
    }

    @Test
    fun `backspace on an empty entry is a no-op`() {
        val state = type(KeypadKey.Backspace)
        assertEquals("", state.digits)
        assertFalse(state.canCommit)
    }

    @Test
    fun `backspace removes one zero of a triple zero, not all three`() {
        val state = type(KeypadKey.Digit(5), KeypadKey.TripleZero, KeypadKey.Backspace)
        assertEquals("500", state.digits)
    }

    @Test
    fun `backspacing everything returns to a non-committable state`() {
        var state = type(*digits("42"))
        repeat(2) { state = state.press(KeypadKey.Backspace) }
        assertEquals("", state.digits)
        assertEquals(0L, state.amount)
        assertFalse(state.canCommit)
    }

    // --- 12-digit cap ------------------------------------------------------

    @Test
    fun `twelve digits are allowed`() {
        val state = type(*digits("123456789012"))
        assertEquals("123456789012", state.digits)
        assertEquals(123_456_789_012L, state.amount)
    }

    @Test
    fun `a thirteenth digit is refused`() {
        val state = type(*digits("123456789012"), KeypadKey.Digit(9))
        assertEquals("123456789012", state.digits)
    }

    @Test
    fun `triple zero is refused whole rather than partially appended`() {
        // 10 digits + "000" would be 13. Appending the two zeros that fit would
        // multiply by 100 while the user pressed a key meaning 1000.
        val state = type(*digits("1234567890"), KeypadKey.TripleZero)
        assertEquals("1234567890", state.digits)
    }

    @Test
    fun `triple zero is allowed when it lands exactly on the cap`() {
        val state = type(*digits("123456789"), KeypadKey.TripleZero)
        assertEquals("123456789000", state.digits)
        assertEquals(12, state.digits.length)
    }

    @Test
    fun `backspace after hitting the cap frees room again`() {
        val state = type(*digits("123456789012"), KeypadKey.Backspace, KeypadKey.Digit(7))
        assertEquals("123456789017", state.digits)
    }

    // --- wallet ------------------------------------------------------------

    @Test
    fun `an amount without a wallet cannot commit`() {
        val state = type(KeypadState(), *digits("5000"))
        assertEquals(5_000L, state.amount)
        assertFalse(state.canCommit)
    }

    @Test
    fun `selecting a wallet makes a typed amount committable`() {
        val state = type(KeypadState(), *digits("5000")).selectWallet(WALLET_A)
        assertTrue(state.canCommit)
    }

    @Test
    fun `selecting a wallet replaces the previous one`() {
        val state = withWallet().selectWallet(WALLET_B)
        assertEquals(WALLET_B, state.walletId)
    }

    // --- transfer ----------------------------------------------------------

    @Test
    fun `transfer without a destination cannot commit`() {
        val state = type(*digits("5000")).setTransfer(true)
        assertTrue(state.isTransfer)
        assertFalse(state.canCommit)
    }

    @Test
    fun `transfer to the same wallet cannot commit`() {
        val state = type(*digits("5000")).setTransfer(true).selectToWallet(WALLET_A)
        assertFalse(state.canCommit)
    }

    @Test
    fun `transfer to a different wallet can commit`() {
        val state = type(*digits("5000")).setTransfer(true).selectToWallet(WALLET_B)
        assertTrue(state.canCommit)
    }

    @Test
    fun `a zero-amount transfer still cannot commit`() {
        val state = withWallet().setTransfer(true).selectToWallet(WALLET_B)
        assertFalse(state.canCommit)
    }

    @Test
    fun `entering transfer mode drops the selected category`() {
        val state = withWallet().toggleCategory(CATEGORY_FOOD).setTransfer(true)
        assertNull(state.categoryId)
    }

    @Test
    fun `leaving transfer mode drops the destination wallet`() {
        val state = withWallet()
            .setTransfer(true)
            .selectToWallet(WALLET_B)
            .setTransfer(false)
        assertNull(state.toWalletId)
        assertFalse(state.isTransfer)
    }

    @Test
    fun `changing the source onto the destination blocks commit until resolved`() {
        // Deliberately reachable: the selection is not silently rewritten.
        val conflicted = type(*digits("5000"))
            .setTransfer(true)
            .selectToWallet(WALLET_B)
            .selectWallet(WALLET_B)
        assertFalse(conflicted.canCommit)
        assertTrue(conflicted.selectToWallet(WALLET_A).canCommit)
    }

    // --- category ----------------------------------------------------------

    @Test
    fun `toggling an unselected category selects it`() {
        assertEquals(CATEGORY_FOOD, withWallet().toggleCategory(CATEGORY_FOOD).categoryId)
    }

    @Test
    fun `toggling the selected category deselects it`() {
        val state = withWallet().toggleCategory(CATEGORY_FOOD).toggleCategory(CATEGORY_FOOD)
        assertNull(state.categoryId)
    }

    @Test
    fun `toggling a different category replaces the selection`() {
        val state = withWallet().toggleCategory(CATEGORY_FOOD).toggleCategory(CATEGORY_SALARY)
        assertEquals(CATEGORY_SALARY, state.categoryId)
    }

    @Test
    fun `category does not affect commitability`() {
        assertTrue(type(KeypadKey.Digit(1)).canCommit)
        assertTrue(type(KeypadKey.Digit(1)).toggleCategory(CATEGORY_FOOD).canCommit)
    }

    // --- commit-time category direction resolution -------------------------

    @Test
    fun `a matching category survives commit`() {
        val state = withWallet().toggleCategory(CATEGORY_FOOD)
        assertEquals(
            CATEGORY_FOOD,
            state.categoryIdForCommit(TransactionType.OUT, TransactionType.OUT)
        )
    }

    @Test
    fun `a category from the other direction is cleared rather than mismatched`() {
        val state = withWallet().toggleCategory(CATEGORY_FOOD)  // an expense category
        assertNull(state.categoryIdForCommit(TransactionType.IN, TransactionType.OUT))
    }

    @Test
    fun `no selection resolves to no category`() {
        assertNull(withWallet().categoryIdForCommit(TransactionType.OUT, null))
    }

    @Test
    fun `an unresolvable category type is cleared`() {
        val state = withWallet().toggleCategory(CATEGORY_FOOD)
        assertNull(state.categoryIdForCommit(TransactionType.OUT, null))
    }

    @Test
    fun `a transfer never carries a category`() {
        val state = withWallet().toggleCategory(CATEGORY_FOOD)
        assertNull(
            state.categoryIdForCommit(TransactionType.TRANSFER, TransactionType.OUT)
        )
    }

    // --- date --------------------------------------------------------------

    @Test
    fun `occurredAt can be set and cleared back to now`() {
        val backdated = withWallet().withOccurredAt(1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, backdated.occurredAt)
        assertNull(backdated.withOccurredAt(null).occurredAt)
    }

    // --- reset -------------------------------------------------------------

    @Test
    fun `cleared resets amount, category, date and transfer mode`() {
        val state = type(*digits("500"))
            .toggleCategory(CATEGORY_FOOD)
            .withOccurredAt(1_700_000_000_000L)
            .setTransfer(true)
            .selectToWallet(WALLET_B)
            .cleared()

        assertEquals("", state.digits)
        assertNull(state.categoryId)
        assertNull(state.occurredAt)
        assertNull(state.toWalletId)
        assertFalse(state.isTransfer)
        assertFalse(state.canCommit)
    }

    @Test
    fun `cleared keeps the selected wallet`() {
        val state = type(withWallet().selectWallet(WALLET_B), *digits("500")).cleared()
        assertEquals(WALLET_B, state.walletId)
    }

    // --- formatting --------------------------------------------------------

    @Test
    fun `amounts are grouped with dots and a space after Rp`() {
        assertEquals("Rp 500.000", KeypadFormat.rupiah(500_000))
        assertEquals("Rp 1.234.567", KeypadFormat.rupiah(1_234_567))
        assertEquals("Rp 5", KeypadFormat.rupiah(5))
        assertEquals("Rp 0", KeypadFormat.rupiah(0))
    }

    @Test
    fun `display tracks the entry as it is typed`() {
        var state = withWallet()
        assertEquals("Rp 0", KeypadFormat.display(state))
        state = state.press(KeypadKey.Digit(5))
        assertEquals("Rp 5", KeypadFormat.display(state))
        state = state.press(KeypadKey.TripleZero)
        assertEquals("Rp 5.000", KeypadFormat.display(state))
        state = state.press(KeypadKey.TripleZero)
        assertEquals("Rp 5.000.000", KeypadFormat.display(state))
    }

    // --- key validity ------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `digit key rejects values outside 0 to 9`() {
        KeypadKey.Digit(10)
    }
}
