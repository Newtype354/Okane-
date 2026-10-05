package com.example.okane.ui.keypad

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.keypad.KeypadFormat
import com.example.okane.domain.keypad.KeypadKey
import com.example.okane.domain.keypad.KeypadState
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.components.CalmBottomSheet
import com.example.okane.ui.components.CalmButton
import com.example.okane.ui.components.CalmChip
import com.example.okane.ui.components.CalmIconButton
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.components.pressScale
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.theme.ExpenseRed
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.Ink
import com.example.okane.ui.theme.OkaneMotion
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.theme.Stone
import com.example.okane.ui.theme.TransferBlue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt
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
    val haptics = LocalHapticFeedback.current

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = CalmTokens.screenPadding),
        ) {
            Header(
                occurredAt = state.occurredAt,
                onPickDate = { showDatePicker = true },
                onClose = onClose,
            )

            ModeToggle(
                isTransfer = state.isTransfer,
                onToggle = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onToggleTransfer(it)
                },
                modifier = Modifier.padding(top = 14.dp),
            )

            WalletLine(
                isTransfer = state.isTransfer,
                sourceName = sourceWallet?.name ?: "Pilih dompet",
                destinationName = destinationWallet?.name,
                onPickSource = { walletPicker = WalletPickerTarget.SOURCE },
                onPickDestination = { walletPicker = WalletPickerTarget.DESTINATION },
                modifier = Modifier.padding(top = 16.dp),
            )

            AmountDisplay(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f),
            )

            // Transfer replaces the category row rather than stacking on top
            // of it, so the keypad keeps the same amount of room either way.
            AnimatedContent(
                targetState = state.isTransfer,
                transitionSpec = {
                    fadeIn(OkaneMotion.enter(OkaneMotion.MEDIUM)) togetherWith
                        fadeOut(OkaneMotion.exit()) using SizeTransform(clip = false)
                },
                label = "categoryRow",
                modifier = Modifier.padding(bottom = 14.dp),
            ) { transfer ->
                if (transfer) {
                    TransferHint(
                        isSameAsSource = state.toWalletId != null &&
                            state.toWalletId == state.walletId,
                        hasDestination = state.toWalletId != null,
                    )
                } else {
                    CategoryChipRow(
                        categories = chipCategories,
                        selectedId = state.categoryId,
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onToggleCategory(it)
                        },
                        onShowAll = { showAllCategories = true },
                    )
                }
            }

            Keypad(
                onKey = {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onKey(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.4f),
            )

            CommitRow(
                enabled = state.canCommit,
                isTransfer = state.isTransfer,
                onCommit = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onCommit(it)
                },
                onCommitTransfer = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onCommitTransfer()
                },
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(CalmTokens.controlRadius),
                containerColor = Ink,
                contentColor = Color.White,
                actionColor = Color(0xFFB9DCC3),
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
            title = if (target == WalletPickerTarget.SOURCE) "Dari dompet" else "Pindahkan ke",
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
    occurredAt: Long?,
    onPickDate: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIconButton(icon = OkaneIcons.Close, contentDescription = "Tutup", onClick = onClose)
        Box(Modifier.weight(1f))
        // The active date is always readable without interaction: "Hari ini"
        // by default, the actual date once backdated.
        CalmChip(
            text = occurredAt?.let(Formatters::date) ?: "Hari ini",
            selected = occurredAt != null,
            onClick = onPickDate,
            leadingIcon = OkaneIcons.Calendar,
        )
    }
}

/**
 * Two-way toggle between recording (Masuk/Keluar) and moving money. The thumb
 * slides between the halves instead of the colours swapping in place.
 */
@Composable
private fun ModeToggle(
    isTransfer: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumb by animateFloatAsState(
        targetValue = if (isTransfer) 1f else 0f,
        animationSpec = OkaneMotion.standard(OkaneMotion.MEDIUM),
        label = "modeThumb",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(Stone)
            .padding(4.dp),
    ) {
        val half = maxWidth / 2
        Box(
            modifier = Modifier
                .offset { IntOffset((half.toPx() * thumb).roundToInt(), 0) }
                .width(half)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        Row(Modifier.fillMaxSize()) {
            ModeOption("Masuk / Keluar", selected = !isTransfer, onClick = { onToggle(false) })
            ModeOption("Pindahkan", selected = isTransfer, onClick = { onToggle(true) })
        }
    }
}

@Composable
private fun RowScope.ModeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        OkaneMotion.standard(OkaneMotion.MEDIUM),
        label = "modeText",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}

@Composable
private fun WalletLine(
    isTransfer: Boolean,
    sourceName: String,
    destinationName: String?,
    onPickSource: () -> Unit,
    onPickDestination: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        WalletPill(label = "Dari", name = sourceName, onClick = onPickSource)
        if (isTransfer) {
            Icon(
                OkaneIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(18.dp),
            )
            WalletPill(label = "Ke", name = destinationName ?: "Pilih", onClick = onPickDestination)
        }
    }
}

@Composable
private fun WalletPill(label: String, name: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .pressScale(interaction, 0.95f),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                OkaneIcons.Wallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "$label ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            Icon(
                OkaneIcons.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Amount
// ---------------------------------------------------------------------------

/**
 * The amount glides as it changes: digits typed push it up, backspace lets it
 * settle down. Short and soft, so fast typing never feels laggy.
 */
@Composable
private fun AmountDisplay(state: KeypadState, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        // Muted while empty so "Rp 0" reads as a placeholder rather than a
        // real amount someone might try to commit.
        if (state.amount > 0) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.outline
        },
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "amountColor",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = state.digits,
            transitionSpec = {
                val growing = targetState.length >= initialState.length
                val direction = if (growing) 1 else -1
                (slideInVertically(tween(200, easing = OkaneMotion.Emphasized)) { direction * it / 4 } +
                    fadeIn(tween(160))) togetherWith
                    (slideOutVertically(tween(160, easing = OkaneMotion.Exit)) { -direction * it / 4 } +
                        fadeOut(tween(110))) using SizeTransform(clip = false)
            },
            label = "amount",
        ) { digits ->
            val text = KeypadFormat.rupiah(digits.toLongOrNull() ?: 0L)
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = when {
                        text.length > 15 -> 34.sp
                        text.length > 12 -> 40.sp
                        else -> 46.sp
                    },
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = color,
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Date
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackdateDialog(
    currentOccurredAt: Long?,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onPicked: (Long?) -> Unit,
) {
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

    val colors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary,
        todayContentColor = MaterialTheme.colorScheme.primary,
        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        colors = colors,
        confirmButton = {
            TextButton(
                onClick = { onPicked(pickerState.selectedDateMillis?.let { toOccurredAt(it, zone) }) },
            ) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    ) {
        DatePicker(state = pickerState, colors = colors)
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
// Transfer hint
// ---------------------------------------------------------------------------

@Composable
private fun TransferHint(isSameAsSource: Boolean, hasDestination: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                // Reachable by changing the source onto the destination. Say
                // why the commit button is dead rather than leaving it unexplained.
                isSameAsSource -> "Pilih dompet tujuan yang berbeda"
                !hasDestination -> "Pilih dompet tujuan untuk memindahkan saldo"
                else -> "Pindah saldo tidak dihitung sebagai pemasukan atau pengeluaran"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isSameAsSource) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
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
    CalmBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle(title)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            wallets.filter { it.id != excludeId }.forEach { wallet ->
                SheetRow(
                    text = wallet.name,
                    icon = OkaneIcons.Wallet,
                    selected = wallet.id == selectedId,
                    onClick = { onPick(wallet.id) },
                )
            }
            // Onboarding runs once, so this is the only way to add a wallet
            // afterwards. Without it the list is frozen for the life of the install.
            InlineAddRow(
                label = "Tambah dompet",
                placeholder = "Nama dompet",
                onAdd = onCreate,
            )
        }
        Box(Modifier.navigationBarsPadding().height(16.dp))
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            CalmChip(
                text = category.name,
                selected = selectedId == category.id,
                // Tapping the selected chip deselects it — category is optional.
                onClick = { onToggle(category.id) },
            )
        }
        CalmChip(
            text = "Semua",
            selected = false,
            onClick = onShowAll,
            leadingIcon = OkaneIcons.Plus,
        )
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

    CalmBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Grouped by side, because picking an income category and then
            // committing Keluar silently drops it — the user should be able to
            // see which side they are choosing from before that happens.
            // Grouping also gives each add row an unambiguous direction.
            listOf(
                "Pengeluaran" to TransactionType.OUT,
                "Pemasukan" to TransactionType.IN,
            ).forEach { (heading, type) ->
                // Rendered even when the group is empty: a user who unticked
                // every category during onboarding would otherwise have no way back.
                SheetTitle(heading, horizontalPadding = 8.dp)
                categories.filter { it.type == type }.forEach { category ->
                    SheetRow(
                        text = category.name,
                        icon = if (type == TransactionType.OUT) OkaneIcons.ArrowUpRight else OkaneIcons.ArrowDownLeft,
                        selected = selectedId == category.id,
                        onClick = {
                            onToggle(category.id)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                    )
                }
                InlineAddRow(
                    label = "Tambah kategori",
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
        }
        Box(Modifier.navigationBarsPadding().height(16.dp))
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

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            fadeIn(OkaneMotion.enter(OkaneMotion.SHORT)) togetherWith
                fadeOut(OkaneMotion.exit()) using SizeTransform(clip = false)
        },
        label = "inlineAdd",
    ) { open ->
        if (!open) {
            SheetRow(
                text = label,
                icon = OkaneIcons.Plus,
                selected = false,
                accent = true,
                onClick = { expanded = true },
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(CalmTokens.controlRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.weight(1f),
                )
                CalmIconButton(
                    icon = OkaneIcons.Check,
                    contentDescription = "Simpan",
                    container = MaterialTheme.colorScheme.primary,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                    size = 48.dp,
                    onClick = {
                        // Blank names are dropped by the ViewModel anyway;
                        // collapsing here stops an empty field lingering open.
                        if (text.isBlank()) expanded = false else onAdd(text)
                        text = ""
                    },
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String, horizontalPadding: androidx.compose.ui.unit.Dp = 24.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
    )
}

@Composable
private fun SheetRow(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "sheetRow",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f),
        shape = RoundedCornerShape(CalmTokens.controlRadius),
        color = container,
        contentColor = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (accent || selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            if (selected) {
                Icon(
                    OkaneIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Keypad — three columns, no empty cells.
// ---------------------------------------------------------------------------

@Composable
private fun Keypad(onKey: (KeypadKey) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeyRow { DigitKey(1, onKey); DigitKey(2, onKey); DigitKey(3, onKey) }
        KeyRow { DigitKey(4, onKey); DigitKey(5, onKey); DigitKey(6, onKey) }
        KeyRow { DigitKey(7, onKey); DigitKey(8, onKey); DigitKey(9, onKey) }
        KeyRow {
            // "000" appends three zeros. Not a ×1000 key: pressed twice, a
            // multiplier produces unintended millions.
            Key("000", "nol tiga kali") { onKey(KeypadKey.TripleZero) }
            DigitKey(0, onKey)
            Key(null, "hapus", icon = OkaneIcons.Backspace) { onKey(KeypadKey.Backspace) }
        }
        // There is deliberately no + or − key. The commit buttons below are
        // the only operators in the product.
    }
}

@Composable
private fun ColumnScope.KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun RowScope.DigitKey(value: Int, onKey: (KeypadKey) -> Unit) {
    Key(value.toString(), value.toString()) { onKey(KeypadKey.Digit(value)) }
}

/**
 * A soft stone key. It sinks and brightens under the finger with a quick
 * spring, which is most of what makes the keypad feel responsive.
 */
@Composable
private fun RowScope.Key(
    label: String?,
    contentDescription: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pressScale(interaction, 0.92f)
            // "⌫" and "000" are not useful spoken labels on their own.
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            } else if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                    fontWeight = FontWeight.Medium,
                )
            }
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
    AnimatedContent(
        targetState = isTransfer,
        transitionSpec = {
            fadeIn(OkaneMotion.enter(OkaneMotion.MEDIUM)) togetherWith fadeOut(OkaneMotion.exit())
        },
        label = "commitRow",
        modifier = modifier,
    ) { transfer ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (transfer) {
                // The two direction buttons collapse into one: a transfer has
                // no direction to choose.
                CommitButton(
                    text = "Pindahkan",
                    icon = OkaneIcons.Transfer,
                    container = TransferBlue,
                    enabled = enabled,
                    onClick = onCommitTransfer,
                    modifier = Modifier.weight(1f),
                )
            } else {
                // Keluar on the LEFT, Masuk on the RIGHT. Same order everywhere.
                CommitButton(
                    text = "Keluar",
                    icon = OkaneIcons.ArrowUpRight,
                    container = ExpenseRed,
                    enabled = enabled,
                    onClick = { onCommit(TransactionType.OUT) },
                    modifier = Modifier.weight(1f),
                )
                CommitButton(
                    text = "Masuk",
                    icon = OkaneIcons.ArrowDownLeft,
                    container = IncomeGreen,
                    enabled = enabled,
                    onClick = { onCommit(TransactionType.IN) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CommitButton(
    text: String,
    icon: ImageVector,
    container: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Disabled while the amount is zero, no wallet is chosen, or a transfer
    // has no distinct destination. The colour fades in as it becomes ready.
    CalmButton(
        onClick = onClick,
        enabled = enabled,
        container = container,
        content = Color.White,
        modifier = modifier.height(58.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
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
