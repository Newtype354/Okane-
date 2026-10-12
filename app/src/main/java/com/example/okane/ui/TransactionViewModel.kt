package com.example.okane.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletBalance
import com.example.okane.data.local.WalletEntity
import com.example.okane.data.repository.CategoryRepository
import com.example.okane.data.repository.TransactionRepository
import com.example.okane.data.repository.WalletRepository
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.transactions.HistoryFilters
import com.example.okane.ui.transactions.HistoryPeriod
import com.example.okane.ui.transactions.HistoryPresenter
import com.example.okane.ui.transactions.HistoryTypeFilter
import com.example.okane.ui.transactions.HistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    walletRepository: WalletRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    // These existing streams remain available to other app surfaces.
    val transactions: StateFlow<List<TransactionEntity>> =
        transactionRepository.transactions.asState(emptyList())

    /** Computed per wallet, including archived rows; never stored. */
    val walletBalances: StateFlow<List<WalletBalance>> =
        walletRepository.balances.asState(emptyList())

    /** Includes archived wallets so historical references still resolve. */
    val wallets: StateFlow<List<WalletEntity>> =
        walletRepository.all.asState(emptyList())

    /** Includes archived categories for the same historical name resolution. */
    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.all.asState(emptyList())

    val uncategorised: StateFlow<List<TransactionEntity>> =
        transactionRepository.uncategorised.asState(emptyList())

    val recent: StateFlow<List<TransactionEntity>> =
        transactionRepository.recent(RECENT_LIMIT).asState(emptyList())

    private val historyData: StateFlow<HistoryData> = combine(
        transactions,
        wallets,
        categories,
    ) { transactionRows, walletRows, categoryRows ->
        HistoryData(transactionRows, walletRows, categoryRows)
    }.asState(HistoryData())

    private val _historyFilters = MutableStateFlow(HistoryFilters())
    private val _detailTransactionId = MutableStateFlow<Long?>(null)
    private val _categoryPickerTransactionId = MutableStateFlow<Long?>(null)
    private val _categoryUpdateInProgress = MutableStateFlow(false)
    private val _historyError = MutableStateFlow<String?>(null)

    private val historySelection = combine(
        _detailTransactionId,
        _categoryPickerTransactionId,
    ) { detailId, pickerId ->
        HistorySelection(detailId, pickerId)
    }

    private val historyOperation = combine(
        _categoryUpdateInProgress,
        _historyError,
    ) { updating, error ->
        HistoryOperation(updating, error)
    }

    /**
     * History filtering, reference resolution, summary, and grouping happen on
     * Flow emissions rather than in Compose recompositions. A midnight emission
     * keeps calendar filters correct while the app remains open.
     */
    val historyUiState: StateFlow<HistoryUiState> = combine(
        historyData,
        _historyFilters,
        historySelection,
        historyOperation,
        localDayTicker(),
    ) { data, filters, selection, operation, now ->
        HistoryPresenter.present(
            transactions = data.transactions,
            wallets = data.wallets,
            categories = data.categories,
            filters = filters,
            detailTransactionId = selection.detailTransactionId,
            categoryPickerTransactionId = selection.categoryPickerTransactionId,
            categoryUpdateInProgress = operation.categoryUpdateInProgress,
            errorMessage = operation.errorMessage,
            now = now,
            zoneId = ZoneId.systemDefault(),
        )
    }.asState(HistoryUiState())

    fun selectHistoryPeriod(period: HistoryPeriod) {
        _historyFilters.update { it.copy(period = period) }
    }

    fun selectHistoryType(type: HistoryTypeFilter) {
        _historyFilters.update { it.copy(type = type) }
    }

    /**
     * The recovery entry point is global so old uncategorised rows cannot be
     * hidden by the current month/type filters.
     */
    fun showUncategorisedHistory() {
        _historyFilters.value = HistoryFilters(
            period = HistoryPeriod.ALL_TIME,
            type = HistoryTypeFilter.ALL,
            onlyUncategorised = true,
        )
    }

    fun showAllHistoryTransactions() {
        _historyFilters.update { it.copy(onlyUncategorised = false) }
    }

    fun resetHistoryFilters() {
        _historyFilters.value = HistoryFilters()
    }

    fun openTransactionDetail(transactionId: Long) {
        if (historyData.value.transactions.none { it.id == transactionId }) return
        _categoryPickerTransactionId.value = null
        _detailTransactionId.value = transactionId
        _historyError.value = null
    }

    fun dismissTransactionDetail() {
        _detailTransactionId.value = null
    }

    fun openHistoryCategoryPicker(transactionId: Long) {
        val transaction = historyData.value.transactions.firstOrNull { it.id == transactionId }
            ?: return
        if (transaction.type == TransactionType.TRANSFER) return
        _detailTransactionId.value = null
        _categoryPickerTransactionId.value = transactionId
        _historyError.value = null
    }

    fun dismissHistoryCategoryPicker() {
        if (_categoryUpdateInProgress.value) return
        _categoryPickerTransactionId.value = null
        _historyError.value = null
    }

    /** Inbox tidy-up with direction and archive validation at the UI boundary. */
    fun assignCategory(transactionId: Long, categoryId: Long) {
        if (_categoryUpdateInProgress.value) return
        val data = historyData.value
        val transaction = data.transactions.firstOrNull { it.id == transactionId }
        val category = data.categories.firstOrNull { it.id == categoryId }
        if (
            transaction == null ||
            transaction.type == TransactionType.TRANSFER ||
            category == null ||
            category.isArchived ||
            category.type != transaction.type
        ) {
            _historyError.value = "Kategori tidak dapat digunakan untuk transaksi ini."
            return
        }

        _categoryUpdateInProgress.value = true
        _historyError.value = null
        viewModelScope.launch {
            try {
                if (transactionRepository.setCategory(transactionId, categoryId)) {
                    _categoryPickerTransactionId.value = null
                } else {
                    _historyError.value = "Transaksi tidak ditemukan. Muat ulang dan coba lagi."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _historyError.value = "Kategori gagal diperbarui. Coba lagi."
            } finally {
                _categoryUpdateInProgress.value = false
            }
        }
    }

    private fun localDayTicker(): Flow<Instant> = flow {
        while (true) {
            val now = Instant.now()
            emit(now)
            val zone = ZoneId.systemDefault()
            val nextDay = now.atZone(zone)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
            val waitMillis = Duration.between(now, nextDay).toMillis()
                .coerceAtLeast(MINIMUM_TICK_DELAY_MILLIS)
            delay(waitMillis + TICK_BOUNDARY_PADDING_MILLIS)
        }
    }

    private fun <T> Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    private data class HistoryData(
        val transactions: List<TransactionEntity> = emptyList(),
        val wallets: List<WalletEntity> = emptyList(),
        val categories: List<CategoryEntity> = emptyList(),
    )

    private data class HistorySelection(
        val detailTransactionId: Long?,
        val categoryPickerTransactionId: Long?,
    )

    private data class HistoryOperation(
        val categoryUpdateInProgress: Boolean,
        val errorMessage: String?,
    )

    private companion object {
        const val RECENT_LIMIT = 15
        const val MINIMUM_TICK_DELAY_MILLIS = 1_000L
        const val TICK_BOUNDARY_PADDING_MILLIS = 100L
    }
}
