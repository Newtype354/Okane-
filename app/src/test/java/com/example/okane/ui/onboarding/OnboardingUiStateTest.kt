package com.example.okane.ui.onboarding

import com.example.okane.data.local.DefaultCategories
import com.example.okane.data.local.DefaultWallets
import com.example.okane.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The skip-everything path is the single most important behaviour here: a user
 * who taps `Lewati` three times must end up with a working app. These tests
 * pin the defaults that make that true without any special-casing in `finish`.
 */
class OnboardingUiStateTest {

    private val initial = OnboardingUiState()

    // --- defaults that make skipping safe ----------------------------------

    @Test
    fun `every category starts ticked, so skipping accepts the full default set`() {
        assertTrue(initial.categories.isNotEmpty())
        assertTrue(initial.categories.all { it.selected })
        assertEquals(DefaultCategories.ALL.size, initial.categories.size)
    }

    @Test
    fun `both sides are offered`() {
        val expense = initial.categories.filter { it.type == TransactionType.OUT }
        val income = initial.categories.filter { it.type == TransactionType.IN }
        assertEquals(DefaultCategories.EXPENSE.size, expense.size)
        assertEquals(DefaultCategories.INCOME.size, income.size)
    }

    @Test
    fun `Lainnya exists on both sides as two distinct categories`() {
        assertEquals(2, initial.categories.count { it.name == "Lainnya" })
    }

    @Test
    fun `no wallet starts ticked, so skipping falls through to the Tunai default`() {
        assertTrue(initial.wallets.none { it.selected })
        assertEquals(DefaultWallets.SUGGESTIONS, initial.wallets.map { it.name })
    }

    @Test
    fun `the fallback wallet is on the suggestion list`() {
        assertTrue(DefaultWallets.SUGGESTIONS.contains(DefaultWallets.FALLBACK_NAME))
    }

    @Test
    fun `wallets start with no balance or fee, so nothing is required inline`() {
        assertTrue(initial.wallets.all { it.initialBalance == 0L && it.monthlyFee == 0L })
    }

    // --- navigation --------------------------------------------------------

    @Test
    fun `slides advance and clamp at the last one`() {
        val last = initial.atNextSlide().atNextSlide()
        assertEquals(OnboardingUiState.LAST_SLIDE, last.slide)
        assertTrue(last.isLastSlide)
        assertEquals(OnboardingUiState.LAST_SLIDE, last.atNextSlide().slide)
    }

    @Test
    fun `going back clamps at the first slide`() {
        assertEquals(0, initial.atPreviousSlide().slide)
        assertEquals(0, initial.atNextSlide().atPreviousSlide().atPreviousSlide().slide)
    }

    // --- wallets -----------------------------------------------------------

    @Test
    fun `toggling a wallet selects then deselects it`() {
        val on = initial.toggleWallet("BCA")
        assertTrue(on.wallets.first { it.name == "BCA" }.selected)
        assertFalse(on.toggleWallet("BCA").wallets.first { it.name == "BCA" }.selected)
    }

    @Test
    fun `toggling one wallet leaves the others alone`() {
        val state = initial.toggleWallet("BCA")
        assertEquals(1, state.wallets.count { it.selected })
    }

    @Test
    fun `optional details attach to the right wallet`() {
        val state = initial
            .withWalletInitialBalance("BCA", 1_500_000)
            .withWalletMonthlyFee("BCA", 15_000)
        val bca = state.wallets.first { it.name == "BCA" }
        assertEquals(1_500_000L, bca.initialBalance)
        assertEquals(15_000L, bca.monthlyFee)
        assertTrue(state.wallets.filter { it.name != "BCA" }.all { it.initialBalance == 0L })
    }

    @Test
    fun `a custom wallet is added already ticked`() {
        val state = initial.withCustomWallet("Blu")
        val added = state.wallets.last()
        assertEquals("Blu", added.name)
        assertTrue(added.selected)
    }

    @Test
    fun `a custom wallet name is trimmed`() {
        assertEquals("Blu", initial.withCustomWallet("  Blu  ").wallets.last().name)
    }

    @Test
    fun `a blank custom wallet is ignored`() {
        assertEquals(initial.wallets.size, initial.withCustomWallet("   ").wallets.size)
    }

    @Test
    fun `re-adding an existing wallet ticks it instead of duplicating`() {
        // Otherwise typing a name already on the checklist would later create
        // two wallets with the same name.
        val state = initial.withCustomWallet("bca")
        assertEquals(initial.wallets.size, state.wallets.size)
        assertTrue(state.wallets.first { it.name == "BCA" }.selected)
    }

    // --- categories --------------------------------------------------------

    @Test
    fun `unticking a category leaves its twin on the other side alone`() {
        val state = initial.toggleCategory("Lainnya", TransactionType.OUT)
        assertFalse(state.categories.first {
            it.name == "Lainnya" && it.type == TransactionType.OUT
        }.selected)
        assertTrue(state.categories.first {
            it.name == "Lainnya" && it.type == TransactionType.IN
        }.selected)
    }

    @Test
    fun `unticking everything is respected rather than overridden`() {
        val state = initial.categories.fold(initial) { acc, category ->
            acc.toggleCategory(category.name, category.type)
        }
        assertTrue(state.categories.none { it.selected })
    }

    @Test
    fun `a custom category is added to the chosen side only`() {
        val state = initial.withCustomCategory("Kos", TransactionType.OUT)
        val added = state.categories.last()
        assertEquals("Kos", added.name)
        assertEquals(TransactionType.OUT, added.type)
        assertTrue(added.selected)
        assertTrue(state.categories.none {
            it.name == "Kos" && it.type == TransactionType.IN
        })
    }

    @Test
    fun `the same custom name on each side makes two categories`() {
        val state = initial
            .withCustomCategory("Hadiah", TransactionType.OUT)
            .withCustomCategory("Hadiah", TransactionType.IN)
        assertEquals(2, state.categories.count { it.name == "Hadiah" })
    }

    @Test
    fun `re-adding an existing category re-ticks it instead of duplicating`() {
        val unticked = initial.toggleCategory("Makan", TransactionType.OUT)
        val state = unticked.withCustomCategory("makan", TransactionType.OUT)
        assertEquals(initial.categories.size, state.categories.size)
        assertTrue(state.categories.first {
            it.name == "Makan" && it.type == TransactionType.OUT
        }.selected)
    }

    @Test
    fun `a blank custom category is ignored`() {
        assertEquals(
            initial.categories.size,
            initial.withCustomCategory("  ", TransactionType.OUT).categories.size
        )
    }
}
