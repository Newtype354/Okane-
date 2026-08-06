package com.example.okane.ui.transactions

import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class HistoryPeriod(val label: String) {
    TODAY("Hari ini"),
    LAST_SEVEN_DAYS("7 hari terakhir"),
    THIS_MONTH("Bulan ini"),
    LAST_MONTH("Bulan lalu"),
    ALL_TIME("Semua waktu"),
}

enum class HistoryTypeFilter(
    val label: String,
    val transactionType: TransactionType?,
) {
    ALL("Semua jenis", null),
    IN("Masuk", TransactionType.IN),
    OUT("Keluar", TransactionType.OUT),
    TRANSFER("Transfer", TransactionType.TRANSFER),
}

data class HistoryFilters(
    val period: HistoryPeriod = HistoryPeriod.THIS_MONTH,
    val type: HistoryTypeFilter = HistoryTypeFilter.ALL,
    val onlyUncategorised: Boolean = false,
)

data class HistorySummary(
    val income: Long = 0,
    val expense: Long = 0,
) {
    val net: Long get() = income - expense
}

data class HistoryTransactionItem(
    val transaction: TransactionEntity,
    val title: String,
    val walletName: String,
    val destinationWalletName: String?,
    val categoryName: String?,
) {
    val isUncategorised: Boolean
        get() = transaction.categoryId == null && transaction.type != TransactionType.TRANSFER

    val categoryLabel: String?
        get() = when {
            transaction.type == TransactionType.TRANSFER -> null
            categoryName != null -> categoryName
            transaction.categoryId == null -> "Perlu kategori"
            else -> "Kategori tidak ditemukan"
        }
}

data class HistoryGroup(
    val date: LocalDate,
    val transactions: List<HistoryTransactionItem>,
)

data class HistoryCategoryPickerState(
    val transaction: HistoryTransactionItem,
    val categories: List<CategoryEntity>,
)

data class HistoryUiState(
    val filters: HistoryFilters = HistoryFilters(),
    val summary: HistorySummary = HistorySummary(),
    val groups: List<HistoryGroup> = emptyList(),
    val totalTransactionCount: Int = 0,
    val visibleTransactionCount: Int = 0,
    val uncategorisedCount: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val detailTransaction: HistoryTransactionItem? = null,
    val categoryPicker: HistoryCategoryPickerState? = null,
    val categoryUpdateInProgress: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasOpenModal: Boolean
        get() = detailTransaction != null || categoryPicker != null
}

data class HistoryActions(
    val onPeriodSelected: (HistoryPeriod) -> Unit,
    val onTypeSelected: (HistoryTypeFilter) -> Unit,
    val onShowUncategorised: () -> Unit,
    val onShowAllTransactions: () -> Unit,
    val onResetFilters: () -> Unit,
    val onOpenDetail: (Long) -> Unit,
    val onDismissDetail: () -> Unit,
    val onOpenCategoryPicker: (Long) -> Unit,
    val onDismissCategoryPicker: () -> Unit,
    val onAssignCategory: (Long, Long) -> Unit,
)

object HistoryPresenter {

    fun present(
        transactions: List<TransactionEntity>,
        wallets: List<WalletEntity>,
        categories: List<CategoryEntity>,
        filters: HistoryFilters = HistoryFilters(),
        detailTransactionId: Long? = null,
        categoryPickerTransactionId: Long? = null,
        categoryUpdateInProgress: Boolean = false,
        errorMessage: String? = null,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HistoryUiState {
        val walletNames = wallets.associate { it.id to it.name }
        val categoriesById = categories.associateBy { it.id }
        val allItems = transactions.associate { transaction ->
            transaction.id to transaction.toHistoryItem(walletNames, categoriesById)
        }
        val range = filters.period.range(now, zoneId)
        val filtered = transactions
            .asSequence()
            .filter { transaction ->
                range == null ||
                    transaction.occurredAt >= range.startInclusive &&
                    transaction.occurredAt < range.endExclusive
            }
            .filter { transaction ->
                filters.type.transactionType == null ||
                    transaction.type == filters.type.transactionType
            }
            .filter { transaction ->
                !filters.onlyUncategorised ||
                    transaction.categoryId == null &&
                    transaction.type != TransactionType.TRANSFER
            }
            .sortedWith(
                compareByDescending<TransactionEntity> { it.occurredAt }
                    .thenByDescending { it.id }
            )
            .toList()

        var income = 0L
        var expense = 0L
        filtered.forEach { transaction ->
            when (transaction.type) {
                TransactionType.IN -> income += transaction.amount
                TransactionType.OUT -> expense += transaction.amount
                TransactionType.TRANSFER -> Unit
            }
        }

        val grouped = filtered
            .mapNotNull { allItems[it.id] }
            .groupBy { item ->
                Instant.ofEpochMilli(item.transaction.occurredAt)
                    .atZone(zoneId)
                    .toLocalDate()
            }
            .map { (date, items) -> HistoryGroup(date, items) }

        val pickerTransaction = categoryPickerTransactionId?.let(allItems::get)
            ?.takeIf { it.transaction.type != TransactionType.TRANSFER }
        val picker = pickerTransaction?.let { item ->
            HistoryCategoryPickerState(
                transaction = item,
                categories = categories.filter { category ->
                    !category.isArchived && category.type == item.transaction.type
                }
            )
        }

        return HistoryUiState(
            filters = filters,
            summary = HistorySummary(income = income, expense = expense),
            groups = grouped,
            totalTransactionCount = transactions.size,
            visibleTransactionCount = filtered.size,
            uncategorisedCount = transactions.count {
                it.categoryId == null && it.type != TransactionType.TRANSFER
            },
            today = now.atZone(zoneId).toLocalDate(),
            detailTransaction = detailTransactionId?.let(allItems::get),
            categoryPicker = picker,
            categoryUpdateInProgress = categoryUpdateInProgress,
            errorMessage = errorMessage,
        )
    }

    private fun TransactionEntity.toHistoryItem(
        walletNames: Map<Long, String>,
        categoriesById: Map<Long, CategoryEntity>,
    ): HistoryTransactionItem {
        val walletName = walletNames[walletId] ?: "Dompet tidak ditemukan"
        val destinationName = toWalletId?.let { walletNames[it] ?: "Dompet tidak ditemukan" }
        val categoryName = categoryId?.let(categoriesById::get)?.name
        return HistoryTransactionItem(
            transaction = this,
            title = historyTitle(categoryName, destinationName),
            walletName = walletName,
            destinationWalletName = destinationName,
            categoryName = categoryName,
        )
    }

    private fun TransactionEntity.historyTitle(
        categoryName: String?,
        destinationWalletName: String?,
    ): String =
        merchant.nonBlank()
            ?: categoryName
            ?: sourceApp.nonBlank()
            ?: note.safeTitle()
            ?: when (type) {
                TransactionType.IN -> "Transaksi masuk"
                TransactionType.OUT -> "Transaksi keluar"
                TransactionType.TRANSFER -> destinationWalletName
                    ?.let { "Transfer ke $it" }
                    ?: "Transfer"
            }

    private fun String?.nonBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private fun String?.safeTitle(): String? {
        val firstLine = this?.trim()?.lineSequence()?.firstOrNull()?.trim().orEmpty()
        return firstLine.takeIf { it.isNotEmpty() && it.length <= SAFE_NOTE_TITLE_LENGTH }
    }

    private fun HistoryPeriod.range(now: Instant, zoneId: ZoneId): EpochRange? {
        val today = now.atZone(zoneId).toLocalDate()
        val startDate: LocalDate
        val endDate: LocalDate
        when (this) {
            HistoryPeriod.TODAY -> {
                startDate = today
                endDate = today.plusDays(1)
            }

            HistoryPeriod.LAST_SEVEN_DAYS -> {
                startDate = today.minusDays(6)
                endDate = today.plusDays(1)
            }

            HistoryPeriod.THIS_MONTH -> {
                startDate = YearMonth.from(today).atDay(1)
                endDate = startDate.plusMonths(1)
            }

            HistoryPeriod.LAST_MONTH -> {
                endDate = YearMonth.from(today).atDay(1)
                startDate = endDate.minusMonths(1)
            }

            HistoryPeriod.ALL_TIME -> return null
        }
        return EpochRange(
            startInclusive = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endExclusive = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )
    }

    private data class EpochRange(
        val startInclusive: Long,
        val endExclusive: Long,
    )

    private const val SAFE_NOTE_TITLE_LENGTH = 48
}
