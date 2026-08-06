package com.example.okane.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.okane.domain.keypad.KeypadFormat
import com.example.okane.domain.keypad.KeypadState
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.components.OkaneButton
import com.example.okane.ui.components.OkaneCard
import com.example.okane.ui.components.OkaneOutlinedButton
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.theme.OkaneTheme

/**
 * Runs once on first launch. Three slides, every one skippable.
 *
 * Optimised for being finishable, not for completeness: asking for a pile of
 * setup before the user has seen any benefit is the most common reason finance
 * apps get uninstalled on day one. `Lewati` is present on every slide and does
 * the same thing as `Lanjut` minus the deliberation — on the last slide both
 * finish, and the defaults are built so that path still lands on a working app.
 *
 * Stateless (values in, callbacks out), like the rest of the app's screens.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onToggleWallet: (String) -> Unit,
    onWalletInitialBalance: (String, Long) -> Unit,
    onWalletMonthlyFee: (String, Long) -> Unit,
    onAddCustomWallet: (String) -> Unit,
    onToggleCategory: (String, TransactionType) -> Unit,
    onAddCustomCategory: (String, TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            TopBar(slide = state.slide, onSkip = onNext)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (state.slide) {
                    0 -> WelcomeSlide()
                    1 -> WalletSlide(
                        wallets = state.wallets,
                        onToggle = onToggleWallet,
                        onInitialBalance = onWalletInitialBalance,
                        onMonthlyFee = onWalletMonthlyFee,
                        onAddCustom = onAddCustomWallet,
                    )
                    else -> CategorySlide(
                        categories = state.categories,
                        onToggle = onToggleCategory,
                        onAddCustom = onAddCustomCategory,
                    )
                }
            }

            BottomBar(
                slide = state.slide,
                isLastSlide = state.isLastSlide,
                isFinishing = state.isFinishing,
                onBack = onBack,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun TopBar(slide: Int, onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(OnboardingUiState.LAST_SLIDE + 1) { index ->
                val active = index == slide
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (active) 22.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
        Box(Modifier.weight(1f))
        // Present on every slide, including the last, where it finishes.
        TextButton(onClick = onSkip) { Text("Lewati") }
    }
}

@Composable
private fun BottomBar(
    slide: Int,
    isLastSlide: Boolean,
    isFinishing: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (slide > 0) {
            OkaneOutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Kembali")
            }
        }
        OkaneButton(
            onClick = { if (!isFinishing) onNext() },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (isLastSlide) "Selesai" else "Lanjut")
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 1 — welcome
// ---------------------------------------------------------------------------

@Composable
private fun WelcomeSlide() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Selamat datang di Okane",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Okane mencatat pemasukan dan pengeluaran kamu. Catat sendiri lewat " +
                "papan angka dalam beberapa ketukan, dan biarkan Okane menangkap " +
                "transaksi dari notifikasi aplikasi bank dan dompet digital kamu.",
            style = MaterialTheme.typography.bodyLarge,
        )
        // Stated here so the conservative labelling elsewhere in the app reads
        // as honest rather than apologetic.
        OkaneCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Okane tidak terhubung ke bank mana pun.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Semua angka di sini berasal dari apa yang kamu catat dan dari " +
                    "notifikasi yang berhasil dibaca — bukan dari saldo rekening " +
                    "kamu yang sebenarnya.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            "Dua langkah berikutnya bisa dilewati. Kamu bisa mengubah semuanya " +
                "nanti.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Slide 2 — wallets
// ---------------------------------------------------------------------------

@Composable
private fun WalletSlide(
    wallets: List<WalletDraft>,
    onToggle: (String) -> Unit,
    onInitialBalance: (String, Long) -> Unit,
    onMonthlyFee: (String, Long) -> Unit,
    onAddCustom: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SlideHeading(
            title = "Dompet kamu",
            subtitle = "Pilih yang kamu pakai. Lewati saja kalau belum yakin — " +
                "Okane akan membuat dompet \"Tunai\" untuk kamu.",
        )

        wallets.forEach { wallet ->
            WalletRow(
                wallet = wallet,
                onToggle = { onToggle(wallet.name) },
                onInitialBalance = { onInitialBalance(wallet.name, it) },
                onMonthlyFee = { onMonthlyFee(wallet.name, it) },
            )
        }

        AddCustomRow(placeholder = "Nama dompet lain", onAdd = onAddCustom)
    }
}

@Composable
private fun WalletRow(
    wallet: WalletDraft,
    onToggle: () -> Unit,
    onInitialBalance: (Long) -> Unit,
    onMonthlyFee: (Long) -> Unit,
) {
    // Details are revealed on tap and never required inline — a required
    // balance field is exactly the kind of friction that stops people
    // finishing setup.
    var expanded by remember { mutableStateOf(false) }

    OkaneCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = wallet.selected, onCheckedChange = { onToggle() })
            Text(
                wallet.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() },
            )
            if (wallet.selected) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Tutup" else "Atur")
                }
            }
        }

        if (wallet.selected && expanded) {
            AmountField(
                label = "Saldo awal (opsional)",
                value = wallet.initialBalance,
                onValueChange = onInitialBalance,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            AmountField(
                label = "Biaya admin bulanan (opsional)",
                value = wallet.monthlyFee,
                onValueChange = onMonthlyFee,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                // Say so plainly rather than letting the user expect deductions
                // that will never appear.
                "Biaya admin baru disimpan, belum dipotong otomatis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 3 — categories
// ---------------------------------------------------------------------------

@Composable
private fun CategorySlide(
    categories: List<CategoryDraft>,
    onToggle: (String, TransactionType) -> Unit,
    onAddCustom: (String, TransactionType) -> Unit,
) {
    var customSide by remember { mutableStateOf(TransactionType.OUT) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SlideHeading(
            title = "Kategori",
            subtitle = "Sudah dicentang semua. Hapus yang tidak kamu perlukan, " +
                "atau lewati saja.",
        )

        listOf(
            "Pengeluaran" to TransactionType.OUT,
            "Pemasukan" to TransactionType.IN,
        ).forEach { (heading, type) ->
            SectionHeader(heading, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            categories.filter { it.type == type }.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(category.name, category.type) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = category.selected,
                        onCheckedChange = { onToggle(category.name, category.type) },
                    )
                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // A custom category has to belong to exactly one side, so the side is
        // chosen before the name rather than guessed afterwards.
        SectionHeader("Tambah sendiri", modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Pengeluaran" to TransactionType.OUT,
                "Pemasukan" to TransactionType.IN,
            ).forEach { (label, type) ->
                FilterChip(
                    selected = customSide == type,
                    onClick = { customSide = type },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
        AddCustomRow(
            placeholder = "Nama kategori",
            onAdd = { onAddCustom(it, customSide) },
        )
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun SlideHeading(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AddCustomRow(placeholder: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
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
                onAdd(text)
                text = ""
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah")
        }
    }
}

/**
 * Whole-rupiah input. Non-digits are dropped rather than rejected, so a user
 * typing "50.000" out of habit still gets 50000 instead of an error, and the
 * money path never sees a decimal.
 */
@Composable
private fun AmountField(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = if (value == 0L) "" else value.toString(),
        onValueChange = { raw ->
            // Same 12-digit ceiling the keypad enforces, so the two cannot
            // disagree about what fits in a rupiah amount.
            val digits = raw.filter { it.isDigit() }.take(KeypadState.MAX_DIGITS)
            onValueChange(digits.toLongOrNull() ?: 0L)
        },
        label = { Text(label) },
        placeholder = { Text("0") },
        supportingText = { if (value > 0) Text(KeypadFormat.rupiah(value)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun OnboardingWelcomePreview() {
    OkaneTheme {
        OnboardingScreen(
            state = OnboardingUiState(slide = 0),
            onNext = {}, onBack = {}, onToggleWallet = {},
            onWalletInitialBalance = { _, _ -> }, onWalletMonthlyFee = { _, _ -> },
            onAddCustomWallet = {}, onToggleCategory = { _, _ -> },
            onAddCustomCategory = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun OnboardingWalletsPreview() {
    OkaneTheme {
        OnboardingScreen(
            state = OnboardingUiState(slide = 1).let { base ->
                base.copy(
                    wallets = base.wallets.map {
                        if (it.name == "Tunai") it.copy(selected = true) else it
                    }
                )
            },
            onNext = {}, onBack = {}, onToggleWallet = {},
            onWalletInitialBalance = { _, _ -> }, onWalletMonthlyFee = { _, _ -> },
            onAddCustomWallet = {}, onToggleCategory = { _, _ -> },
            onAddCustomCategory = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun OnboardingCategoriesPreview() {
    OkaneTheme {
        OnboardingScreen(
            state = OnboardingUiState(slide = 2),
            onNext = {}, onBack = {}, onToggleWallet = {},
            onWalletInitialBalance = { _, _ -> }, onWalletMonthlyFee = { _, _ -> },
            onAddCustomWallet = {}, onToggleCategory = { _, _ -> },
            onAddCustomCategory = { _, _ -> },
        )
    }
}
