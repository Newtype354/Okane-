package com.example.okane.ui.wallets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.okane.data.local.WalletBalance
import com.example.okane.ui.components.GlassSurface
import com.example.okane.ui.components.GlassVariant
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.StatusChip
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.theme.IncomeGreen

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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp),
            )
        },
        topBar = {
            when (state.surface) {
                WalletSurface.ROOT -> WalletRootTopBar(actions.onOpenSettings)
                WalletSurface.ARCHIVED -> WalletBackTopBar(
                    title = "Dompet diarsipkan",
                    onBack = actions.onBack,
                )
                WalletSurface.DETAIL -> WalletBackTopBar(
                    title = "Detail dompet",
                    onBack = actions.onBack,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(bottom = 92.dp),
        ) {
            when (state.surface) {
                WalletSurface.ROOT -> WalletRootContent(state, actions)
                WalletSurface.ARCHIVED -> ArchivedWalletContent(state, actions)
                WalletSurface.DETAIL -> WalletDetailContent(state, actions)
            }

            if (state.operationInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .testTag("walletOperationProgress")
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletRootTopBar(onOpenSettings: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Dompetku", style = MaterialTheme.typography.titleLarge) },
        actions = {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("walletSettings"),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletBackTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun WalletRootContent(state: WalletsUiState, actions: WalletActions) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("walletList"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TotalMoneyPanel(
                totalMoney = state.totalMoney,
                showArchivedNote = state.archivedContributesToTotal,
            )
        }

        if (state.activeWallets.isNotEmpty()) {
            item {
                Button(
                    onClick = actions.onOpenAdd,
                    enabled = !state.operationInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("addWallet"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Tambah dompet", modifier = Modifier.padding(start = 8.dp))
                }
            }
            item {
                SectionHeader("Dompet aktif", modifier = Modifier.padding(top = 8.dp))
            }
            items(state.activeWallets, key = WalletBalance::walletId) { wallet ->
                WalletRow(
                    wallet = wallet,
                    archived = false,
                    onClick = { actions.onOpenDetail(wallet.walletId, false) },
                )
            }
        } else if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item { WalletEmptyState(actions.onOpenAdd) }
        }

        item {
            OutlinedButton(
                onClick = actions.onOpenArchived,
                enabled = !state.operationInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("openArchivedWallets"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(ArchiveMaterialIcon, contentDescription = null)
                Text(
                    if (state.archivedWallets.isEmpty()) {
                        "Lihat dompet diarsipkan"
                    } else {
                        "Lihat dompet diarsipkan (${state.archivedWallets.size})"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TotalMoneyPanel(totalMoney: Long, showArchivedNote: Boolean) {
    GlassSurface(
        variant = GlassVariant.Subtle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("walletTotal"),
        contentPadding = PaddingValues(20.dp),
    ) {
        Column {
            Text(
                "Total uangmu",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Formatters.balanceRupiah(totalMoney),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("walletTotalAmount"),
            )
            if (showArchivedNote) {
                Text(
                    "Termasuk dompet diarsipkan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WalletRow(
    wallet: WalletBalance,
    archived: Boolean,
    onClick: () -> Unit,
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("walletRow-${wallet.walletId}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (wallet.isDefault) {
                        StatusChip(text = "Utama", color = MaterialTheme.colorScheme.primary)
                    }
                    if (archived) {
                        StatusChip(text = "Diarsipkan", color = MaterialTheme.colorScheme.outline)
                    }
                }
                Text(
                    balanceText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                if (wallet.monthlyFee > 0) {
                    Text(
                        "Admin bulanan ${Formatters.rupiah(wallet.monthlyFee)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun WalletEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            WalletMaterialIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(42.dp),
        )
        Text(
            "Belum ada dompet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Tambahkan dompet untuk mulai mencatat sumber uangmu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Button(
            onClick = onAdd,
            modifier = Modifier
                .padding(top = 16.dp)
                .height(52.dp)
                .testTag("emptyAddWallet"),
        ) {
            Text("Tambah dompet")
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
            Text("Belum ada dompet diarsipkan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Dompet yang diarsipkan akan tetap menyimpan transaksi dan saldonya.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("archivedWalletList"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.archivedWallets, key = WalletBalance::walletId) { wallet ->
            WalletRow(
                wallet = wallet,
                archived = true,
                onClick = { actions.onOpenDetail(wallet.walletId, true) },
            )
        }
    }
}

@Composable
private fun WalletDetailContent(state: WalletsUiState, actions: WalletActions) {
    val wallet = state.selectedWallet
    if (wallet == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Dompet tidak ditemukan.")
            TextButton(onClick = actions.onBack) { Text("Kembali") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("walletDetail"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GlassSurface(
                variant = GlassVariant.Subtle,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp),
            ) {
                Column {
                    Text(
                        wallet.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Saldo saat ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        Formatters.balanceRupiah(wallet.balance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    DetailLine("Nama wallet", wallet.name)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    DetailLine("Saldo awal", Formatters.balanceRupiah(wallet.initialBalance))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    DetailLine("Biaya admin bulanan", Formatters.rupiah(wallet.monthlyFee))
                    Text(
                        "Hanya informasi, belum dipotong otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    DetailLine("Status utama", if (wallet.isDefault) "Utama" else "Bukan utama")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    DetailLine("Status", if (wallet.isArchived) "Diarsipkan" else "Aktif")
                }
            }
        }
        if (wallet.isArchived) {
            item {
                Button(
                    onClick = actions.onRestore,
                    enabled = !state.operationInProgress,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("restoreWallet"),
                ) {
                    Text("Aktifkan kembali")
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = actions.onOpenEdit,
                        enabled = !state.operationInProgress,
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("editWallet"),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text("Edit", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (!wallet.isDefault) {
                        OutlinedButton(
                            onClick = actions.onMakeDefault,
                            enabled = !state.operationInProgress,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("makeDefaultWallet"),
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Text("Jadikan utama", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Button(
                        onClick = actions.onRequestArchive,
                        enabled = !state.operationInProgress,
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("archiveWallet"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(ArchiveMaterialIcon, contentDescription = null)
                        Text("Arsipkan", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
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
    ModalBottomSheet(
        onDismissRequest = actions.onDismissEditor,
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
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = editor.name,
                onValueChange = actions.onEditorNameChanged,
                label = { Text("Nama") },
                isError = editor.errorMessage != null,
                supportingText = editor.errorMessage?.let { message -> ({ Text(message) }) },
                singleLine = true,
                enabled = !operationInProgress,
                modifier = Modifier.fillMaxWidth().testTag("walletNameInput"),
            )
            WalletAmountField(
                label = "Saldo awal",
                value = editor.initialBalanceInput,
                onValueChange = actions.onEditorInitialBalanceChanged,
                enabled = !operationInProgress,
            )
            if (editor.mode == WalletEditorMode.EDIT) {
                Text(
                    "Mengubah saldo awal akan menyesuaikan saldo tercatat wallet ini.",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !editor.defaultLocked && !operationInProgress,
                        onClick = { actions.onEditorDefaultChanged(!editor.makeDefault) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wallet utama", style = MaterialTheme.typography.bodyLarge)
                    if (editor.defaultLocked) {
                        Text(
                            "Pilih wallet aktif lain untuk mengganti wallet utama.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = editor.makeDefault,
                    onCheckedChange = actions.onEditorDefaultChanged,
                    enabled = !editor.defaultLocked && !operationInProgress,
                )
            }
            Button(
                onClick = actions.onSaveEditor,
                enabled = !operationInProgress,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("saveWallet"),
            ) {
                if (operationInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (editor.mode == WalletEditorMode.ADD) "Tambah dompet" else "Simpan perubahan")
                }
            }
        }
    }
}

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
        icon = { Icon(ArchiveMaterialIcon, contentDescription = null) },
        title = { Text("Arsipkan dompet?") },
        text = {
            Text(
                "$walletName tidak akan muncul saat mencatat transaksi baru, " +
                    "tetapi transaksi lama dan saldonya tetap tersimpan."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !operationInProgress,
                modifier = Modifier.testTag("confirmArchiveWallet"),
            ) {
                Text("Arsipkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !operationInProgress) {
                Text("Batal")
            }
        },
    )
}

