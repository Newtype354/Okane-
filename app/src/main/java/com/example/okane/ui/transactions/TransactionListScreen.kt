package com.example.okane.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import com.example.okane.ui.components.AnimatedAmountText
import com.example.okane.ui.components.CalmBottomSheet
import com.example.okane.ui.components.CalmCard
import com.example.okane.ui.components.CalmSecondaryButton
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.components.IconBadge
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.StatusChip
import com.example.okane.ui.components.pressScale
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.illustration.Pebbles
import com.example.okane.ui.theme.ExpenseRed
import com.example.okane.ui.theme.ExpenseSoft
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.IncomeSoft
import com.example.okane.ui.theme.Lavender
import com.example.okane.ui.theme.LavenderInk
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.theme.TransferBlue
import com.example.okane.ui.theme.TransferSoft
import java.time.Instant
import java.time.ZoneId

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
        // Bottom room clears the floating navigation bar and the add button.
        contentPadding = PaddingValues(
            start = CalmTokens.screenPadding,
            end = CalmTokens.screenPadding,
            top = 12.dp,
            bottom = 184.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "history-filters") {
            HistoryFilters(state.filters, actions)
        }

        item(key = "history-summary") {
            HistorySummaryCard(state.summary, state.filters.period)
        }

        if (state.uncategorisedCount > 0 || state.filters.onlyUncategorised) {
            item(key = "history-uncategorised") {
                UncategorisedEntryPoint(state, actions, Modifier.animateItem())
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
                        modifier = Modifier.animateItem(),
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
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            else -> {
                state.groups.forEach { group ->
                    item(key = "history-date-${group.date}") {
                        SectionHeader(
                            text = Formatters.historyDateLabel(group.date, state.today),
                            modifier = Modifier
                                .animateItem()
                                .padding(top = 10.dp, start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(
                        items = group.transactions,
                        key = { item -> "transaction-${item.transaction.id}" },
                    ) { item ->
                        // animateItem: rows fade in, fade out and glide to new
                        // positions when a filter changes, instead of popping.
                        HistoryTransactionRow(
                            item = item,
                            onOpenDetail = {
                                actions.onOpenDetail(item.transaction.id)
                            },
                            onOpenCategoryPicker = {
                                actions.onOpenCategoryPicker(item.transaction.id)
                            },
                            modifier = Modifier.animateItem(),
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HistoryFilterMenu(
            label = "Periode",
            selectedLabel = filters.period.label,
            icon = OkaneIcons.Calendar,
            options = HistoryPeriod.entries,
            optionLabel = HistoryPeriod::label,
            optionTag = { "historyPeriod-${it.name}" },
            onSelected = actions.onPeriodSelected,
            tag = "historyPeriodFilter",
            modifier = Modifier.weight(1f),
        )
        HistoryFilterMenu(
            label = "Jenis transaksi",
            selectedLabel = filters.type.label,
            icon = OkaneIcons.Transfer,
            options = HistoryTypeFilter.entries,
            optionLabel = HistoryTypeFilter::label,
            optionTag = { "historyType-${it.name}" },
            onSelected = actions.onTypeSelected,
            tag = "historyTypeFilter",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun <T> HistoryFilterMenu(
    label: String,
    selectedLabel: String,
    icon: ImageVector,
    options: List<T>,
    optionLabel: (T) -> String,
    optionTag: (T) -> String,
    onSelected: (T) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .pressScale(interaction, 0.97f)
                .pointerInput(label) {
                    // A horizontal drag that starts on the control belongs to
                    // the control, not to root-page navigation.
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                    }
                }
                // Tag and state on the same (clickable) node, so the current
                // choice is readable wherever the control is found.
                .testTag(tag)
                .semantics {
                    stateDescription = "$label: $selectedLabel"
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
            interactionSource = interaction,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                Icon(
                    OkaneIcons.ChevronDown,
                    contentDescription = "Buka pilihan $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 6.dp,
        ) {
            options.forEach { option ->
                val optionText = optionLabel(option)
                val isSelected = optionText == selectedLabel
                DropdownMenuItem(
                    text = {
                        Text(
                            optionText,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                    modifier = Modifier
                        .testTag(optionTag(option))
                        .semantics { selected = isSelected },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                OkaneIcons.Check,
                                contentDescription = "Dipilih",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
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
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        Text(
            text = "Ringkasan · ${period.label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints(modifier = Modifier.padding(top = 12.dp)) {
            if (maxWidth < 340.dp || fontScale >= 1.3f || useStackedSummary) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryMetric(
                        label = "Pemasukan",
                        amount = summary.income,
                        format = Formatters::rupiah,
                        color = IncomeGreen,
                        soft = IncomeSoft,
                        icon = OkaneIcons.ArrowDownLeft,
                        testTag = "historyIncome",
                    )
                    SummaryMetric(
                        label = "Pengeluaran",
                        amount = summary.expense,
                        format = Formatters::rupiah,
                        color = ExpenseRed,
                        soft = ExpenseSoft,
                        icon = OkaneIcons.ArrowUpRight,
                        testTag = "historyExpense",
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryMetric(
                        label = "Pemasukan",
                        amount = summary.income,
                        format = Formatters::rupiah,
                        color = IncomeGreen,
                        soft = IncomeSoft,
                        icon = OkaneIcons.ArrowDownLeft,
                        testTag = "historyIncome",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetric(
                        label = "Pengeluaran",
                        amount = summary.expense,
                        format = Formatters::rupiah,
                        color = ExpenseRed,
                        soft = ExpenseSoft,
                        icon = OkaneIcons.ArrowUpRight,
                        testTag = "historyExpense",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Selisih",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            val netColor = when {
                summary.net > 0 -> IncomeGreen
                summary.net < 0 -> ExpenseRed
                else -> MaterialTheme.colorScheme.onSurface
            }
            AnimatedAmountText(
                value = summary.net,
                format = Formatters::netRupiah,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = netColor,
                modifier = Modifier.testTag("historyNet"),
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    amount: Long,
    format: (Long) -> String,
    color: Color,
    soft: Color,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, container = soft, tint = color, size = 38.dp)
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedAmountText(
                value = amount,
                format = format,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = color,
                modifier = Modifier.testTag(testTag),
            )
        }
    }
}

@Composable
private fun UncategorisedEntryPoint(
    state: HistoryUiState,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
) {
    val isSelected = state.filters.onlyUncategorised
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = if (isSelected) {
            actions.onShowAllTransactions
        } else {
            actions.onShowUncategorised
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressScale(interaction, 0.98f)
            .testTag("uncategorisedEntry")
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) {
                    "Mode transaksi perlu kategori aktif"
                } else {
                    "Mode transaksi perlu kategori tidak aktif"
                }
            },
        shape = RoundedCornerShape(CalmTokens.tileRadius),
        color = Lavender,
        contentColor = LavenderInk,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(OkaneIcons.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = when {
                    state.uncategorisedCount == 0 -> "Semua transaksi sudah berkategori"
                    isSelected -> "Hanya perlu kategori · ${state.uncategorisedCount} tersisa"
                    else -> "${state.uncategorisedCount} transaksi perlu kategori"
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
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
    modifier: Modifier = Modifier,
) {
    CalmCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("historyEmptyState"),
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Pebbles(modifier = Modifier.size(width = 120.dp, height = 72.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            CalmSecondaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .testTag("historyEmptyAction"),
            )
        }
    }
}

@Composable
private fun HistoryTransactionRow(
    item: HistoryTransactionItem,
    onOpenDetail: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    modifier: Modifier = Modifier,
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
    val interaction = remember { MutableInteractionSource() }

    Surface(
        onClick = onOpenDetail,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f)
            .testTag("transactionRow-${transaction.id}")
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(CalmTokens.tileRadius),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interaction,
    ) {
        BoxWithConstraints(modifier = Modifier.padding(14.dp)) {
            val stackAmount =
                maxWidth < 300.dp || fontScale >= 1.3f || formattedAmount.length > 16
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(
                        icon = transactionIcon(transaction.type),
                        container = transactionSoft(transaction.type),
                        tint = accent,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.isUncategorised) {
                            Text(
                                text = "Perlu kategori",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button, onClick = onOpenCategoryPicker)
                                    .heightIn(min = 36.dp)
                                    .padding(vertical = 8.dp)
                                    .testTag("transactionCategory-${transaction.id}"),
                            )
                            Text(
                                text = item.walletName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = supportingText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = Formatters.time(transaction.occurredAt),
                            style = MaterialTheme.typography.labelSmall,
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
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.End,
        )
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    CalmBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("transactionDetailSheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = transactionIcon(transaction.type),
                    container = transactionSoft(transaction.type),
                    tint = accent,
                    size = 52.dp,
                )
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = "Detail transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatters.signedRupiah(transaction.amount, transaction.type),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(text = transactionTypeLabel(transaction.type), color = accent)
            }

            CalmCard(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = false,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (transaction.type == TransactionType.TRANSFER) {
                    DetailValue("Dari", item.walletName)
                    DetailValue("Ke", item.destinationWalletName ?: "Dompet tidak ditemukan")
                } else {
                    DetailValue("Dompet", item.walletName)
                    DetailValue("Kategori", item.categoryLabel ?: "Perlu kategori")
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

            if (transaction.type != TransactionType.TRANSFER) {
                CalmSecondaryButton(
                    text = if (item.isUncategorised) "Pilih kategori" else "Ubah kategori",
                    icon = OkaneIcons.Pencil,
                    onClick = onOpenCategoryPicker,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    bordered = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detailCategoryAction"),
                )
            }
        }
    }
}

@Composable
private fun DetailValue(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
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
    CalmBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("historyCategoryPicker"),
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
            )
            Text(
                text = state.transaction.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    text = errorMessage.orEmpty(),
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.categories, key = CategoryEntity::id) { category ->
                        val selectedCategory =
                            state.transaction.transaction.categoryId == category.id
                        CategoryOptionRow(
                            name = category.name,
                            selected = selectedCategory,
                            enabled = !updateInProgress,
                            loading = updateInProgress && selectedCategory,
                            onClick = { onCategorySelected(category.id) },
                            modifier = Modifier.testTag("historyCategory-${category.id}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryOptionRow(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(CalmTokens.controlRadius),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                selected -> Icon(
                    OkaneIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                else -> Spacer(Modifier.width(20.dp))
            }
        }
    }
}

private fun transactionTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.IN -> "Masuk"
    TransactionType.OUT -> "Keluar"
    TransactionType.TRANSFER -> "Transfer"
}

private fun transactionAccent(type: TransactionType): Color = when (type) {
    TransactionType.IN -> IncomeGreen
    TransactionType.OUT -> ExpenseRed
    TransactionType.TRANSFER -> TransferBlue
}

private fun transactionSoft(type: TransactionType): Color = when (type) {
    TransactionType.IN -> IncomeSoft
    TransactionType.OUT -> ExpenseSoft
    TransactionType.TRANSFER -> TransferSoft
}

private fun transactionIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.IN -> OkaneIcons.ArrowDownLeft
    TransactionType.OUT -> OkaneIcons.ArrowUpRight
    TransactionType.TRANSFER -> OkaneIcons.Transfer
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
