package com.example.okane.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.okane.domain.Greeting
import com.example.okane.ui.components.GlassSurface
import com.example.okane.ui.components.GlassVariant
import com.example.okane.ui.dashboard.DashboardScreen
import com.example.okane.ui.transactions.TransactionListScreen
import com.example.okane.ui.transactions.HistoryActions
import com.example.okane.ui.transactions.HistoryUiState
import com.example.okane.ui.wallets.WalletActions
import com.example.okane.ui.wallets.WalletMaterialIcon
import com.example.okane.ui.wallets.WalletsScreen
import com.example.okane.ui.wallets.WalletsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Root layout: a horizontal pager under a stationary glass bottom bar.
 *
 * Deliberately not using Navigation Compose: three top-level destinations with
 * no back stack share one [PagerState] as their navigation state.
 */
private enum class OkaneTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Filled.Home),
    TRANSACTIONS("Riwayat", Icons.AutoMirrored.Filled.List),
    WALLETS("Dompetku", WalletMaterialIcon)
}

/**
 * Kept by MainActivity across its full-screen keypad branch. Foundation's
 * saveable pager state also restores the selected root page after recreation.
 */
@Composable
fun rememberOkaneRootPagerState(): PagerState = rememberPagerState(
    pageCount = { OkaneTab.entries.size }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OkaneMainScreen(
    pagerState: PagerState = rememberOkaneRootPagerState(),
    historyUiState: HistoryUiState,
    historyActions: HistoryActions,
    walletsUiState: WalletsUiState,
    walletActions: WalletActions,
    onAddTransaction: () -> Unit
) {
    val currentTab = OkaneTab.entries[pagerState.currentPage]
    val coroutineScope = rememberCoroutineScope()
    var navigationJob by remember { mutableStateOf<Job?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !historyUiState.hasOpenModal &&
                !walletsUiState.hasOpenSecondarySurface,
            modifier = Modifier
                .fillMaxSize()
                .testTag("rootPager"),
            // There are only three lightweight, ViewModel-free page trees.
            // Keeping all of them composed preserves scroll and transient UI
            // state without creating resources on every page change.
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
                        onAddTransaction = onAddTransaction,
                        modifier = Modifier.fillMaxSize()
                    )

                    OkaneTab.TRANSACTIONS -> RootPageScaffold(
                        tab = tab,
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

        GlassSurface(
            variant = GlassVariant.Navigation,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0),
            ) {
                OkaneTab.entries.forEach { tab ->
                    val page = tab.ordinal
                    NavigationBarItem(
                        selected = currentTab == tab,
                        enabled = !historyUiState.hasOpenModal &&
                            !walletsUiState.hasOpenSecondarySurface,
                        onClick = {
                            // A settled active item is already at its target.
                            // During a swipe/fling, tapping any item (including
                            // the selected one) explicitly settles there.
                            if (page != pagerState.currentPage || pagerState.isScrollInProgress) {
                                navigationJob?.cancel()
                                navigationJob = coroutineScope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                            }
                        },
                        modifier = Modifier.testTag("rootNavigation-${tab.name}"),
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.72f
                            ),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootPageScaffold(
    tab: OkaneTab,
    onAddTransaction: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        // Manual entry remains reachable from Riwayat as well as Home.
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp)
            ) {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Catat transaksi")
                }
            }
        },
    ) { innerPadding ->
        content(
            Modifier
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(bottom = 92.dp)
        )
    }
}
