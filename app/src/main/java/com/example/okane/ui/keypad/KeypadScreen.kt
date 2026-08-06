package com.example.okane.ui.keypad

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.keypad.KeypadFormat
import com.example.okane.domain.keypad.KeypadKey
import com.example.okane.domain.keypad.KeypadState
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.components.GlassTokens
import com.example.okane.ui.components.GlassVariant
import com.example.okane.ui.components.glassStyle
import com.example.okane.ui.theme.ExpenseRed
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.OkaneTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The calculator screen: a numeric keypad as the main surface, no navigation,
 * no form fields.
 *
 * Stateless by design (values in, callbacks out), matching the rest of the
 * app's screens. All state lives in [KeypadViewModel], and the input rules
 * live in [KeypadState] so the deferred widget can share them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeypadScreen(
    state: KeypadState,
    wallets: List<WalletEntity>,
    chipCategories: List<CategoryEntity>,
    allCategories: List<CategoryEntity>,
    pendingUndo: UndoEvent?,
    onKey: (KeypadKey) -> Unit,
    onSelectWallet: (Long) -> Unit,
    onSelectToWallet: (Long) -> Unit,
    onToggleTransfer: (Boolean) -> Unit,
    onToggleCategory: (Long) -> Unit,
    onCreateWallet: (String, Boolean) -> Unit,
    onCreateCategory: (String, TransactionType) -> Unit,
    onPickDate: (Long?) -> Unit,
    onCommit: (TransactionType) -> Unit,
    onCommitTransfer: () -> Unit,
    onUndo: () -> Unit,
    onUndoExpired: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // Injectable for previews and tests; "today" must not be a hidden global.
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }
    var walletPicker by remember { mutableStateOf(WalletPickerTarget.NONE) }

    // Brief commit confirmation with an undo affordance lasting ~5 seconds.
    // Indefinite + withTimeoutOrNull rather than SnackbarDuration.Short,
    // because Short is ~4s and this window is specified, not incidental.
    LaunchedEffect(pendingUndo) {
        val event = pendingUndo ?: return@LaunchedEffect
        val what = when (event.type) {
            TransactionType.IN -> "tercatat sebagai Masuk"
            TransactionType.OUT -> "tercatat sebagai Keluar"
            TransactionType.TRANSFER -> "dipindahkan"
        }
        val suffix = if (event.categoryWasDropped) " · kategori dilepas" else ""
        val result = withTimeoutOrNull(UNDO_WINDOW_MILLIS) {
            snackbarHostState.showSnackbar(
                message = "${KeypadFormat.rupiah(event.amount)} $what$suffix",
                actionLabel = "Urungkan",
                duration = SnackbarDuration.Indefinite,
            )
        }
        if (result == SnackbarResult.ActionPerformed) onUndo() else onUndoExpired()
    }

    val sourceWallet = wallets.firstOrNull { it.id == state.walletId }
    val destinationWallet = wallets.firstOrNull { it.id == state.toWalletId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Header(
                walletName = sourceWallet?.name ?: "Pilih dompet",
                isTransfer = state.isTransfer,
                onPickWallet = { walletPicker = WalletPickerTarget.SOURCE },
                onToggleTransfer = onToggleTransfer,
                onClose = onClose,
            )

            AmountDisplay(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )

            DateChip(
                occurredAt = state.occurredAt,
                onClick = { showDatePicker = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // Transfer replaces the category row rather than stacking on top
            // of it, so the keypad keeps the same amount of room either way.
            if (state.isTransfer) {
                TransferDestinationRow(
                    destinationName = destinationWallet?.name,
                    isSameAsSource = state.toWalletId != null &&
                        state.toWalletId == state.walletId,
                    onPick = { walletPicker = WalletPickerTarget.DESTINATION },
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                CategoryChipRow(
                    categories = chipCategories,
                    selectedId = state.categoryId,
                    onToggle = onToggleCategory,
                    onShowAll = { showAllCategories = true },
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }

            Keypad(
                onKey = onKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            CommitRow(
                enabled = state.canCommit,
                isTransfer = state.isTransfer,
                onCommit = onCommit,
                onCommitTransfer = onCommitTransfer,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )
        }
    }

    if (showDatePicker) {
        BackdateDialog(
            currentOccurredAt = state.occurredAt,
            zone = zone,
            onDismiss = { showDatePicker = false },
            onPicked = {
                onPickDate(it)
                showDatePicker = false
            },
        )
    }

    if (showAllCategories) {
        AllCategoriesSheet(
            categories = allCategories,
            selectedId = state.categoryId,
            onToggle = onToggleCategory,
            onCreate = onCreateCategory,
            onDismiss = { showAllCategories = false },
        )
    }

    if (walletPicker != WalletPickerTarget.NONE) {
        val target = walletPicker
        WalletPickerSheet(
            title = if (target == WalletPickerTarget.SOURCE) "Dompet" else "Pindahkan ke",
            wallets = wallets,
            selectedId = if (target == WalletPickerTarget.SOURCE) state.walletId else state.toWalletId,
            // The source wallet is not offered as its own destination.
            excludeId = if (target == WalletPickerTarget.DESTINATION) state.walletId else null,
            onPick = {
                if (target == WalletPickerTarget.SOURCE) onSelectWallet(it) else onSelectToWallet(it)
                walletPicker = WalletPickerTarget.NONE
            },
            onCreate = { name ->
                onCreateWallet(name, target == WalletPickerTarget.DESTINATION)
                walletPicker = WalletPickerTarget.NONE
            },
            onDismiss = { walletPicker = WalletPickerTarget.NONE },
        )
    }
}

private enum class WalletPickerTarget { NONE, SOURCE, DESTINATION }

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun Header(
    walletName: String,
    isTransfer: Boolean,
    onPickWallet: () -> Unit,
    onToggleTransfer: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = onPickWallet,
            label = {
                Text(
                    walletName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            },
        )
        Box(Modifier.weight(1f))
        FilterChip(
            selected = isTransfer,
            onClick = { onToggleTransfer(!isTransfer) },
            label = { Text("Transfer") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Tutup")
        }
    }
}

// ---------------------------------------------------------------------------
// Amount
// ---------------------------------------------------------------------------

@Composable
private fun AmountDisplay(state: KeypadState, modifier: Modifier = Modifier) {
    Text(
        text = KeypadFormat.display(state),
        modifier = modifier,
        textAlign = TextAlign.Center,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        // Muted while empty so "Rp 0" reads as a placeholder rather than a
        // real amount someone might try to commit.
        color = if (state.amount > 0) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

// ---------------------------------------------------------------------------
// Date
// ---------------------------------------------------------------------------

@Composable
private fun DateChip(
    occurredAt: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The active date is always readable without interaction: "Hari ini" by
    // default, the actual date once backdated.
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        leadingIcon = {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.height(18.dp),
            )
        },
        label = { Text(occurredAt?.let(Formatters::date) ?: "Hari ini") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackdateDialog(
    currentOccurredAt: Long?,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onPicked: (Long?) -> Unit,
) {
    val modalStyle = glassStyle(GlassVariant.Modal)
    val today = LocalDate.now(zone)

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = (currentOccurredAt ?: System.currentTimeMillis())
            .let { millis ->
                // The picker works in UTC-midnight terms, so convert the local
                // calendar date across rather than passing the raw instant.
                Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        selectableDates = object : SelectableDates {
            // A transaction that has not happened yet cannot be recorded.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !utcMillisToLocalDate(utcTimeMillis).isAfter(today)

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(
            GlassTokens.borderWidth,
            modalStyle.border,
            modalStyle.shape,
        ),
        shape = modalStyle.shape,
        tonalElevation = modalStyle.elevation,
        colors = androidx.compose.material3.DatePickerDefaults.colors(
            containerColor = modalStyle.fill,
        ),
        confirmButton = {
            TextButton(
                onClick = { onPicked(pickerState.selectedDateMillis?.let { toOccurredAt(it, zone) }) },
            ) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    ) {
        DatePicker(
            state = pickerState,
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = Color.Transparent,
            ),
        )
    }
}

private fun utcMillisToLocalDate(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Converts the picker's UTC-midnight value into an `occurredAt`.
 *
 * Returns null for today, so the state falls back to "now" and the chip reads
 * "Hari ini" — picking today's date and never opening the picker should not
 * produce two different stored values.
 *
 * For any earlier date the current time of day is carried over, so backdated
 * entries sort sensibly among that day's transactions instead of all piling up
 * at midnight.
 */
private fun toOccurredAt(utcMillis: Long, zone: ZoneId): Long? {
    val picked = utcMillisToLocalDate(utcMillis)
    if (picked == LocalDate.now(zone)) return null
    return picked.atTime(LocalTime.now(zone)).atZone(zone).toInstant().toEpochMilli()
}

// ---------------------------------------------------------------------------
// Transfer destination
// ---------------------------------------------------------------------------

@Composable
private fun TransferDestinationRow(
    destinationName: String?,
    isSameAsSource: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "ke",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AssistChip(
            onClick = onPick,
            label = { Text(destinationName ?: "Pilih dompet tujuan") },
        )
        if (isSameAsSource) {
            // Reachable by changing the source onto the destination. Say why
            // the commit button is dead rather than leaving it unexplained.
            Text(
                text = "Pilih dompet yang berbeda",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Wallet picker
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletPickerSheet(
    title: String,
    wallets: List<WalletEntity>,
    selectedId: Long?,
    excludeId: Long?,
    onPick: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val modalStyle = glassStyle(GlassVariant.Modal)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.border(GlassTokens.borderWidth, modalStyle.border, modalStyle.shape),
        shape = modalStyle.shape,
        containerColor = modalStyle.fill,
        contentColor = modalStyle.content,
        tonalElevation = modalStyle.elevation,
        scrimColor = Color.Black.copy(alpha = 0.44f),
    ) {
        SheetTitle(title)
        wallets.filter { it.id != excludeId }.forEach { wallet ->
            SheetRow(
                text = wallet.name,
                selected = wallet.id == selectedId,
                onClick = { onPick(wallet.id) },
            )
        }
        // Onboarding runs once, so this is the only way to add a wallet
        // afterwards. Without it the list is frozen for the life of the install.
        InlineAddRow(
            label = "+ Tambah dompet",
            placeholder = "Nama dompet",
            onAdd = onCreate,
        )
        Box(Modifier.navigationBarsPadding().height(8.dp))
    }
}

// ---------------------------------------------------------------------------
// Categories
// ---------------------------------------------------------------------------

@Composable
private fun CategoryChipRow(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onToggle: (Long) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = selectedId == category.id,
                // Tapping the selected chip deselects it — category is optional.
                onClick = { onToggle(category.id) },
                label = { Text(category.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
        AssistChip(onClick = onShowAll, label = { Text("Semua") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllCategoriesSheet(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onToggle: (Long) -> Unit,
    onCreate: (String, TransactionType) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val modalStyle = glassStyle(GlassVariant.Modal)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.border(GlassTokens.borderWidth, modalStyle.border, modalStyle.shape),
        shape = modalStyle.shape,
        containerColor = modalStyle.fill,
        contentColor = modalStyle.content,
        tonalElevation = modalStyle.elevation,
        scrimColor = Color.Black.copy(alpha = 0.44f),
    ) {
        // Grouped by side, because picking an income category and then
        // committing Keluar silently drops it — the user should be able to see
        // which side they are choosing from before that happens. Grouping also
        // gives each add row an unambiguous direction.
        listOf(
            "Pengeluaran" to TransactionType.OUT,
            "Pemasukan" to TransactionType.IN,
        ).forEach { (heading, type) ->
            // Rendered even when the group is empty: a user who unticked every
            // category during onboarding would otherwise have no way back.
            SheetTitle(heading)
            categories.filter { it.type == type }.forEach { category ->
                SheetRow(
                    text = category.name,
                    selected = selectedId == category.id,
                    onClick = {
                        onToggle(category.id)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                )
            }
            InlineAddRow(
                label = "+ Tambah kategori",
                placeholder = if (type == TransactionType.OUT) {
                    "Kategori pengeluaran"
                } else {
                    "Kategori pemasukan"
                },
                onAdd = { name ->
                    onCreate(name, type)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
            )
        }
        Box(Modifier.navigationBarsPadding().height(8.dp))
    }
}

/**
 * A sheet row that turns into a text field when tapped.
 *
 * Inline rather than a separate screen: needing a wallet or category that does
 * not exist yet is something the user discovers mid-entry, and sending them
 * elsewhere to create it would throw away the amount they have already typed.
 */
@Composable
private fun InlineAddRow(
    label: String,
    placeholder: String,
    onAdd: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    if (!expanded) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.height(20.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    // Blank names are dropped by the ViewModel anyway;
                    // collapsing here stops an empty field lingering open.
                    if (text.isBlank()) expanded = false else onAdd(text)
                    text = ""
                },
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Simpan")
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun SheetRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Keypad — three columns, no empty cells.
// ---------------------------------------------------------------------------

@Composable
private fun Keypad(onKey: (KeypadKey) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        KeyRow(Modifier.weight(1f)) {
            DigitKey(1, onKey); DigitKey(2, onKey); DigitKey(3, onKey)
        }
        KeyRow(Modifier.weight(1f)) {
            DigitKey(4, onKey); DigitKey(5, onKey); DigitKey(6, onKey)
        }
        KeyRow(Modifier.weight(1f)) {
            DigitKey(7, onKey); DigitKey(8, onKey); DigitKey(9, onKey)
        }
        KeyRow(Modifier.weight(1f)) {
            DigitKey(0, onKey)
            // "000" appends three zeros. Not a ×1000 key: pressed twice, a
            // multiplier produces unintended millions.
            Key("000", "nol tiga kali") { onKey(KeypadKey.TripleZero) }
            Key("⌫", "hapus") { onKey(KeypadKey.Backspace) }
        }
        // There is deliberately no + or − key. The commit buttons below are
        // the only operators in the product.
    }
}

@Composable
private fun KeyRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier.fillMaxWidth(), content = content)
}

@Composable
private fun RowScope.DigitKey(value: Int, onKey: (KeypadKey) -> Unit) {
    Key(value.toString(), value.toString()) { onKey(KeypadKey.Digit(value)) }
}

@Composable
private fun RowScope.Key(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // Contrast: the light scheme's surfaceVariant is #F5F5F5 on a #FFFFFF
    // background — the key faces would be all but invisible at full brightness
    // outdoors. A brand-tinted face keeps the key edges obvious in light mode,
    // and the label stays near-maximum contrast against it in both schemes.
    val face = MaterialTheme.colorScheme.primaryContainer
    val labelColor = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(4.dp)
            // "⌫" and "000" are not useful spoken labels on their own.
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(14.dp),
        color = face,
        contentColor = labelColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, fontSize = 26.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ---------------------------------------------------------------------------
// Commit
// ---------------------------------------------------------------------------

@Composable
private fun CommitRow(
    enabled: Boolean,
    isTransfer: Boolean,
    onCommit: (TransactionType) -> Unit,
    onCommitTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val out = ExpenseRed
    val income = IncomeGreen
    val onExpense = Color.White
    val onIncome = Color.White

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isTransfer) {
            // The two direction buttons collapse into one: a transfer has no
            // direction to choose.
            CommitButton(
                text = "Pindahkan",
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary,
                enabled = enabled,
                onClick = onCommitTransfer,
                modifier = Modifier.weight(1f),
            )
        } else {
            // Keluar on the LEFT, Masuk on the RIGHT. Same order everywhere.
            CommitButton(
                text = "Keluar",
                container = out,
                content = onExpense,
                enabled = enabled,
                onClick = { onCommit(TransactionType.OUT) },
                modifier = Modifier.weight(1f),
            )
            CommitButton(
                text = "Masuk",
                container = income,
                content = onIncome,
                enabled = enabled,
                onClick = { onCommit(TransactionType.IN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CommitButton(
    text: String,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        // Disabled while the amount is zero, no wallet is chosen, or a
        // transfer has no distinct destination.
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.30f),
            disabledContentColor = content.copy(alpha = 0.60f),
        ),
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

private const val UNDO_WINDOW_MILLIS = 5_000L

// ---------------------------------------------------------------------------

private val previewWallets = listOf(
    WalletEntity(1, "Tunai", isDefault = true),
    WalletEntity(2, "BCA"),
    WalletEntity(3, "GoPay"),
)

private val previewCategories = listOf(
    CategoryEntity(1, "Makan", TransactionType.OUT),
    CategoryEntity(2, "Transport", TransactionType.OUT),
    CategoryEntity(3, "Belanja", TransactionType.OUT),
    CategoryEntity(4, "Tagihan", TransactionType.OUT),
    CategoryEntity(5, "Lainnya", TransactionType.OUT),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun KeypadScreenPreview() {
    OkaneTheme {
        KeypadScreen(
            state = KeypadState(digits = "125000", walletId = 1, categoryId = 1),
            wallets = previewWallets,
            chipCategories = previewCategories,
            allCategories = previewCategories,
            pendingUndo = null,
            onKey = {}, onSelectWallet = {}, onSelectToWallet = {},
            onToggleTransfer = {}, onToggleCategory = {},
            onCreateWallet = { _, _ -> }, onCreateCategory = { _, _ -> },
            onPickDate = {},
            onCommit = {}, onCommitTransfer = {}, onUndo = {},
            onUndoExpired = {}, onClose = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun KeypadScreenTransferPreview() {
    OkaneTheme {
        KeypadScreen(
            state = KeypadState(
                digits = "500000",
                walletId = 2,
                toWalletId = 3,
                isTransfer = true,
            ),
            wallets = previewWallets,
            chipCategories = previewCategories,
            allCategories = previewCategories,
            pendingUndo = null,
            onKey = {}, onSelectWallet = {}, onSelectToWallet = {},
            onToggleTransfer = {}, onToggleCategory = {},
            onCreateWallet = { _, _ -> }, onCreateCategory = { _, _ -> },
            onPickDate = {},
            onCommit = {}, onCommitTransfer = {}, onUndo = {},
            onUndoExpired = {}, onClose = {},
        )
    }
}
