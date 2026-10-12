package com.example.okane.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.okane.domain.Greeting
import com.example.okane.ui.capture.CaptureInboxActions
import com.example.okane.ui.capture.CaptureInboxUiState
import com.example.okane.ui.capture.CaptureReviewSheet
import com.example.okane.ui.components.CalmFab
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.dashboard.DashboardScreen
import com.example.okane.ui.dashboard.HomeActions
import com.example.okane.ui.dashboard.HomeUiState
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.theme.OkaneMotion
import com.example.okane.ui.transactions.HistoryActions
import com.example.okane.ui.transactions.HistoryUiState
import com.example.okane.ui.transactions.TransactionListScreen
import com.example.okane.ui.wallets.WalletActions
import com.example.okane.ui.wallets.WalletsScreen
import com.example.okane.ui.wallets.WalletsUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Root layout: a horizontal pager under a floating navigation bar.
 *
 * Deliberately not using Navigation Compose: three top-level destinations with
 * no back stack share one [PagerState] as their navigation state.
 */
private enum class OkaneTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", OkaneIcons.Home),
    TRANSACTIONS("Riwayat", OkaneIcons.History),
    WALLETS("Dompetku", OkaneIcons.Wallet)
}

/**
 * Kept by MainActivity across its full-screen keypad branch. Foundation's
 * saveable pager state also restores the selected root page after recreation.
 */
@Composable
fun rememberOkaneRootPagerState(): PagerState = rememberPagerState(
    pageCount = { OkaneTab.entries.size }
)

@Composable
fun OkaneMainScreen(
    pagerState: PagerState = rememberOkaneRootPagerState(),
    historyUiState: HistoryUiState,
    historyActions: HistoryActions,
    walletsUiState: WalletsUiState,
    walletActions: WalletActions,
    onAddTransaction: () -> Unit,
    onAddTransfer: () -> Unit = onAddTransaction,
    homeUiState: HomeUiState = HomeUiState(),
    captureInbox: CaptureInboxUiState = CaptureInboxUiState(),
    captureActions: CaptureInboxActions = CaptureInboxActions.None,
) {
    val coroutineScope = rememberCoroutineScope()
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    val navigationEnabled = !historyUiState.hasOpenModal &&
        !walletsUiState.hasOpenSecondarySurface

    fun goTo(tab: OkaneTab) {
        // A settled active item is already at its target. During a swipe or
        // fling, tapping any item (including the selected one) settles there.
        if (tab.ordinal != pagerState.currentPage || pagerState.isScrollInProgress) {
            navigationJob?.cancel()
            navigationJob = coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = tab.ordinal,
                    animationSpec = OkaneMotion.standard(OkaneMotion.LONG),
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = navigationEnabled,
            modifier = Modifier
                .fillMaxSize()
                .testTag("rootPager"),
            // There are only three lightweight, ViewModel-free page trees.
            // Keeping all of them composed preserves scroll and transient UI
            // state, and makes swiping between them jank-free.
            beyondViewportPageCount = OkaneTab.entries.lastIndex,
            key = { page -> OkaneTab.entries[page].name },
        ) { page ->
            val tab = OkaneTab.entries[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("rootPage-${tab.name}")
            ) {
                when (tab) {
                    OkaneTab.DASHBOARD -> DashboardScreen(
                        greeting = Greeting.now(),
                        totalMoney = walletsUiState.totalMoney,
                        activeWalletCount = walletsUiState.activeWallets.size,
                        state = homeUiState,
                        actions = HomeActions(
                            onAddTransaction = onAddTransaction,
                            onAddTransfer = onAddTransfer,
                            onOpenHistory = { goTo(OkaneTab.TRANSACTIONS) },
                            onOpenWallets = { goTo(OkaneTab.WALLETS) },
                            onOpenSettings = walletActions.onOpenSettings,
                            onOpenCaptureInbox = captureActions.onOpen,
                        ),
                        captureInbox = captureInbox,
                        modifier = Modifier.fillMaxSize()
                    )

                    OkaneTab.TRANSACTIONS -> RootPageScaffold(
                        title = tab.label,
                        subtitle = "Semua catatan keuanganmu",
                        onAddTransaction = onAddTransaction,
                    ) { contentModifier ->
                        TransactionListScreen(
                            state = historyUiState,
                            actions = historyActions,
                            onAddTransaction = onAddTransaction,
                            modifier = contentModifier
                        )
                    }

                    OkaneTab.WALLETS -> WalletsScreen(
                        state = walletsUiState,
                        actions = walletActions,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        CalmNavigationBar(
            pagerState = pagerState,
            enabled = navigationEnabled,
            onSelect = ::goTo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )

        CaptureReviewSheet(state = captureInbox, actions = captureActions)
    }
}

/**
 * A floating white bar with a soft pill behind the current destination.
 *
 * The pill tracks the pager's live scroll position (page + offset fraction)
 * rather than animating after the swipe settles, so it glides under the finger
 * in lockstep with the content. Its offset is read in the layout phase only,
 * so swiping never recomposes the bar.
 */
@Composable
private fun CalmNavigationBar(
    pagerState: PagerState,
    enabled: Boolean,
    onSelect: (OkaneTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(6.dp),
        ) {
            val itemWidth = maxWidth / OkaneTab.entries.size
            val indicatorColor = MaterialTheme.colorScheme.primaryContainer
            Box(
                modifier = Modifier
                    .offset {
                        val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        IntOffset((itemWidth.toPx() * position).roundToInt(), 0)
                    }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(indicatorColor)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                OkaneTab.entries.forEach { tab ->
                    NavigationItem(
                        tab = tab,
                        selected = pagerState.currentPage == tab.ordinal,
                        enabled = enabled,
                        onClick = { onSelect(tab) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationItem(
    tab: OkaneTab,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = OkaneMotion.standard(OkaneMotion.MEDIUM),
        label = "navTint",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .testTag("rootNavigation-${tab.name}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** Title + subtitle header and the add button, for root pages without their own chrome. */
@Composable
private fun RootPageScaffold(
    title: String,
    subtitle: String,
    onAddTransaction: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(
                        start = CalmTokens.screenPadding,
                        end = CalmTokens.screenPadding,
                        top = 16.dp,
                        bottom = 4.dp,
                    ),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            content(Modifier.weight(1f))
        }
        // Manual entry remains reachable from Riwayat as well as Home.
        CalmFab(
            onClick = onAddTransaction,
            icon = OkaneIcons.Plus,
            contentDescription = "Catat transaksi",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 104.dp),
        )
    }
}
