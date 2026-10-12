package com.example.okane

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.okane.domain.capture.BatteryGuide
import com.example.okane.ui.OkaneMainScreen
import com.example.okane.ui.capture.CaptureViewModel
import com.example.okane.ui.rememberOkaneRootPagerState
import com.example.okane.ui.splash.OkaneSplashScreen
import com.example.okane.ui.SettingsViewModel
import com.example.okane.ui.TransactionViewModel
import com.example.okane.ui.keypad.KeypadScreen
import com.example.okane.ui.keypad.KeypadViewModel
import com.example.okane.ui.navigation.OkaneRoute
import com.example.okane.ui.navigation.okaneRoute
import com.example.okane.ui.navigation.okaneRouteTransition
import com.example.okane.ui.onboarding.OnboardingScreen
import com.example.okane.ui.onboarding.OnboardingViewModel
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.transactions.HistoryActions
import com.example.okane.ui.settings.SettingsSecondaryScreen
import com.example.okane.ui.wallets.WalletActions
import com.example.okane.ui.wallets.WalletsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Hilt builds both ViewModels (with their repositories) because of
    // @AndroidEntryPoint + @HiltViewModel.
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val keypadViewModel: KeypadViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val walletsViewModel: WalletsViewModel by viewModels()
    private val captureViewModel: CaptureViewModel by viewModels()

    // mutableStateOf: Compose re-draws automatically whenever these change.
    private var postNotificationsGranted by mutableStateOf(false)
    private var listenerAccessGranted by mutableStateOf(false)
    private var batteryUnrestricted by mutableStateOf(true)

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            postNotificationsGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate(): hooks this activity into the
        // system splash window (Theme.Okane.Starting in the manifest) and
        // swaps back to Theme.Okane once the activity is drawn.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // The calm palette is light-only, so system bar icons are always dark,
        // whatever the device's dark-mode setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(LIGHT_NAV_SCRIM, DARK_NAV_SCRIM),
        )
        // Only *check* the permission here. The request dialog itself waits
        // until the splash animation is over so it never covers the logo.
        postNotificationsGranted = isPostNotificationsGranted()
        setContent {
            OkaneTheme {
                val historyUiState by transactionViewModel.historyUiState.collectAsState()
                val homeUiState by transactionViewModel.homeUiState.collectAsState()
                val walletsUiState by walletsViewModel.uiState.collectAsState()
                val sourceToggles by settingsViewModel.sourceToggles.collectAsState()
                val capturedCount by settingsViewModel.capturedSampleCount.collectAsState()
                val captureHealth by settingsViewModel.health.collectAsState()
                val captureInbox by captureViewModel.uiState.collectAsState()

                // rememberSaveable survives rotation, so the splash only
                // plays on a fresh launch, not on every configuration change.
                var splashDone by rememberSaveable { mutableStateOf(false) }

                // Surface B is a full-screen takeover rather than a tab: it has
                // its own close affordance and no bottom bar. Still no
                // navigation library — one boolean is the whole back stack.
                var showKeypad by rememberSaveable { mutableStateOf(false) }
                var showSettings by rememberSaveable { mutableStateOf(false) }

                // Hoisted above the full-screen router branches so returning
                // from the keypad restores the same root page. The pager is
                // only composed (and therefore swipeable) on the main surface.
                val rootPagerState: PagerState = rememberOkaneRootPagerState()

                // Read synchronously from SharedPreferences on the first
                // composition, so a returning user never sees onboarding flash.
                val onboardingCompleted by onboardingViewModel.completed.collectAsState()

                val route = okaneRoute(
                    splashDone = splashDone,
                    onboardingCompleted = onboardingCompleted,
                    showSettings = showSettings,
                    showKeypad = showKeypad,
                )

                // Each surface keeps its saveable state (list scroll
                // positions, open filters) while another one is on screen,
                // so returning from the keypad lands exactly where you were.
                val saveableStateHolder = rememberSaveableStateHolder()

                AnimatedContent(
                    targetState = route,
                    transitionSpec = { okaneRouteTransition() },
                    label = "route",
                ) { target ->
                    saveableStateHolder.SaveableStateProvider(target.name) {
                        when (target) {
                            OkaneRoute.Splash -> OkaneSplashScreen(
                                onFinished = {
                                    splashDone = true
                                    askForPostNotificationsIfNeeded()
                                }
                            )

                            OkaneRoute.Onboarding -> {
                                val onboardingState by onboardingViewModel.uiState.collectAsState()

                                // Back steps through the slides rather than
                                // leaving a half-set-up app; nothing is written
                                // until Selesai.
                                BackHandler(enabled = onboardingState.slide > 0) {
                                    onboardingViewModel.back()
                                }

                                OnboardingScreen(
                                    state = onboardingState,
                                    onNext = onboardingViewModel::next,
                                    onBack = onboardingViewModel::back,
                                    onToggleWallet = onboardingViewModel::toggleWallet,
                                    onWalletInitialBalance = onboardingViewModel::setWalletInitialBalance,
                                    onWalletMonthlyFee = onboardingViewModel::setWalletMonthlyFee,
                                    onAddCustomWallet = onboardingViewModel::addCustomWallet,
                                    onToggleCategory = onboardingViewModel::toggleCategory,
                                    onAddCustomCategory = onboardingViewModel::addCustomCategory
                                )
                            }

                            OkaneRoute.Settings -> SettingsSecondaryScreen(
                                postNotificationsGranted = postNotificationsGranted,
                                listenerAccessGranted = listenerAccessGranted,
                                sourceToggles = sourceToggles,
                                capturedSampleCount = capturedCount,
                                onOpenListenerSettings = {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                                onSourceToggled = settingsViewModel::setSourceEnabled,
                                onShareSamples = { settingsViewModel.shareSamples(::shareText) },
                                onBack = { showSettings = false },
                                health = captureHealth,
                                batteryUnrestricted = batteryUnrestricted,
                                batteryTip = BatteryGuide.tipFor(Build.MANUFACTURER),
                                onOpenBatterySettings = ::openBatterySettings,
                            )

                            OkaneRoute.Keypad -> {
                                val keypadState by keypadViewModel.state.collectAsState()
                                val keypadWallets by keypadViewModel.wallets.collectAsState()
                                val chipCategories by keypadViewModel.chipCategories.collectAsState()
                                val allCategories by keypadViewModel.allCategories.collectAsState()
                                val pendingUndo by keypadViewModel.pendingUndo.collectAsState()

                                BackHandler { showKeypad = false }

                                KeypadScreen(
                                    state = keypadState,
                                    wallets = keypadWallets,
                                    chipCategories = chipCategories,
                                    allCategories = allCategories,
                                    pendingUndo = pendingUndo,
                                    onKey = keypadViewModel::press,
                                    onSelectWallet = keypadViewModel::selectWallet,
                                    onSelectToWallet = keypadViewModel::selectToWallet,
                                    onToggleTransfer = keypadViewModel::setTransfer,
                                    onToggleCategory = keypadViewModel::toggleCategory,
                                    onCreateWallet = keypadViewModel::createWallet,
                                    onCreateCategory = keypadViewModel::createCategory,
                                    onPickDate = keypadViewModel::setOccurredAt,
                                    onCommit = keypadViewModel::commit,
                                    onCommitTransfer = keypadViewModel::commitTransfer,
                                    onUndo = keypadViewModel::undo,
                                    onUndoExpired = keypadViewModel::dismissUndo,
                                    onClose = { showKeypad = false }
                                )
                            }

                            OkaneRoute.Main -> OkaneMainScreen(
                                pagerState = rootPagerState,
                                historyUiState = historyUiState,
                                historyActions = HistoryActions(
                                    onPeriodSelected = transactionViewModel::selectHistoryPeriod,
                                    onTypeSelected = transactionViewModel::selectHistoryType,
                                    onShowUncategorised =
                                        transactionViewModel::showUncategorisedHistory,
                                    onShowAllTransactions =
                                        transactionViewModel::showAllHistoryTransactions,
                                    onResetFilters = transactionViewModel::resetHistoryFilters,
                                    onOpenDetail = transactionViewModel::openTransactionDetail,
                                    onDismissDetail = transactionViewModel::dismissTransactionDetail,
                                    onOpenCategoryPicker =
                                        transactionViewModel::openHistoryCategoryPicker,
                                    onDismissCategoryPicker =
                                        transactionViewModel::dismissHistoryCategoryPicker,
                                    onAssignCategory = transactionViewModel::assignCategory,
                                ),
                                walletsUiState = walletsUiState,
                                walletActions = WalletActions(
                                    onOpenSettings = { showSettings = true },
                                    onOpenAdd = walletsViewModel::openAddWallet,
                                    onOpenDetail = walletsViewModel::openDetail,
                                    onOpenArchived = walletsViewModel::openArchived,
                                    onBack = walletsViewModel::navigateBack,
                                    onOpenEdit = walletsViewModel::openEditWallet,
                                    onDismissEditor = walletsViewModel::dismissEditor,
                                    onEditorNameChanged = walletsViewModel::updateEditorName,
                                    onEditorInitialBalanceChanged =
                                        walletsViewModel::updateEditorInitialBalance,
                                    onEditorMonthlyFeeChanged =
                                        walletsViewModel::updateEditorMonthlyFee,
                                    onEditorDefaultChanged = walletsViewModel::updateEditorDefault,
                                    onSaveEditor = walletsViewModel::saveEditor,
                                    onMakeDefault = walletsViewModel::makeSelectedDefault,
                                    onRequestArchive = walletsViewModel::requestArchiveSelected,
                                    onDismissArchive = walletsViewModel::dismissArchiveConfirmation,
                                    onConfirmArchive = walletsViewModel::confirmArchive,
                                    onRestore = walletsViewModel::restoreSelected,
                                    onFeedbackShown = walletsViewModel::consumeFeedback,
                                ),
                                homeUiState = homeUiState,
                                captureInbox = captureInbox,
                                captureActions = captureViewModel.actions,
                                // Each entry point opens the keypad in the mode
                                // its label promises; the typed amount is kept.
                                onAddTransaction = {
                                    keypadViewModel.setTransfer(false)
                                    showKeypad = true
                                },
                                onAddTransfer = {
                                    keypadViewModel.setTransfer(true)
                                    showKeypad = true
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check when the user comes back from the settings screen,
        // so the status on screen is always current.
        listenerAccessGranted = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)
        batteryUnrestricted = getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(packageName) ?: true
    }

    /**
     * The system list of battery-optimised apps. Not the direct "allow?"
     * dialog: that needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which Play
     * reserves for apps whose core function breaks without it.
     */
    private fun openBatterySettings() {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.fromParts("package", packageName, null))
            )
        }
    }

    /** The system share sheet; the user picks where the samples go. */
    private fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Sampel notifikasi Okane")
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "Bagikan sampel"))
    }

    private fun isPostNotificationsGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // The permission does not exist before Android 13; nothing to ask.
            true
        }

    private companion object {
        // Same scrims androidx.activity uses for its default light/dark bars.
        val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }

    private fun askForPostNotificationsIfNeeded() {
        if (!postNotificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
