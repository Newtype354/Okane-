package com.example.okane.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletBalance
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.transactions.HistoryActions
import com.example.okane.ui.transactions.HistoryFilters
import com.example.okane.ui.transactions.HistoryPeriod
import com.example.okane.ui.transactions.HistoryPresenter
import com.example.okane.ui.transactions.HistoryTypeFilter
import com.example.okane.ui.wallets.WalletActions
import com.example.okane.ui.wallets.WalletsUiState
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OkaneMainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var addTransactionOpened = false
    private var settingsOpened = false
    private var historyFilters by mutableStateOf(
        HistoryFilters(period = HistoryPeriod.ALL_TIME)
    )
    private val historyActions = HistoryActions(
        onPeriodSelected = { historyFilters = historyFilters.copy(period = it) },
        onTypeSelected = { historyFilters = historyFilters.copy(type = it) },
        onShowUncategorised = {
            historyFilters = HistoryFilters(
                period = HistoryPeriod.ALL_TIME,
                onlyUncategorised = true,
            )
        },
        onShowAllTransactions = {
            historyFilters = historyFilters.copy(onlyUncategorised = false)
        },
        onResetFilters = { historyFilters = HistoryFilters() },
        onOpenDetail = {},
        onDismissDetail = {},
        onOpenCategoryPicker = {},
        onDismissCategoryPicker = {},
        onAssignCategory = { _, _ -> },
    )

    @Before
    fun setUp() {
        composeRule.setContent {
            OkaneTheme {
                val historyNow = Instant.parse("2026-08-05T06:00:00Z")
                OkaneMainScreen(
                    historyUiState = HistoryPresenter.present(
                        transactions = List(30) { index ->
                            TransactionEntity(
                                id = index.toLong() + 1,
                                amount = (index.toLong() + 1) * 1_000,
                                type = TransactionType.OUT,
                                walletId = 1,
                                categoryId = null,
                                occurredAt = historyNow.minusSeconds(index.toLong() * 60)
                                    .toEpochMilli(),
                                createdAt = historyNow.toEpochMilli(),
                                source = TransactionSource.MANUAL,
                                merchant = "Merchant $index"
                            )
                        },
                        wallets = listOf(WalletEntity(id = 1, name = "Tunai")),
                        categories = emptyList(),
                        filters = historyFilters,
                        now = historyNow,
                        zoneId = ZoneId.of("Asia/Jakarta"),
                    ),
                    historyActions = historyActions,
                    walletsUiState = WalletsUiState(
                        isLoading = false,
                        totalMoney = 465_000,
                        activeWallets = List(30) { index ->
                            WalletBalance(
                                walletId = index.toLong() + 1,
                                name = "Wallet $index",
                                balance = (index.toLong() + 1) * 1_000,
                                isDefault = index == 0,
                                initialBalance = 0,
                                monthlyFee = 0,
                                isArchived = false,
                            )
                        },
                    ),
                    walletActions = walletActions(),
                    onAddTransaction = { addTransactionOpened = true }
                )
            }
        }
    }

    @Test
    fun launchAndSwipesKeepPagerAndNavigationInSync() {
        composeRule.onNodeWithTag("rootNavigation-DASHBOARD").assertIsSelected()

        composeRule.onNodeWithTag("rootPager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rootNavigation-DASHBOARD").assertIsNotSelected()
        composeRule.onNodeWithTag("rootNavigation-TRANSACTIONS").assertIsSelected()
        composeRule.onNodeWithTag("rootNavigation-WALLETS").assertIsNotSelected()

        composeRule.onNodeWithTag("rootPager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rootNavigation-WALLETS").assertIsSelected()

        composeRule.onNodeWithTag("rootPager").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rootNavigation-TRANSACTIONS").assertIsSelected()
    }

    @Test
    fun navigationTapAndBoundarySwipesStayOnValidPages() {
        composeRule.onNodeWithTag("rootNavigation-WALLETS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rootNavigation-WALLETS").assertIsSelected()

        composeRule.onNodeWithTag("rootPager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rootNavigation-WALLETS").assertIsSelected()

        composeRule.onNodeWithTag("rootNavigation-DASHBOARD").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rootNavigation-DASHBOARD").assertIsSelected()

        composeRule.onNodeWithTag("rootPager").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rootNavigation-DASHBOARD").assertIsSelected()
    }

    @Test
    fun walletGearOpensSettingsAndHomeFabStillOpensKeypadRoute() {
        composeRule.onNodeWithTag("rootNavigation-WALLETS").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("walletSettings").performClick()
        composeRule.runOnIdle {
            assertTrue(settingsOpened)
        }

        composeRule.onNodeWithTag("rootNavigation-DASHBOARD").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("homeAddTransaction").performClick()
        composeRule.runOnIdle {
            assertTrue(addTransactionOpened)
        }
    }

    @Test
    fun verticalScrollStateSurvivesRootPageChanges() {
        composeRule.onNodeWithTag("rootNavigation-TRANSACTIONS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("transactionList")
            .performScrollToNode(hasText("Merchant 29"))
        composeRule.onNodeWithText("Merchant 29").assertIsDisplayed()

        composeRule.onNodeWithTag("rootNavigation-WALLETS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("walletList")
            .performScrollToNode(hasText("Wallet 29"))
        composeRule.onNodeWithText("Wallet 29")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("rootNavigation-TRANSACTIONS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Merchant 29").assertIsDisplayed()

        composeRule.onNodeWithTag("rootNavigation-WALLETS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Wallet 29").assertIsDisplayed()
    }

    @Test
    fun historyFilterStateSurvivesRootPageChanges() {
        composeRule.onNodeWithTag("rootNavigation-TRANSACTIONS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("historyTypeFilter").performClick()
        composeRule.onNodeWithTag("historyType-${HistoryTypeFilter.IN.name}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rootNavigation-WALLETS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rootNavigation-TRANSACTIONS").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("historyTypeFilter").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Jenis transaksi: Masuk",
            )
        )
    }

    private fun walletActions() = WalletActions(
        onOpenSettings = { settingsOpened = true },
        onOpenAdd = {},
        onOpenDetail = { _, _ -> },
        onOpenArchived = {},
        onBack = {},
        onOpenEdit = {},
        onDismissEditor = {},
        onEditorNameChanged = {},
        onEditorInitialBalanceChanged = {},
        onEditorMonthlyFeeChanged = {},
        onEditorDefaultChanged = {},
        onSaveEditor = {},
        onMakeDefault = {},
        onRequestArchive = {},
        onDismissArchive = {},
        onConfirmArchive = {},
        onRestore = {},
        onFeedbackShown = {},
    )
}
