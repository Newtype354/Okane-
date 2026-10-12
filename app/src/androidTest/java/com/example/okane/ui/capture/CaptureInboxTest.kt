package com.example.okane.ui.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.theme.OkaneTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureInboxTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val card = SuggestionCard(
        id = 1,
        sourceApp = "Livin",
        channelLabel = null,
        title = "Transaksi berhasil",
        text = "Top Up GoPay Rp101.000 berhasil",
        amount = 101_000,
        counterparty = null,
        timeLabel = "Hari ini · 08.10",
        proposedType = TransactionType.TRANSFER,
        proposedWalletId = 3,
        proposedToWalletId = 2,
        duplicateNote = "Sudah tercatat sebagai pindah saldo Mandiri → GoPay Rp100.000.",
    )
    private val wallets = listOf(WalletChoice(1, "Tunai"), WalletChoice(2, "GoPay"), WalletChoice(3, "Mandiri"))

    @Test
    fun homeCardShowsTheCountAndOpensTheInbox() {
        var opened = false
        composeRule.setContent {
            OkaneTheme {
                CaptureInboxCard(
                    state = CaptureInboxUiState(cards = listOf(card), wallets = wallets),
                    onOpen = { opened = true },
                )
            }
        }

        composeRule.onNodeWithText("1 transaksi perlu konfirmasi").assertIsDisplayed()
        composeRule.onNodeWithTag("homeCaptureInbox").performClick()
        composeRule.runOnIdle { assertTrue(opened) }
    }

    @Test
    fun anEmptyInboxLeavesHomeUnchanged() {
        composeRule.setContent {
            OkaneTheme { CaptureInboxCard(state = CaptureInboxUiState(), onOpen = {}) }
        }
        composeRule.onNodeWithTag("homeCaptureInbox").assertDoesNotExist()
    }

    @Test
    fun reviewSheetLetsTheUserChangeTheTypeThenConfirm() {
        var state by mutableStateOf(
            CaptureInboxUiState(
                cards = listOf(card),
                wallets = wallets,
                sheetOpen = true,
                selection = ReviewSelection(TransactionType.TRANSFER, walletId = 3, toWalletId = 2),
            )
        )
        val events = mutableListOf<String>()
        val actions = CaptureInboxActions(
            onOpen = {},
            onClose = { events += "close" },
            onSelectType = { type ->
                events += "type:$type"
                state = state.copy(selection = state.selection!!.copy(type = type))
            },
            onSelectWallet = { events += "wallet:$it" },
            onSelectToWallet = { events += "to:$it" },
            onConfirm = { events += "confirm" },
            onDismiss = { events += "dismiss" },
        )
        composeRule.setContent { OkaneTheme { CaptureReviewSheet(state = state, actions = actions) } }

        composeRule.onNodeWithTag("captureDuplicateNote").assertIsDisplayed()
        composeRule.onNodeWithTag("captureToWallet-2").assertExists()

        composeRule.onNodeWithTag("captureType-OUT").performScrollTo().performClick()
        // A plain expense has no destination wallet.
        composeRule.onNodeWithTag("captureToWallet-2").assertDoesNotExist()

        composeRule.onNodeWithTag("captureConfirm").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(listOf("type:OUT", "confirm"), events) }
    }

    @Test
    fun confirmWaitsUntilTheDirectionIsChosen() {
        composeRule.setContent {
            OkaneTheme {
                CaptureReviewSheet(
                    state = CaptureInboxUiState(
                        cards = listOf(card.copy(proposedType = null)),
                        wallets = wallets,
                        sheetOpen = true,
                        selection = ReviewSelection(type = null, walletId = 3, toWalletId = null),
                    ),
                    actions = CaptureInboxActions.None,
                )
            }
        }
        composeRule.onNodeWithTag("captureConfirm").performScrollTo().assertIsNotEnabled()
    }
}
