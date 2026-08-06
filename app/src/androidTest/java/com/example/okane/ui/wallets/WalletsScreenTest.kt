package com.example.okane.ui.wallets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.okane.data.local.WalletBalance
import com.example.okane.ui.theme.OkaneTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val cash = wallet(id = 1, name = "Tunai", balance = 500_000, default = true)
    private val bank = wallet(id = 2, name = "BCA", balance = 2_000_000, fee = 15_000)
    private val old = wallet(id = 3, name = "Lama", balance = 125_000, archived = true)
    private var settingsOpened = false
    private var state by mutableStateOf(WalletsUiState())

    @Before
    fun setUp() {
        settingsOpened = false
        state = WalletsUiState(
            isLoading = false,
            totalMoney = cash.balance + bank.balance + old.balance,
            activeWallets = listOf(cash, bank),
            archivedWallets = listOf(old),
            archivedContributesToTotal = true,
        )
        composeRule.setContent {
            OkaneTheme {
                WalletsScreen(state = state, actions = actions())
            }
        }
    }

    @Test
    fun totalIncludesArchivedAndAddFormCreatesVisibleWallet() {
        composeRule.onNodeWithText("Rp2.625.000").assertIsDisplayed()
        composeRule.onNodeWithText("Termasuk dompet diarsipkan").assertIsDisplayed()

        composeRule.onNodeWithTag("addWallet").performClick()
        composeRule.onNodeWithTag("walletEditor").assertIsDisplayed()
        composeRule.onNodeWithTag("walletNameInput").performTextInput("  GoPay  ")
        composeRule.onNodeWithTag("saveWallet").performClick()

        composeRule.onNodeWithText("GoPay").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(3, state.activeWallets.size) }
    }

    @Test
    fun detailMovesDefaultThenArchiveAppearsInArchivedList() {
        composeRule.onNodeWithTag("walletRow-2").performClick()
        composeRule.onNodeWithTag("walletDetail").assertIsDisplayed()
        composeRule.onNodeWithText("Rp15.000").assertIsDisplayed()

        composeRule.onNodeWithTag("makeDefaultWallet").performClick()
        composeRule.runOnIdle { assertTrue(state.selectedWallet?.isDefault == true) }

        composeRule.onNodeWithTag("archiveWallet").performClick()
        composeRule.onNodeWithText("Arsipkan dompet?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmArchiveWallet").performClick()
        composeRule.onNodeWithTag("walletRow-2").assertDoesNotExist()

        composeRule.onNodeWithTag("openArchivedWallets").performClick()
        composeRule.onNodeWithTag("walletRow-2").assertIsDisplayed()
        composeRule.onAllNodesWithText("Diarsipkan", useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }

    private fun actions() = WalletActions(
        onOpenSettings = { settingsOpened = true },
        onOpenAdd = {
            state = state.copy(editor = WalletEditorState(mode = WalletEditorMode.ADD))
        },
        onOpenDetail = { id, fromArchived ->
            val selected = (state.activeWallets + state.archivedWallets)
                .first { it.walletId == id }
            state = state.copy(
                surface = WalletSurface.DETAIL,
                selectedWallet = selected,
            )
        },
        onOpenArchived = { state = state.copy(surface = WalletSurface.ARCHIVED) },
        onBack = { state = state.copy(surface = WalletSurface.ROOT, selectedWallet = null) },
        onOpenEdit = {},
        onDismissEditor = { state = state.copy(editor = null) },
        onEditorNameChanged = { value ->
            state = state.copy(editor = state.editor?.copy(name = value))
        },
        onEditorInitialBalanceChanged = { value ->
            state = state.copy(editor = state.editor?.copy(initialBalanceInput = value))
        },
        onEditorMonthlyFeeChanged = { value ->
            state = state.copy(editor = state.editor?.copy(monthlyFeeInput = value))
        },
        onEditorDefaultChanged = { value ->
            state = state.copy(editor = state.editor?.copy(makeDefault = value))
        },
        onSaveEditor = {
            val editor = checkNotNull(state.editor)
            val added = wallet(
                id = 99,
                name = editor.name.trim(),
                balance = editor.initialBalance,
                default = editor.makeDefault,
                fee = editor.monthlyFee,
            )
            state = state.copy(
                activeWallets = state.activeWallets + added,
                totalMoney = state.totalMoney + added.balance,
                editor = null,
            )
        },
        onMakeDefault = {
            val selectedId = checkNotNull(state.selectedWallet).walletId
            val active = state.activeWallets.map {
                it.copy(isDefault = it.walletId == selectedId)
            }
            state = state.copy(
                activeWallets = active,
                selectedWallet = active.first { it.walletId == selectedId },
            )
        },
        onRequestArchive = {
            state = state.copy(archiveCandidate = state.selectedWallet)
        },
        onDismissArchive = { state = state.copy(archiveCandidate = null) },
        onConfirmArchive = {
            val archived = checkNotNull(state.archiveCandidate).copy(
                isArchived = true,
                isDefault = false,
            )
            val remaining = state.activeWallets
                .filterNot { it.walletId == archived.walletId }
                .mapIndexed { index, wallet -> wallet.copy(isDefault = index == 0) }
            state = state.copy(
                activeWallets = remaining,
                archivedWallets = state.archivedWallets + archived,
                surface = WalletSurface.ROOT,
                selectedWallet = null,
                archiveCandidate = null,
            )
        },
        onRestore = {},
        onFeedbackShown = { state = state.copy(feedbackMessage = null) },
    )

    private fun wallet(
        id: Long,
        name: String,
        balance: Long,
        default: Boolean = false,
        fee: Long = 0,
        archived: Boolean = false,
    ) = WalletBalance(
        walletId = id,
        name = name,
        balance = balance,
        isDefault = default,
        initialBalance = balance,
        monthlyFee = fee,
        isArchived = archived,
    )
}
