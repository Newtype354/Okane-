package com.example.okane.ui.capture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.components.CalmBottomSheet
import com.example.okane.ui.components.CalmCard
import com.example.okane.ui.components.CalmChip
import com.example.okane.ui.components.CalmPrimaryButton
import com.example.okane.ui.components.CalmSecondaryButton
import com.example.okane.ui.components.IconBadge
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.theme.ExpenseRed
import com.example.okane.ui.theme.ExpenseSoft
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.IncomeSoft
import com.example.okane.ui.theme.OkaneMotion
import com.example.okane.ui.theme.Peach
import com.example.okane.ui.theme.PeachAccent
import com.example.okane.ui.theme.PeachInk
import com.example.okane.ui.theme.Stone
import com.example.okane.ui.theme.TransferBlue
import com.example.okane.ui.theme.TransferSoft

/**
 * The quiet nudge at the top of Riwayat: notifications waiting for a yes or
 * no. Hidden when the inbox is empty, so a fully automatic day leaves the
 * page unchanged.
 */
@Composable
fun CaptureInboxCard(
    state: CaptureInboxUiState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.pendingCount > 0,
        enter = fadeIn(OkaneMotion.enter()),
        exit = fadeOut(OkaneMotion.exit()),
        modifier = modifier,
    ) {
        val first = state.current
        CalmCard(
            // Spacing lives inside the visibility, so an empty inbox leaves no gap.
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .testTag("captureInboxCard"),
            color = Peach,
            border = false,
            onClick = onOpen,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = OkaneIcons.Inbox,
                    container = MaterialTheme.colorScheme.surface,
                    tint = PeachAccent,
                    size = 44.dp,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(
                        "${state.pendingCount} transaksi perlu konfirmasi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PeachInk,
                    )
                    if (first != null) {
                        Text(
                            "${first.sourceApp} · ${Formatters.rupiah(first.amount)} · ${first.timeLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PeachInk.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Icon(
                    OkaneIcons.ChevronRight,
                    contentDescription = null,
                    tint = PeachInk,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Reviews one captured notification at a time: the original text, what Okane
 * thinks it was, and two answers. Choices are pre-filled from the text and
 * the wallets that belong to the app; anything can be changed before "Catat".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureReviewSheet(
    state: CaptureInboxUiState,
    actions: CaptureInboxActions,
) {
    val card = state.current ?: return
    if (!state.sheetOpen) return
    CalmBottomSheet(
        onDismissRequest = actions.onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 18.dp)
                .navigationBarsPadding()
                .testTag("captureReviewSheet"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Perlu konfirmasi",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "1 dari ${state.pendingCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedContent(
                targetState = card.id,
                transitionSpec = {
                    (slideInHorizontally(OkaneMotion.enter()) { it / 6 } + fadeIn(OkaneMotion.enter())) togetherWith
                        (slideOutHorizontally(OkaneMotion.exit()) { -it / 6 } + fadeOut(OkaneMotion.exit()))
                },
                label = "captureCard",
            ) { _ ->
                ReviewBody(card = card, state = state, actions = actions)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewBody(
    card: SuggestionCard,
    state: CaptureInboxUiState,
    actions: CaptureInboxActions,
) {
    val selection = state.selection
    val type = selection?.type
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            listOfNotNull(card.sourceApp, card.channelLabel, card.timeLabel).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            Formatters.rupiah(card.amount),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp,
                fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.SemiBold,
            color = when (type) {
                TransactionType.IN -> IncomeGreen
                TransactionType.OUT -> ExpenseRed
                TransactionType.TRANSFER -> TransferBlue
                null -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 4.dp),
        )
        card.counterparty?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // The notification itself, so the user judges the real words.
        Column(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Stone)
                .padding(14.dp),
        ) {
            card.title?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                card.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }

        card.duplicateNote?.let { note ->
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Peach)
                    .padding(12.dp)
                    .testTag("captureDuplicateNote"),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    OkaneIcons.Info,
                    contentDescription = null,
                    tint = PeachInk,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = PeachInk,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Label("Jenis")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeChip("Keluar", TransactionType.OUT, type, ExpenseSoft, ExpenseRed, actions)
            TypeChip("Masuk", TransactionType.IN, type, IncomeSoft, IncomeGreen, actions)
            TypeChip("Pindah saldo", TransactionType.TRANSFER, type, TransferSoft, TransferBlue, actions)
        }

        Label(if (type == TransactionType.TRANSFER) "Dari dompet" else "Dompet")
        WalletChips(
            wallets = state.wallets,
            selected = selection?.walletId,
            onSelect = actions.onSelectWallet,
            tagPrefix = "captureWallet",
        )
        if (type == TransactionType.TRANSFER) {
            Label("Ke dompet")
            WalletChips(
                wallets = state.wallets.filter { it.id != selection.walletId },
                selected = selection.toWalletId,
                onSelect = actions.onSelectToWallet,
                tagPrefix = "captureToWallet",
            )
        }

        state.message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CalmSecondaryButton(
                text = "Abaikan",
                onClick = actions.onDismiss,
                enabled = !state.busy,
                modifier = Modifier
                    .weight(1f)
                    .testTag("captureDismiss"),
            )
            CalmPrimaryButton(
                text = "Catat",
                onClick = actions.onConfirm,
                enabled = !state.busy && selection?.complete == true,
                icon = OkaneIcons.Check,
                modifier = Modifier
                    .weight(1f)
                    .testTag("captureConfirm"),
            )
        }
        Text(
            "Abaikan jika ini bukan transaksimu. Notifikasinya tetap tersimpan " +
                "sebagai contoh agar Okane makin pintar mengenali formatnya.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun TypeChip(
    label: String,
    type: TransactionType,
    selected: TransactionType?,
    soft: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    actions: CaptureInboxActions,
) {
    CalmChip(
        text = label,
        selected = selected == type,
        onClick = { actions.onSelectType(type) },
        selectedContainer = soft,
        selectedContent = accent,
        modifier = Modifier.testTag("captureType-${type.name}"),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletChips(
    wallets: List<WalletChoice>,
    selected: Long?,
    onSelect: (Long) -> Unit,
    tagPrefix: String,
) {
    if (wallets.isEmpty()) {
        Text(
            "Belum ada dompet lain. Tambahkan di Dompetku.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        wallets.forEach { wallet ->
            CalmChip(
                text = wallet.name,
                selected = wallet.id == selected,
                onClick = { onSelect(wallet.id) },
                modifier = Modifier.testTag("$tagPrefix-${wallet.id}"),
            )
        }
    }
}
