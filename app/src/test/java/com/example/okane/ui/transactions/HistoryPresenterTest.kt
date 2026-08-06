package com.example.okane.ui.transactions

import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPresenterTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = at(2026, 8, 5, 12, 0)
    private val wallets = listOf(
        WalletEntity(id = 1, name = "BCA"),
        WalletEntity(id = 2, name = "GoPay"),
    )

    @Test
    fun todayUsesLocalHalfOpenOccurredAtRange() {
        val state = present(
            period = HistoryPeriod.TODAY,
            transactions = listOf(
                transaction(1, occurredAt = at(2026, 8, 5, 0, 0)),
                transaction(2, occurredAt = at(2026, 8, 5, 23, 59)),
                transaction(3, occurredAt = at(2026, 8, 4, 23, 59)),
                transaction(4, occurredAt = at(2026, 8, 6, 0, 0)),
            ),
        )

        assertEquals(listOf(2L, 1L), state.visibleIds())
    }

    @Test
    fun lastSevenDaysIncludesTodayAndSixPreviousCalendarDays() {
        val state = present(
            period = HistoryPeriod.LAST_SEVEN_DAYS,
            transactions = listOf(
                transaction(1, occurredAt = at(2026, 7, 30, 0, 0)),
                transaction(2, occurredAt = at(2026, 8, 5, 23, 59)),
                transaction(3, occurredAt = at(2026, 7, 29, 23, 59)),
                transaction(4, occurredAt = at(2026, 8, 6, 0, 0)),
            ),
        )

        assertEquals(listOf(2L, 1L), state.visibleIds())
    }

    @Test
    fun thisMonthUsesCalendarMonthBoundaries() {
        val state = present(
            period = HistoryPeriod.THIS_MONTH,
            transactions = listOf(
                transaction(1, occurredAt = at(2026, 8, 1, 0, 0)),
                transaction(2, occurredAt = at(2026, 8, 31, 23, 59)),
                transaction(3, occurredAt = at(2026, 7, 31, 23, 59)),
                transaction(4, occurredAt = at(2026, 9, 1, 0, 0)),
            ),
        )

        assertEquals(listOf(2L, 1L), state.visibleIds())
    }

    @Test
    fun lastMonthHandlesYearRollover() {
        val januaryNow = at(2026, 1, 15, 12, 0)
        val state = present(
            period = HistoryPeriod.LAST_MONTH,
            now = januaryNow,
            transactions = listOf(
                transaction(1, occurredAt = at(2025, 12, 1, 0, 0)),
                transaction(2, occurredAt = at(2025, 12, 31, 23, 59)),
                transaction(3, occurredAt = at(2025, 11, 30, 23, 59)),
                transaction(4, occurredAt = at(2026, 1, 1, 0, 0)),
            ),
        )

        assertEquals(listOf(2L, 1L), state.visibleIds())
    }

    @Test
    fun allTimeHasNoTimeBoundary() {
        val state = present(
            period = HistoryPeriod.ALL_TIME,
            transactions = listOf(
                transaction(1, occurredAt = at(2010, 1, 1, 0, 0)),
                transaction(2, occurredAt = at(2040, 1, 1, 0, 0)),
            ),
        )

        assertEquals(listOf(2L, 1L), state.visibleIds())
    }

    @Test
    fun incomeFilterOnlyIncludesIncome() {
        assertEquals(
            listOf(1L),
            present(
                type = HistoryTypeFilter.IN,
                transactions = transactionsOfEveryType(),
            ).visibleIds(),
        )
    }

    @Test
    fun expenseFilterOnlyIncludesExpense() {
        assertEquals(
            listOf(2L),
            present(
                type = HistoryTypeFilter.OUT,
                transactions = transactionsOfEveryType(),
            ).visibleIds(),
        )
    }

    @Test
    fun transferFilterOnlyIncludesTransferAndHasZeroSummary() {
        val state = present(
            type = HistoryTypeFilter.TRANSFER,
            transactions = transactionsOfEveryType(),
        )

        assertEquals(listOf(3L), state.visibleIds())
        assertEquals(HistorySummary(), state.summary)
    }

    @Test
    fun periodAndTypeFiltersCompose() {
        val state = present(
            period = HistoryPeriod.TODAY,
            type = HistoryTypeFilter.OUT,
            transactions = listOf(
                transaction(1, type = TransactionType.OUT, occurredAt = at(2026, 8, 5, 9, 0)),
                transaction(2, type = TransactionType.IN, occurredAt = at(2026, 8, 5, 10, 0)),
                transaction(3, type = TransactionType.OUT, occurredAt = at(2026, 8, 4, 10, 0)),
            ),
        )

        assertEquals(listOf(1L), state.visibleIds())
    }

    @Test
    fun summaryExcludesTransferAndCanBePositive() {
        val state = present(
            transactions = listOf(
                transaction(1, amount = 100, type = TransactionType.IN),
                transaction(2, amount = 30, type = TransactionType.OUT),
                transaction(3, amount = 5_000, type = TransactionType.TRANSFER),
            ),
        )

        assertEquals(100L, state.summary.income)
        assertEquals(30L, state.summary.expense)
        assertEquals(70L, state.summary.net)
    }

    @Test
    fun summaryCanBeNegative() {
        val state = present(
            transactions = listOf(
                transaction(1, amount = 20, type = TransactionType.IN),
                transaction(2, amount = 100, type = TransactionType.OUT),
            ),
        )

        assertEquals(-80L, state.summary.net)
    }

    @Test
    fun groupingAndOrderingUseOccurredAtNotCreatedAt() {
        val createdAt = at(2026, 8, 5, 12, 0)
        val state = present(
            transactions = listOf(
                transaction(
                    1,
                    occurredAt = at(2026, 8, 3, 14, 0),
                    createdAt = createdAt,
                ),
                transaction(
                    2,
                    occurredAt = at(2026, 8, 5, 8, 0),
                    createdAt = at(2020, 1, 1, 0, 0),
                ),
                transaction(
                    3,
                    occurredAt = at(2026, 8, 3, 18, 0),
                    createdAt = createdAt,
                ),
            ),
        )

        assertEquals(
            listOf(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 3)),
            state.groups.map(HistoryGroup::date),
        )
        assertEquals(listOf(3L, 1L), state.groups[1].transactions.map { it.transaction.id })
    }

    @Test
    fun uncategorisedModeIncludesOnlyNullNonTransferTransactions() {
        val state = HistoryPresenter.present(
            transactions = listOf(
                transaction(1, type = TransactionType.OUT, categoryId = null),
                transaction(2, type = TransactionType.IN, categoryId = 10),
                transaction(3, type = TransactionType.TRANSFER, categoryId = null),
            ),
            wallets = wallets,
            categories = listOf(
                CategoryEntity(id = 10, name = "Gaji", type = TransactionType.IN)
            ),
            filters = HistoryFilters(
                period = HistoryPeriod.ALL_TIME,
                onlyUncategorised = true,
            ),
            now = now,
            zoneId = zone,
        )

        assertEquals(1, state.uncategorisedCount)
        assertEquals(listOf(1L), state.visibleIds())
        assertFalse(state.groups.single().transactions.single().transaction.type == TransactionType.TRANSFER)
    }

    @Test
    fun archivedReferencesResolveButPickerOnlyOffersActiveMatchingCategories() {
        val archivedWallet = WalletEntity(id = 1, name = "BCA Lama", isArchived = true)
        val archivedCategory = CategoryEntity(
            id = 10,
            name = "Makan Lama",
            type = TransactionType.OUT,
            isArchived = true,
        )
        val activeExpense = CategoryEntity(
            id = 11,
            name = "Makan",
            type = TransactionType.OUT,
        )
        val activeIncome = CategoryEntity(
            id = 12,
            name = "Gaji",
            type = TransactionType.IN,
        )
        val categorised = transaction(
            id = 1,
            type = TransactionType.OUT,
            categoryId = archivedCategory.id,
        )
        val needsCategory = transaction(
            id = 2,
            type = TransactionType.OUT,
            categoryId = null,
        )
        val state = HistoryPresenter.present(
            transactions = listOf(categorised, needsCategory),
            wallets = listOf(archivedWallet),
            categories = listOf(archivedCategory, activeExpense, activeIncome),
            filters = HistoryFilters(period = HistoryPeriod.ALL_TIME),
            detailTransactionId = categorised.id,
            categoryPickerTransactionId = needsCategory.id,
            now = now,
            zoneId = zone,
        )

        assertEquals("BCA Lama", state.detailTransaction?.walletName)
        assertEquals("Makan Lama", state.detailTransaction?.categoryName)
        assertEquals(listOf(activeExpense), state.categoryPicker?.categories)
        assertTrue(state.categoryPicker?.transaction?.isUncategorised == true)
    }

    @Test
    fun transferNeverCreatesCategoryPicker() {
        val transfer = transaction(1, type = TransactionType.TRANSFER)
        val state = HistoryPresenter.present(
            transactions = listOf(transfer),
            wallets = wallets,
            categories = emptyList(),
            filters = HistoryFilters(period = HistoryPeriod.ALL_TIME),
            categoryPickerTransactionId = transfer.id,
            now = now,
            zoneId = zone,
        )

        assertNull(state.categoryPicker)
        assertEquals(0, state.uncategorisedCount)
    }

    @Test
    fun titleFallbacksAvoidLongRawNotes() {
        val category = CategoryEntity(id = 10, name = "Makan", type = TransactionType.OUT)
        val fromCategory = transaction(1, categoryId = category.id)
        val fromSource = transaction(2).copy(sourceApp = "GoPay")
        val fromShortNote = transaction(3).copy(note = "Makan bersama")
        val fromLongRawNote = transaction(4).copy(note = "x".repeat(80))
        val state = HistoryPresenter.present(
            transactions = listOf(fromCategory, fromSource, fromShortNote, fromLongRawNote),
            wallets = wallets,
            categories = listOf(category),
            filters = HistoryFilters(period = HistoryPeriod.ALL_TIME),
            now = now,
            zoneId = zone,
        )
        val titles = state.groups.flatMap(HistoryGroup::transactions)
            .associate { it.transaction.id to it.title }

        assertEquals("Makan", titles[1])
        assertEquals("GoPay", titles[2])
        assertEquals("Makan bersama", titles[3])
        assertEquals("Transaksi keluar", titles[4])
    }

    private fun present(
        period: HistoryPeriod = HistoryPeriod.THIS_MONTH,
        type: HistoryTypeFilter = HistoryTypeFilter.ALL,
        transactions: List<TransactionEntity>,
        now: Instant = this.now,
    ): HistoryUiState = HistoryPresenter.present(
        transactions = transactions,
        wallets = wallets,
        categories = emptyList(),
        filters = HistoryFilters(period = period, type = type),
        now = now,
        zoneId = zone,
    )

    private fun transactionsOfEveryType(): List<TransactionEntity> = listOf(
        transaction(1, type = TransactionType.IN, occurredAt = at(2026, 8, 5, 11, 0)),
        transaction(2, type = TransactionType.OUT, occurredAt = at(2026, 8, 5, 10, 0)),
        transaction(3, type = TransactionType.TRANSFER, occurredAt = at(2026, 8, 5, 9, 0)),
    )

    private fun transaction(
        id: Long,
        amount: Long = 1_000,
        type: TransactionType = TransactionType.OUT,
        categoryId: Long? = null,
        occurredAt: Instant = at(2026, 8, 5, 10, 0),
        createdAt: Instant = occurredAt,
    ): TransactionEntity = TransactionEntity(
        id = id,
        amount = amount,
        type = type,
        walletId = 1,
        toWalletId = if (type == TransactionType.TRANSFER) 2 else null,
        categoryId = categoryId,
        occurredAt = occurredAt.toEpochMilli(),
        createdAt = createdAt.toEpochMilli(),
        source = TransactionSource.MANUAL,
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

    private fun HistoryUiState.visibleIds(): List<Long> =
        groups.flatMap { group -> group.transactions.map { it.transaction.id } }
}
