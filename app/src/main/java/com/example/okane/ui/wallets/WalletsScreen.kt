package com.example.okane.ui.wallets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.okane.data.local.WalletBalance
import com.example.okane.ui.components.AnimatedAmountText
import com.example.okane.ui.components.CalmBottomSheet
import com.example.okane.ui.components.CalmCard
import com.example.okane.ui.components.CalmIconButton
import com.example.okane.ui.components.CalmPrimaryButton
import com.example.okane.ui.components.CalmSecondaryButton
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.StatusChip
import com.example.okane.ui.components.pressScale
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.illustration.Pebbles
import com.example.okane.ui.theme.ExpenseRed
import com.example.okane.ui.theme.ExpenseSoft
import com.example.okane.ui.theme.Ink
import com.example.okane.ui.theme.Lavender
import com.example.okane.ui.theme.LavenderInk
import com.example.okane.ui.theme.Mint
import com.example.okane.ui.theme.Mist
import com.example.okane.ui.theme.MistInk
import com.example.okane.ui.theme.OkaneMotion
import com.example.okane.ui.theme.Peach
import com.example.okane.ui.theme.PeachInk
import com.example.okane.ui.theme.SageInk
import com.example.okane.ui.theme.SageMist
import com.example.okane.ui.theme.Stone

data class WalletActions(
    val onOpenSettings: () -> Unit,
    val onOpenAdd: () -> Unit,
    val onOpenDetail: (Long, Boolean) -> Unit,
    val onOpenArchived: () -> Unit,
    val onBack: () -> Unit,
    val onOpenEdit: () -> Unit,
    val onDismissEditor: () -> Unit,
    val onEditorNameChanged: (String) -> Unit,
    val onEditorInitialBalanceChanged: (String) -> Unit,
    val onEditorMonthlyFeeChanged: (String) -> Unit,
    val onEditorDefaultChanged: (Boolean) -> Unit,
    val onSaveEditor: () -> Unit,
    val onMakeDefault: () -> Unit,
    val onRequestArchive: () -> Unit,
    val onDismissArchive: () -> Unit,
    val onConfirmArchive: () -> Unit,
    val onRestore: () -> Unit,
    val onFeedbackShown: () -> Unit,
)

@Composable
fun WalletsScreen(
    state: WalletsUiState,
    actions: WalletActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.feedbackMessage) {
        val message = state.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        actions.onFeedbackShown()
    }

    BackHandler(enabled = state.hasOpenSecondarySurface) { actions.onBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ROOT → DETAIL / ARCHIVED slides forward, back slides the other way,
        // so the three surfaces read as one small stack rather than a swap.
        AnimatedContent(
            targetState = state.surface,
            transitionSpec = { walletSurfaceTransition(initialState, targetState) },
            label = "walletSurface",
            modifier = Modifier.fillMaxSize(),
        ) { surface ->
            Column(modifier = Modifier.fillMaxSize()) {
                when (surface) {
                    WalletSurface.ROOT -> {
                        RootHeader(actions.onOpenSettings)
                        WalletRootContent(state, actions)
                    }
                    WalletSurface.ARCHIVED -> {
                        BackHeader(title = "Dompet diarsipkan", onBack = actions.onBack)
                        ArchivedWalletContent(state, actions)
                    }
                    WalletSurface.DETAIL -> {
                        BackHeader(title = "Detail dompet", onBack = actions.onBack)
                        WalletDetailContent(state, actions)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.operationInProgress,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("walletOperationProgress"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = SageMist,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 92.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(CalmTokens.controlRadius),
                containerColor = Ink,
                contentColor = Color.White,
            )
        }
    }

    state.editor?.let { editor ->
        WalletEditorSheet(
            editor = editor,
            operationInProgress = state.operationInProgress,
            actions = actions,
        )
    }

    state.archiveCandidate?.let { wallet ->
        ArchiveWalletDialog(
            walletName = wallet.name,
            operationInProgress = state.operationInProgress,
            onDismiss = actions.onDismissArchive,
            onConfirm = actions.onConfirmArchive,
        )
    }
}

private fun walletSurfaceTransition(from: WalletSurface, to: WalletSurface): ContentTransform {
    val forward = to.ordinal > from.ordinal || from == WalletSurface.ROOT
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(OkaneMotion.enter()) { width -> direction * width / 5 } +
        fadeIn(OkaneMotion.enter())) togetherWith
        (slideOutHorizontally(OkaneMotion.exit()) { width -> -direction * width / 10 } +
            fadeOut(OkaneMotion.exit()))
}

@Composable
private fun RootHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = CalmTokens.screenPadding, end = CalmTokens.screenPadding, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Dompetku",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Tempat uangmu beristirahat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        CalmIconButton(
            icon = OkaneIcons.Settings,
            contentDescription = "Pengaturan",
            onClick = onOpenSettings,
            container = Stone,
            size = 48.dp,
            modifier = Modifier.testTag("walletSettings"),
        )
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = CalmTokens.screenPadding, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIconButton(
            icon = OkaneIcons.ChevronLeft,
            contentDescription = "Kembali",
            onClick = onBack,
            size = 44.dp,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

@Composable
private fun WalletRootContent(state: WalletsUiState, actions: WalletActions) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("walletList"),
        contentPadding = PaddingValues(
            start = CalmTokens.screenPadding,
            end = CalmTokens.screenPadding,
            top = 12.dp,
            bottom = 140.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "wallet-total") {
            TotalMoneyPanel(
                totalMoney = state.totalMoney,
                showArchivedNote = state.archivedContributesToTotal,
            )
        }

        if (state.activeWallets.isNotEmpty()) {
            item(key = "wallet-add") {
                CalmPrimaryButton(
                    text = "Tambah dompet",
                    icon = OkaneIcons.Plus,
                    onClick = actions.onOpenAdd,
                    enabled = !state.operationInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("addWallet"),
                )
            }
            item(key = "wallet-active-header") {
                SectionHeader("Dompet aktif", modifier = Modifier.padding(top = 12.dp, start = 4.dp))
            }
            items(state.activeWallets, key = WalletBalance::walletId) { wallet ->
                WalletRow(
                    wallet = wallet,
                    archived = false,
                    onClick = { actions.onOpenDetail(wallet.walletId, false) },
                    modifier = Modifier.animateItem(),
                )
            }
        } else if (state.isLoading) {
            item(key = "wallet-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            item(key = "wallet-empty") { WalletEmptyState(actions.onOpenAdd) }
        }

        item(key = "wallet-archived") {
            CalmSecondaryButton(
                text = if (state.archivedWallets.isEmpty()) {
                    "Lihat dompet diarsipkan"
                } else {
                    "Lihat dompet diarsipkan (${state.archivedWallets.size})"
                },
                icon = OkaneIcons.Archive,
                onClick = actions.onOpenArchived,
                enabled = !state.operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("openArchivedWallets"),
            )
        }
    }
}

@Composable
private fun TotalMoneyPanel(totalMoney: Long, showArchivedNote: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CalmTokens.cardRadius))
            .background(Brush.linearGradient(listOf(Stone, Stone, SageMist.copy(alpha = 0.7f))))
            .testTag("walletTotal"),
    ) {
        Row(
            modifier = Modifier.padding(start = 22.dp, top = 20.dp, bottom = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Total uangmu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedAmountText(
                    value = totalMoney,
                    format = Formatters::balanceRupiah,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("walletTotalAmount"),
                )
                if (showArchivedNote) {
                    Text(
                        "Termasuk dompet diarsipkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Pebbles(modifier = Modifier.size(width = 84.dp, height = 56.dp))
        }
    }
}

/** Soft, stable avatar colours: the same wallet always gets the same tint. */
private val AvatarTints = listOf(
    Mint to SageInk,
    Mist to MistInk,
    Lavender to LavenderInk,
    Peach to PeachInk,
)

@Composable
private fun WalletAvatar(wallet: WalletBalance, modifier: Modifier = Modifier, size: Int = 44) {
    val (container, ink) = AvatarTints[Math.floorMod(wallet.walletId, AvatarTints.size.toLong()).toInt()]
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (wallet.isArchived) Stone else container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            OkaneIcons.Wallet,
            contentDescription = null,
            tint = if (wallet.isArchived) MaterialTheme.colorScheme.onSurfaceVariant else ink,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

@Composable
private fun WalletRow(
    wallet: WalletBalance,
    archived: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val balanceText = Formatters.balanceRupiah(wallet.balance)
    val description = buildString {
        append(wallet.name)
        append(", saldo ")
        append(balanceText)
        if (wallet.isDefault) append(", Utama")
        if (archived) append(", Diarsipkan")
        if (wallet.monthlyFee > 0) {
            append(", Admin bulanan ")
            append(Formatters.rupiah(wallet.monthlyFee))
        }
    }
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("walletRow-${wallet.walletId}"),
        shape = RoundedCornerShape(CalmTokens.tileRadius),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            CalmTokens.hairline,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WalletAvatar(wallet)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (wallet.isDefault) {
                        StatusChip(text = "Utama", color = MaterialTheme.colorScheme.primary)
                    }
                    if (archived) {
                        StatusChip(text = "Diarsipkan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    balanceText,
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (wallet.monthlyFee > 0) {
                    Text(
                        "Admin bulanan ${Formatters.rupiah(wallet.monthlyFee)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                OkaneIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun WalletEmptyState(onAdd: () -> Unit) {
    CalmCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Pebbles(modifier = Modifier.size(width = 120.dp, height = 72.dp))
            Text(
                "Belum ada dompet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Tambahkan dompet untuk mulai mencatat sumber uangmu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            CalmPrimaryButton(
                text = "Tambah dompet",
                icon = OkaneIcons.Plus,
                onClick = onAdd,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag("emptyAddWallet"),
            )
        }
    }
}

@Composable
private fun ArchivedWalletContent(state: WalletsUiState, actions: WalletActions) {
    if (!state.isLoading && state.archivedWallets.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Pebbles(modifier = Modifier.size(width = 120.dp, height = 72.dp))
            Text(
                "Belum ada dompet diarsipkan",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Dompet yang diarsipkan akan tetap menyimpan transaksi dan saldonya.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("archivedWalletList"),
        contentPadding = PaddingValues(
            start = CalmTokens.screenPadding,
            end = CalmTokens.screenPadding,
            top = 12.dp,
            bottom = 140.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.archivedWallets, key = WalletBalance::walletId) { wallet ->
            WalletRow(
                wallet = wallet,
                archived = true,
                onClick = { actions.onOpenDetail(wallet.walletId, true) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun WalletDetailContent(state: WalletsUiState, actions: WalletActions) {
    val wallet = state.selectedWallet
    if (wallet == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Dompet tidak ditemukan.")
            TextButton(onClick = actions.onBack) { Text("Kembali") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("walletDetail"),
        contentPadding = PaddingValues(
            start = CalmTokens.screenPadding,
            end = CalmTokens.screenPadding,
            top = 12.dp,
            bottom = 140.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CalmTokens.cardRadius))
                    .background(Brush.linearGradient(listOf(Stone, Stone, SageMist.copy(alpha = 0.7f)))),
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WalletAvatar(wallet, size = 48)
                        Text(
                            wallet.name,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (wallet.isDefault) {
                            StatusChip(text = "Utama", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        "Saldo saat ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    AnimatedAmountText(
                        value = wallet.balance,
                        format = Formatters::balanceRupiah,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }
            }
        }
        item {
            CalmCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            ) {
                DetailLine("Nama dompet", wallet.name)
                DetailDivider()
                DetailLine("Saldo awal", Formatters.balanceRupiah(wallet.initialBalance))
                DetailDivider()
                DetailLine("Biaya admin bulanan", Formatters.rupiah(wallet.monthlyFee))
                Text(
                    "Hanya informasi, belum dipotong otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                DetailDivider()
                DetailLine("Status utama", if (wallet.isDefault) "Utama" else "Bukan utama")
                DetailDivider()
                DetailLine("Status", if (wallet.isArchived) "Diarsipkan" else "Aktif")
            }
        }
        if (wallet.isArchived) {
            item {
                CalmPrimaryButton(
                    text = "Aktifkan kembali",
                    icon = OkaneIcons.Restore,
                    onClick = actions.onRestore,
                    enabled = !state.operationInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restoreWallet"),
                )
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalmSecondaryButton(
                        text = "Edit",
                        icon = OkaneIcons.Pencil,
                        onClick = actions.onOpenEdit,
                        enabled = !state.operationInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editWallet"),
                    )
                    if (!wallet.isDefault) {
                        CalmSecondaryButton(
                            text = "Jadikan utama",
                            icon = OkaneIcons.Star,
                            onClick = actions.onMakeDefault,
                            enabled = !state.operationInProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("makeDefaultWallet"),
                        )
                    }
                    CalmSecondaryButton(
                        text = "Arsipkan",
                        icon = OkaneIcons.Archive,
                        onClick = actions.onRequestArchive,
                        enabled = !state.operationInProgress,
                        container = ExpenseSoft,
                        content = ExpenseRed,
                        bordered = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("archiveWallet"),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.25f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletEditorSheet(
    editor: WalletEditorState,
    operationInProgress: Boolean,
    actions: WalletActions,
) {
    // A form opens fully: half-open would hide the save button below the fold.
    CalmBottomSheet(
        onDismissRequest = actions.onDismissEditor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("walletEditor"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (editor.mode == WalletEditorMode.ADD) "Tambah dompet" else "Edit dompet",
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = actions.onEditorNameChanged,
                label = { Text("Nama") },
                isError = editor.errorMessage != null,
                supportingText = editor.errorMessage?.let { message -> ({ Text(message) }) },
                singleLine = true,
                enabled = !operationInProgress,
                shape = RoundedCornerShape(CalmTokens.controlRadius),
                colors = calmFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("walletNameInput"),
            )
            WalletAmountField(
                label = "Saldo awal",
                value = editor.initialBalanceInput,
                onValueChange = actions.onEditorInitialBalanceChanged,
                enabled = !operationInProgress,
            )
            if (editor.mode == WalletEditorMode.EDIT) {
                Text(
                    "Mengubah saldo awal akan menyesuaikan saldo tercatat dompet ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WalletAmountField(
                label = "Biaya admin bulanan",
                value = editor.monthlyFeeInput,
                onValueChange = actions.onEditorMonthlyFeeChanged,
                enabled = !operationInProgress,
            )
            Text(
                "Biaya admin hanya disimpan sebagai informasi dan belum dipotong otomatis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(CalmTokens.controlRadius),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !editor.defaultLocked && !operationInProgress,
                            onClick = { actions.onEditorDefaultChanged(!editor.makeDefault) },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dompet utama", style = MaterialTheme.typography.bodyLarge)
                        if (editor.defaultLocked) {
                            Text(
                                "Pilih dompet aktif lain untuk mengganti dompet utama.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = editor.makeDefault,
                        onCheckedChange = actions.onEditorDefaultChanged,
                        enabled = !editor.defaultLocked && !operationInProgress,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
            }
            CalmPrimaryButton(
                text = if (operationInProgress) {
                    "Menyimpan…"
                } else if (editor.mode == WalletEditorMode.ADD) {
                    "Tambah dompet"
                } else {
                    "Simpan perubahan"
                },
                onClick = actions.onSaveEditor,
                enabled = !operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("saveWallet"),
            )
        }
    }
}

@Composable
private fun calmFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

@Composable
private fun WalletAmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    val amount = value.toLongOrNull() ?: 0L
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("0") },
        prefix = { Text("Rp") },
        supportingText = {
            if (amount > 0) Text(Formatters.rupiah(amount)) else Text("Rp0")
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = enabled,
        shape = RoundedCornerShape(CalmTokens.controlRadius),
        colors = calmFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ArchiveWalletDialog(
    walletName: String,
    operationInProgress: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                OkaneIcons.Archive,
                contentDescription = null,
                tint = ExpenseRed,
            )
        },
        title = { Text("Arsipkan dompet?") },
        text = {
            Text(
                "$walletName tidak akan muncul saat mencatat transaksi baru, " +
                    "tetapi transaksi lama dan saldonya tetap tersimpan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !operationInProgress,
                modifier = Modifier.testTag("confirmArchiveWallet"),
            ) {
                Text("Arsipkan", color = ExpenseRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !operationInProgress) {
                Text("Batal")
            }
        },
    )
}
