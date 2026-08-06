package com.example.okane.ui.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.theme.OkaneTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = at(2026, 8, 5, 13, 0)
    private val wallets = listOf(
        WalletEntity(id = 1, name = "BCA"),
        WalletEntity(id = 2, name = "GoPay"),
    )
    private val categories = listOf(
        CategoryEntity(id = 1, name = "Makan", type = TransactionType.OUT),
        CategoryEntity(id = 2, name = "Gaji", type = TransactionType.IN),
        CategoryEntity(
            id = 3,
            name = "Lama",
            type = TransactionType.OUT,
            isArchived = true,
        ),
    )
    private var transactions by mutableStateOf(sampleTransactions())
    private var filters by mutableStateOf(HistoryFilters())
    private var detailId by mutableStateOf<Long?>(null)
    private var pickerId by mutableStateOf<Long?>(null)

    @Before
    fun setUp() {
        val actions = HistoryActions(
            onPeriodSelected = { filters = filters.copy(period = it) },
            onTypeSelected = { filters = filters.copy(type = it) },
            onShowUncategorised = {
                filters = HistoryFilters(
                    period = HistoryPeriod.ALL_TIME,
                    type = HistoryTypeFilter.ALL,
                    onlyUncategorised = true,
                )
            },
            onShowAllTransactions = {
                filters = filters.copy(onlyUncategorised = false)
            },
            onResetFilters = { filters = HistoryFilters() },
            onOpenDetail = {
                pickerId = null
                detailId = it
            },
            onDismissDetail = { detailId = null },
            onOpenCategoryPicker = {
                detailId = null
                pickerId = it
            },
            onDismissCategoryPicker = { pickerId = null },
            onAssignCategory = { transactionId, categoryId ->
                transactions = transactions.map { transaction ->
                    if (transaction.id == transactionId) {
                        transaction.copy(categoryId = categoryId)
                    } else {
                        transaction
                    }
                }
                pickerId = null
            },
        )

        composeRule.setContent {
            OkaneTheme {
                TransactionListScreen(
                    state = HistoryPresenter.present(
                        transactions = transactions,
                        wallets = wallets,
                        categories = categories,
                        filters = filters,
                        detailTransactionId = detailId,
                        categoryPickerTransactionId = pickerId,
                        now = now,
                        zoneId = zone,
                    ),
                    actions = actions,
                    onAddTransaction = {},
                )
            }
        }
    }

    @Test
    fun filtersUpdateSummaryAndResetRecoversFromEmptyResult() {
        composeRule.onNodeWithTag("historyIncome").assertTextEquals("Rp2.500.000")
        composeRule.onNodeWithTag("historyExpense").assertTextEquals("Rp1.850.000")
        composeRule.onNodeWithTag("historyNet").assertTextEquals("+Rp650.000")

        selectType(HistoryTypeFilter.OUT)
        composeRule.onNodeWithTag("historyIncome").assertTextEquals("Rp0")
        composeRule.onNodeWithTag("historyExpense").assertTextEquals("Rp1.850.000")

        selectPeriod(HistoryPeriod.LAST_MONTH)
        selectType(HistoryTypeFilter.TRANSFER)
        composeRule.onNodeWithText("Tidak ada transaksi yang cocok").assertIsDisplayed()

        composeRule.onNodeWithTag("historyEmptyAction").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("historyIncome").assertTextEquals("Rp2.500.000")
        composeRule.onNodeWithText("Tidak ada transaksi yang cocok").assertDoesNotExist()
    }

    @Test
    fun rowOpensDetailAndCategoryPickerUpdatesRecoveryState() {
        composeRule.onNodeWithTag("transactionRow-2").performClick()
        composeRule.onNodeWithTag("transactionDetailSheet").assertExists()
        composeRule.onNodeWithText("Notifikasi").assertExists()

        composeRule.onNodeWithTag("detailCategoryAction").performClick()
        composeRule.onNodeWithTag("historyCategoryPicker").assertExists()
        composeRule.onNodeWithTag("historyCategory-1").assertExists()
        composeRule.onNodeWithTag("historyCategory-2").assertDoesNotExist()
        composeRule.onNodeWithTag("historyCategory-3").assertDoesNotExist()

        composeRule.onNodeWithTag("historyCategory-1").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("historyCategoryPicker").assertDoesNotExist()
        composeRule.onNodeWithText("Makan · GoPay").assertIsDisplayed()
        composeRule.onNodeWithText("1 transaksi perlu kategori").assertIsDisplayed()
    }

    @Test
    fun uncategorisedEntryFindsRowsOutsideCurrentPeriod() {
        composeRule.onNodeWithText("Transaksi lama tanpa kategori").assertDoesNotExist()

        composeRule.onNodeWithTag("uncategorisedEntry").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Hanya perlu kategori · 2 tersisa").assertIsDisplayed()
        composeRule.onNodeWithTag("transactionList")
            .performScrollToNode(hasText("Transaksi lama tanpa kategori"))
        composeRule.onNodeWithText("Transaksi lama tanpa kategori").assertIsDisplayed()
        composeRule.onNodeWithTag("transactionRow-1").assertDoesNotExist()
        composeRule.onNodeWithTag("transactionRow-3").assertDoesNotExist()
    }

    private fun selectPeriod(period: HistoryPeriod) {
        composeRule.onNodeWithTag("historyPeriodFilter").performClick()
        composeRule.onNodeWithTag("historyPeriod-${period.name}").performClick()
        composeRule.waitForIdle()
    }

    private fun selectType(type: HistoryTypeFilter) {
        composeRule.onNodeWithTag("historyTypeFilter").performClick()
        composeRule.onNodeWithTag("historyType-${type.name}").performClick()
        composeRule.waitForIdle()
    }

    private fun sampleTransactions(): List<TransactionEntity> = listOf(
        transaction(
            id = 1,
            amount = 2_500_000,
            type = TransactionType.IN,
            walletId = 1,
            categoryId = 2,
            occurredAt = at(2026, 8, 5, 8, 0),
            merchant = "Gaji magang",
        ),
        transaction(
            id = 2,
            amount = 1_850_000,
            type = TransactionType.OUT,
            walletId = 2,
            categoryId = null,
            occurredAt = at(2026, 8, 5, 12, 30),
            source = TransactionSource.NOTIFICATION,
            sourceApp = "GoPay",
            merchant = "Makan siang",
        ),
        transaction(
            id = 3,
            amount = 200_000,
            type = TransactionType.TRANSFER,
            walletId = 1,
            toWalletId = 2,
            occurredAt = at(2026, 8, 5, 9, 20),
        ),
        transaction(
            id = 4,
            amount = 80_000,
            type = TransactionType.OUT,
            walletId = 1,
            categoryId = 1,
            occurredAt = at(2026, 7, 20, 10, 0),
            merchant = "Belanja bulan lalu",
        ),
        transaction(
            id = 5,
            amount = 30_000,
            type = TransactionType.OUT,
            walletId = 1,
            categoryId = null,
            occurredAt = at(2026, 7, 15, 10, 0),
            merchant = "Transaksi lama tanpa kategori",
        ),
    )

    private fun transaction(
        id: Long,
        amount: Long,
        type: TransactionType,
        walletId: Long,
        categoryId: Long? = null,
        toWalletId: Long? = null,
        occurredAt: Instant,
        source: TransactionSource = TransactionSource.MANUAL,
        sourceApp: String? = null,
        merchant: String? = null,
    ): TransactionEntity = TransactionEntity(
        id = id,
        amount = amount,
        type = type,
        walletId = walletId,
        toWalletId = toWalletId,
        categoryId = categoryId,
        occurredAt = occurredAt.toEpochMilli(),
        createdAt = now.toEpochMilli(),
        source = source,
        sourceApp = sourceApp,
        merchant = merchant,
    )

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Instant = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zone)
        .toInstant()
}
