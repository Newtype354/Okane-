package com.example.okane.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.components.GlassSurface
import com.example.okane.ui.components.GlassVariant
import com.example.okane.ui.components.OkaneOutlinedButton
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.StatusChip
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.theme.ExpenseRed
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.theme.TransferBlue
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    state: HistoryUiState,
    actions: HistoryActions,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("transactionList"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "history-subtitle") {
            Text(
                text = "Semua catatan keuanganmu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        item(key = "history-filters") {
            HistoryFilters(state.filters, actions)
        }

        item(key = "history-summary") {
            HistorySummaryCard(state.summary, state.filters.period)
        }

        if (state.uncategorisedCount > 0 || state.filters.onlyUncategorised) {
            item(key = "history-uncategorised") {
                UncategorisedEntryPoint(state, actions)
            }
        }

        when {
            state.totalTransactionCount == 0 -> {
                item(key = "history-empty-database") {
                    HistoryEmptyState(
                        title = "Belum ada transaksi",
                        message = "Catatan transaksi yang kamu buat akan muncul di sini.",
                        actionLabel = "Catat transaksi",
                        onAction = onAddTransaction,
                    )
                }
            }

            state.visibleTransactionCount == 0 -> {
                item(key = "history-empty-filter") {
                    HistoryEmptyState(
                        title = "Tidak ada transaksi yang cocok",
                        message = "Coba ubah periode atau jenis transaksi.",
                        actionLabel = "Reset filter",
                        onAction = actions.onResetFilters,
                    )
                }
            }

            else -> {
                state.groups.forEach { group ->
                    item(key = "history-date-${group.date}") {
                        SectionHeader(
                            text = Formatters.historyDateLabel(group.date, state.today),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                    items(
                        items = group.transactions,
                        key = { item -> "transaction-${item.transaction.id}" },
                    ) { item ->
                        HistoryTransactionRow(
                            item = item,
                            onOpenDetail = {
                                actions.onOpenDetail(item.transaction.id)
                            },
                            onOpenCategoryPicker = {
                                actions.onOpenCategoryPicker(item.transaction.id)
                            },
                        )
                    }
                }
            }
        }
    }

    state.detailTransaction?.let { item ->
        TransactionDetailSheet(
            item = item,
            onDismiss = actions.onDismissDetail,
            onOpenCategoryPicker = {
                actions.onOpenCategoryPicker(item.transaction.id)
            },
        )
    }

    state.categoryPicker?.let { picker ->
        HistoryCategoryPickerSheet(
            state = picker,
            updateInProgress = state.categoryUpdateInProgress,
            errorMessage = state.errorMessage,
            onDismiss = actions.onDismissCategoryPicker,
            onCategorySelected = { categoryId ->
                actions.onAssignCategory(picker.transaction.transaction.id, categoryId)
            },
        )
    }
}

@Composable
private fun HistoryFilters(
    filters: HistoryFilters,
    actions: HistoryActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilterMenu(
            label = "Periode",
            selectedLabel = filters.period.label,
            options = HistoryPeriod.entries,
            optionLabel = HistoryPeriod::label,
            optionTag = { "historyPeriod-${it.name}" },
            onSelected = actions.onPeriodSelected,
            modifier = Modifier
                .weight(1f)
                .testTag("historyPeriodFilter"),
        )
        HistoryFilterMenu(
            label = "Jenis transaksi",
            selectedLabel = filters.type.label,
            options = HistoryTypeFilter.entries,
            optionLabel = HistoryTypeFilter::label,
            optionTag = { "historyType-${it.name}" },
            onSelected = actions.onTypeSelected,
            modifier = Modifier
                .weight(1f)
                .testTag("historyTypeFilter"),
        )
    }
}

@Composable
private fun <T> HistoryFilterMenu(
    label: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    optionTag: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .pointerInput(label) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                    }
                }
                .semantics {
                    stateDescription = "$label: $selectedLabel"
                },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Buka pilihan $label",
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                val optionText = optionLabel(option)
                val isSelected = optionText == selectedLabel
                DropdownMenuItem(
                    text = { Text(optionText) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                    modifier = Modifier
                        .testTag(optionTag(option))
                        .semantics { selected = isSelected },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = "Dipilih")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HistorySummaryCard(
    summary: HistorySummary,
    period: HistoryPeriod,
) {
    val fontScale = LocalDensity.current.fontScale
    val useStackedSummary =
        listOf(summary.income, summary.expense, summary.net)
            .maxOf { Formatters.netRupiah(it).length } > 16
    GlassSurface(
        variant = GlassVariant.Subtle,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Ringkasan · ${period.label}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BoxWithConstraints {
                if (maxWidth < 340.dp || fontScale >= 1.3f || useStackedSummary) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryMetric(
                            label = "Pemasukan",
                            amount = Formatters.rupiah(summary.income),
                            color = IncomeGreen,
                            testTag = "historyIncome",
                        )
                        SummaryMetric(
                            label = "Pengeluaran",
                            amount = Formatters.rupiah(summary.expense),
                            color = ExpenseRed,
                            testTag = "historyExpense",
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        SummaryMetric(
                            label = "Pemasukan",
                            amount = Formatters.rupiah(summary.income),
                            color = IncomeGreen,
                            testTag = "historyIncome",
                            modifier = Modifier.weight(1f),
                        )
                        SummaryMetric(
                            label = "Pengeluaran",
                            amount = Formatters.rupiah(summary.expense),
                            color = ExpenseRed,
                            testTag = "historyExpense",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            val netColor = when {
                summary.net > 0 -> IncomeGreen
                summary.net < 0 -> ExpenseRed
                else -> MaterialTheme.colorScheme.onSurface
            }
            SummaryMetric(
                label = "Selisih",
                amount = Formatters.netRupiah(summary.net),
                color = netColor,
                testTag = "historyNet",
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    amount: String,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun UncategorisedEntryPoint(
    state: HistoryUiState,
    actions: HistoryActions,
) {
    val isSelected = state.filters.onlyUncategorised
    Surface(
        onClick = if (isSelected) {
            actions.onShowAllTransactions
        } else {
            actions.onShowUncategorised
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("uncategorisedEntry")
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) {
                    "Mode transaksi perlu kategori aktif"
                } else {
                    "Mode transaksi perlu kategori tidak aktif"
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    state.uncategorisedCount == 0 -> "Semua transaksi sudah berkategori"
                    isSelected -> "Hanya perlu kategori · ${state.uncategorisedCount} tersisa"
                    else -> "${state.uncategorisedCount} transaksi perlu kategori"
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (isSelected) "Tampilkan semua" else "Periksa",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    GlassSurface(
        variant = GlassVariant.Subtle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("historyEmptyState"),
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OkaneOutlinedButton(
                onClick = onAction,
                modifier = Modifier.testTag("historyEmptyAction"),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun HistoryTransactionRow(
    item: HistoryTransactionItem,
    onOpenDetail: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
) {
    val transaction = item.transaction
    val accent = transactionAccent(transaction.type)
    val typeLabel = transactionTypeLabel(transaction.type)
    val supportingText = if (transaction.type == TransactionType.TRANSFER) {
        "${item.walletName} → ${item.destinationWalletName ?: "Dompet tidak ditemukan"}"
    } else {
        "${item.categoryLabel} · ${item.walletName}"
    }
    val description = buildString {
        append(item.title)
        append(", ")
        append(typeLabel)
        append(", ")
        append(Formatters.signedRupiah(transaction.amount, transaction.type))
        append(", ")
        append(supportingText)
        append(", ")
        append(Formatters.time(transaction.occurredAt))
    }
    val fontScale = LocalDensity.current.fontScale
    val formattedAmount = Formatters.signedRupiah(transaction.amount, transaction.type)

    Surface(
        onClick = onOpenDetail,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transactionRow-${transaction.id}")
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        BoxWithConstraints(modifier = Modifier.padding(16.dp)) {
            val stackAmount =
                maxWidth < 340.dp || fontScale >= 1.3f || formattedAmount.length > 16
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    TransactionTypeIcon(transaction.type, accent)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (item.isUncategorised) {
                            TextButton(
                                onClick = onOpenCategoryPicker,
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("transactionCategory-${transaction.id}"),
                            ) {
                                Text(
                                    text = "Perlu kategori",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = item.walletName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = supportingText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = Formatters.time(transaction.occurredAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!stackAmount) {
                        TransactionAmount(transaction, typeLabel, accent)
                    }
                }
                if (stackAmount) {
                    TransactionAmount(
                        transaction = transaction,
                        typeLabel = typeLabel,
                        accent = accent,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionTypeIcon(
    type: TransactionType,
    accent: Color,
) {
    Surface(
        shape = CircleShape,
        color = accent.copy(alpha = 0.14f),
        modifier = Modifier.size(42.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(19.dp)
                    .rotate(
                        when (type) {
                            TransactionType.IN -> 135f
                            TransactionType.OUT -> -45f
                            TransactionType.TRANSFER -> 0f
                        }
                    ),
            )
        }
    }
}

@Composable
private fun TransactionAmount(
    transaction: TransactionEntity,
    typeLabel: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = Formatters.signedRupiah(transaction.amount, transaction.type),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.End,
        )
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    item: HistoryTransactionItem,
    onDismiss: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val transaction = item.transaction
    val accent = transactionAccent(transaction.type)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("transactionDetailSheet"),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Detail transaksi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Formatters.signedRupiah(transaction.amount, transaction.type),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            StatusChip(text = transactionTypeLabel(transaction.type), color = accent)
            HorizontalDivider()

            if (transaction.type == TransactionType.TRANSFER) {
                DetailValue("Dari", item.walletName)
                DetailValue("Ke", item.destinationWalletName ?: "Dompet tidak ditemukan")
            } else {
                DetailValue("Dompet", item.walletName)
                DetailValue("Kategori", item.categoryLabel ?: "Perlu kategori")
                TextButton(
                    onClick = onOpenCategoryPicker,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("detailCategoryAction"),
                ) {
                    Text(if (item.isUncategorised) "Pilih kategori" else "Ubah kategori")
                }
            }

            item.transaction.merchant?.takeIf { it.isNotBlank() }?.let {
                DetailValue("Merchant", it)
            }
            DetailValue(
                "Sumber",
                when (transaction.source) {
                    TransactionSource.MANUAL -> "Manual"
                    TransactionSource.NOTIFICATION -> "Notifikasi"
                }
            )
            transaction.sourceApp?.takeIf { it.isNotBlank() }?.let {
                DetailValue("Aplikasi sumber", it)
            }
            DetailValue("Waktu transaksi", Formatters.dateTime(transaction.occurredAt))
            DetailValue("Waktu pencatatan", Formatters.dateTime(transaction.createdAt))
            transaction.note?.takeIf { it.isNotBlank() }?.let {
                DetailValue("Catatan", it)
            }
        }
    }
}

@Composable
private fun DetailValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCategoryPickerSheet(
    state: HistoryCategoryPickerState,
    updateInProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCategorySelected: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("historyCategoryPicker"),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = "Pilih kategori",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.transaction.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("historyCategoryError"),
                )
            }
            if (state.categories.isEmpty()) {
                Text(
                    text = "Belum ada kategori aktif yang sesuai dengan jenis transaksi ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                ) {
                    items(state.categories, key = CategoryEntity::id) { category ->
                        val selectedCategory =
                            state.transaction.transaction.categoryId == category.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("historyCategory-${category.id}")
                                .selectable(
                                    selected = selectedCategory,
                                    enabled = !updateInProgress,
                                    role = Role.RadioButton,
                                    onClick = { onCategorySelected(category.id) },
                                )
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedCategory,
                                onClick = null,
                                enabled = !updateInProgress,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (updateInProgress && selectedCategory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun transactionTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.IN -> "Masuk"
    TransactionType.OUT -> "Keluar"
    TransactionType.TRANSFER -> "Transfer"
}

@Composable
private fun transactionAccent(type: TransactionType): Color = when (type) {
    TransactionType.IN -> IncomeGreen
    TransactionType.OUT -> ExpenseRed
    TransactionType.TRANSFER -> TransferBlue
}

private val PreviewActions = HistoryActions(
    onPeriodSelected = {},
    onTypeSelected = {},
    onShowUncategorised = {},
    onShowAllTransactions = {},
    onResetFilters = {},
    onOpenDetail = {},
    onDismissDetail = {},
    onOpenCategoryPicker = {},
    onDismissCategoryPicker = {},
    onAssignCategory = { _, _ -> },
)

@Preview(showBackground = true)
@Composable
private fun TransactionListScreenPreview() {
    val now = Instant.parse("2026-08-05T06:00:00Z")
    val zone = ZoneId.of("Asia/Jakarta")
    val transactions = listOf(
        TransactionEntity(
            id = 1,
            amount = 25_000,
            type = TransactionType.OUT,
            walletId = 1,
            categoryId = 1,
            occurredAt = now.minusSeconds(3_600).toEpochMilli(),
            createdAt = now.toEpochMilli(),
            source = TransactionSource.NOTIFICATION,
            sourceApp = "GoPay",
            merchant = "Makan siang",
        ),
        TransactionEntity(
            id = 2,
            amount = 200_000,
            type = TransactionType.TRANSFER,
            walletId = 2,
            toWalletId = 1,
            occurredAt = now.minusSeconds(10_000).toEpochMilli(),
            createdAt = now.toEpochMilli(),
            source = TransactionSource.MANUAL,
        ),
        TransactionEntity(
            id = 3,
            amount = 1_900_000,
            type = TransactionType.IN,
            walletId = 2,
            categoryId = null,
            occurredAt = now.minusSeconds(90_000).toEpochMilli(),
            createdAt = now.toEpochMilli(),
            source = TransactionSource.MANUAL,
        ),
    )
    val state = HistoryPresenter.present(
        transactions = transactions,
        wallets = listOf(
            WalletEntity(id = 1, name = "GoPay"),
            WalletEntity(id = 2, name = "BCA"),
        ),
        categories = listOf(
            CategoryEntity(id = 1, name = "Makan", type = TransactionType.OUT),
        ),
        now = now,
        zoneId = zone,
    )
    OkaneTheme {
        TransactionListScreen(
            state = state,
            actions = PreviewActions,
            onAddTransaction = {},
        )
    }
}
