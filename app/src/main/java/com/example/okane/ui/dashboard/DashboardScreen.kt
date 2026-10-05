package com.example.okane.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.ui.components.AnimatedAmountText
import com.example.okane.ui.components.CalmCard
import com.example.okane.ui.components.CalmIconButton
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.components.PastelTile
import com.example.okane.ui.components.ProgressRing
import com.example.okane.ui.components.SectionTitle
import com.example.okane.ui.components.calmEntrance
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.illustration.LeafSprig
import com.example.okane.ui.theme.ExpenseRed
import com.example.okane.ui.theme.ExpenseSoft
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.IncomeSoft
import com.example.okane.ui.theme.Lavender
import com.example.okane.ui.theme.LavenderAccent
import com.example.okane.ui.theme.Leaf
import com.example.okane.ui.theme.Mint
import com.example.okane.ui.theme.MintAccent
import com.example.okane.ui.theme.Mist
import com.example.okane.ui.theme.MistAccent
import com.example.okane.ui.theme.OkaneMotion
import com.example.okane.ui.theme.Peach
import com.example.okane.ui.theme.PeachAccent
import com.example.okane.ui.theme.SageMist
import com.example.okane.ui.theme.Stone

/** Everything Home can do, so the screen stays stateless. */
data class HomeActions(
    val onAddTransaction: () -> Unit,
    val onAddTransfer: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenWallets: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/**
 * Home: a calm landing page in the spirit of a mindfulness app. A greeting,
 * the all-wallet total beside a stone cairn, four pastel shortcuts, a gentle
 * recording streak, this month at a glance, and one quiet line to end on.
 *
 * It scrolls vertically; root-page swiping stays horizontal in the pager.
 */
@Composable
fun DashboardScreen(
    greeting: String,
    totalMoney: Long,
    activeWalletCount: Int,
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = CalmTokens.screenPadding),
    ) {
        Header(
            greeting = greeting,
            onOpenSettings = actions.onOpenSettings,
            modifier = Modifier
                .padding(top = 12.dp)
                .calmEntrance(0, "home-header"),
        )

        TotalMoneyCard(
            totalMoney = totalMoney,
            caption = when (activeWalletCount) {
                0 -> "Belum ada dompet aktif"
                1 -> "Dari 1 dompet aktif"
                else -> "Dari $activeWalletCount dompet aktif"
            },
            modifier = Modifier
                .padding(top = 24.dp)
                .calmEntrance(1, "home-hero"),
        )

        SectionTitle(
            "Catat cepat",
            modifier = Modifier
                .padding(top = 28.dp, bottom = 12.dp)
                .calmEntrance(2, "home-quick-title"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .calmEntrance(2, "home-quick"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PastelTile(
                title = "Catat",
                subtitle = "Transaksi",
                icon = OkaneIcons.Pencil,
                container = Mint,
                accent = MintAccent,
                onClick = actions.onAddTransaction,
                modifier = Modifier.weight(1f).testTag("homeAddTransaction"),
            )
            PastelTile(
                title = "Pindah",
                subtitle = "Saldo",
                icon = OkaneIcons.Transfer,
                container = Mist,
                accent = MistAccent,
                onClick = actions.onAddTransfer,
                modifier = Modifier.weight(1f).testTag("homeAddTransfer"),
            )
            PastelTile(
                title = "Riwayat",
                subtitle = "${state.transactionsThisMonth} catatan",
                icon = OkaneIcons.History,
                container = Lavender,
                accent = LavenderAccent,
                onClick = actions.onOpenHistory,
                modifier = Modifier.weight(1f),
            )
            PastelTile(
                title = "Dompet",
                subtitle = "$activeWalletCount aktif",
                icon = OkaneIcons.Wallet,
                container = Peach,
                accent = PeachAccent,
                onClick = actions.onOpenWallets,
                modifier = Modifier.weight(1f),
            )
        }

        SectionTitle(
            "Kebiasaanmu",
            actionLabel = "Lihat riwayat",
            onAction = actions.onOpenHistory,
            modifier = Modifier
                .padding(top = 28.dp, bottom = 12.dp)
                .calmEntrance(3, "home-habit-title"),
        )
        StreakCard(
            state = state,
            onClick = actions.onOpenHistory,
            modifier = Modifier.calmEntrance(3, "home-streak"),
        )

        MonthCard(
            income = state.incomeThisMonth,
            expense = state.expenseThisMonth,
            modifier = Modifier
                .padding(top = 12.dp)
                .calmEntrance(4, "home-month"),
        )

        QuoteCard(
            text = state.quote.text,
            source = state.quote.source,
            modifier = Modifier
                .padding(top = 12.dp)
                .calmEntrance(5, "home-quote"),
        )

        // Room for the floating navigation bar.
        Spacer(Modifier.navigationBarsPadding().height(112.dp))
    }
}

@Composable
private fun Header(
    greeting: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    text = "Tarik napas, catat pelan-pelan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    OkaneIcons.Leaf,
                    contentDescription = null,
                    tint = Leaf,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(18.dp),
                )
            }
        }
        CalmIconButton(
            icon = OkaneIcons.Settings,
            contentDescription = "Pengaturan",
            onClick = onOpenSettings,
            container = Stone,
            size = 48.dp,
            modifier = Modifier.testTag("homeSettings"),
        )
    }
}

@Composable
private fun StreakCard(
    state: HomeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val streak = state.streakDays
    val title = when {
        streak == 0 -> "Mulai hari ini"
        state.recordedToday -> "Pertahankan!"
        else -> "Lanjutkan hari ini"
    }
    val body = when {
        streak == 0 -> "Satu catatan kecil sudah cukup untuk memulai."
        state.recordedToday -> "Kamu sudah mencatat $streak hari berturut-turut."
        else -> "$streak hari beruntun. Catat satu transaksi agar tidak putus."
    }
    CalmCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                ProgressRing(
                    progress = state.streakProgress,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    trackColor = SageMist,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$streak",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "hari",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp),
            ) {
                Text(
                    "Catatan beruntun",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Target berikutnya: ${state.nextMilestone} hari",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Icon(
                OkaneIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun MonthCard(
    income: Long,
    expense: Long,
    modifier: Modifier = Modifier,
) {
    CalmCard(modifier = modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        Text(
            "Bulan ini",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthMetric(
                label = "Masuk",
                amount = income,
                icon = OkaneIcons.ArrowDownLeft,
                accent = IncomeGreen,
                soft = IncomeSoft,
                modifier = Modifier.weight(1f),
            )
            MonthMetric(
                label = "Keluar",
                amount = expense,
                icon = OkaneIcons.ArrowUpRight,
                accent = ExpenseRed,
                soft = ExpenseSoft,
                modifier = Modifier.weight(1f),
            )
        }
        BalanceBar(
            income = income,
            expense = expense,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = when {
                income == 0L && expense == 0L -> "Belum ada catatan bulan ini."
                expense <= income -> "Pengeluaranmu masih di bawah pemasukan. Tenang."
                else -> "Pengeluaran melewati pemasukan bulan ini."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun MonthMetric(
    label: String,
    amount: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    soft: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(soft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedAmountText(
                value = amount,
                format = Formatters::rupiah,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = accent,
            )
        }
    }
}

/** Income and expense as two soft segments of one bar; widths glide on change. */
@Composable
private fun BalanceBar(income: Long, expense: Long, modifier: Modifier = Modifier) {
    val total = (income + expense).toDouble()
    val target = if (total <= 0.0) 0.5f else (income / total).toFloat()
    val incomeShare by animateFloatAsState(
        targetValue = target,
        animationSpec = OkaneMotion.enter(duration = 700),
        label = "incomeShare",
    )
    val empty = total <= 0.0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (incomeShare > 0.001f) {
            Box(
                Modifier
                    .weight(incomeShare)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (empty) Stone else IncomeGreen.copy(alpha = 0.75f))
            )
        }
        if (incomeShare < 0.999f) {
            Box(
                Modifier
                    .weight(1f - incomeShare)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (empty) Stone else ExpenseRed.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun QuoteCard(
    text: String,
    source: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CalmTokens.cardRadius))
            .background(Stone),
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 18.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "“",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "– $source",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            LeafSprig(modifier = Modifier.size(width = 72.dp, height = 96.dp))
        }
    }
}
