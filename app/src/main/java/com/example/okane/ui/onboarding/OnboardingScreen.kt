package com.example.okane.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.okane.domain.keypad.KeypadFormat
import com.example.okane.domain.keypad.KeypadState
import com.example.okane.domain.model.TransactionType
import com.example.okane.ui.components.CalmChip
import com.example.okane.ui.components.CalmIconButton
import com.example.okane.ui.components.CalmPrimaryButton
import com.example.okane.ui.components.CalmSecondaryButton
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.pressScale
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.illustration.StackedStones
import com.example.okane.ui.theme.Leaf
import com.example.okane.ui.theme.Mint
import com.example.okane.ui.theme.OkaneMotion
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.theme.Peach
import com.example.okane.ui.theme.PeachInk
import com.example.okane.ui.theme.SageInk
import com.example.okane.ui.theme.SageMist
import com.example.okane.ui.theme.Stone

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CalmTokens.screenPadding),
    ) {
        TopBar(slide = state.slide, onSkip = onNext)

        // Slides glide sideways in the direction of travel, so forward and
        // back feel like moving along one path rather than swapping pages.
        AnimatedContent(
            targetState = state.slide,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(OkaneMotion.enter(OkaneMotion.LONG)) { direction * it / 3 } +
                    fadeIn(OkaneMotion.enter(OkaneMotion.LONG))) togetherWith
                    (slideOutHorizontally(OkaneMotion.exit()) { -direction * it / 6 } +
                        fadeOut(OkaneMotion.exit()))
            },
            label = "onboardingSlide",
            modifier = Modifier.weight(1f),
        ) { slide ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                when (slide) {
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
                val width by animateDpAsState(
                    targetValue = if (active) 26.dp else 8.dp,
                    animationSpec = OkaneMotion.standard(OkaneMotion.MEDIUM),
                    label = "dotWidth",
                )
                val color by animateColorAsState(
                    targetValue = if (index <= slide) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    animationSpec = OkaneMotion.standard(OkaneMotion.MEDIUM),
                    label = "dotColor",
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Box(Modifier.weight(1f))
        // Present on every slide, including the last, where it finishes.
        TextButton(onClick = onSkip) {
            Text(
                "Lewati",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        AnimatedVisibility(
            visible = slide > 0,
            enter = fadeIn(OkaneMotion.enter()) + expandHorizontally(OkaneMotion.enter()),
            exit = fadeOut(OkaneMotion.exit()) + shrinkHorizontally(OkaneMotion.exit()),
        ) {
            CalmSecondaryButton(
                text = "Kembali",
                onClick = onBack,
                modifier = Modifier.width(132.dp),
            )
        }
        CalmPrimaryButton(
            text = when {
                isFinishing -> "Menyiapkan…"
                isLastSlide -> "Selesai"
                else -> "Lanjut"
            },
            onClick = { if (!isFinishing) onNext() },
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// Slide 1 — welcome
// ---------------------------------------------------------------------------

@Composable
private fun WelcomeSlide() {
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val stones by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = OkaneMotion.enter(duration = 1600, delay = 200),
        label = "welcomeStones",
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(CalmTokens.cardRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(SageMist.copy(alpha = 0.6f), Stone)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            // A pale morning sun behind the cairn.
            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Peach.copy(alpha = 0.9f))
            )
            StackedStones(
                progress = stones,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(width = 220.dp, height = 200.dp),
            )
        }
        Text(
            "Selamat datang di Okane",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "Okane mencatat pemasukan dan pengeluaran kamu. Catat sendiri lewat " +
                "papan angka dalam beberapa ketukan, dan biarkan Okane menangkap " +
                "transaksi dari notifikasi aplikasi bank dan dompet digital kamu.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Stated here so the conservative labelling elsewhere in the app reads
        // as honest rather than apologetic.
        Surface(
            shape = RoundedCornerShape(CalmTokens.tileRadius),
            color = Mint,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    OkaneIcons.Leaf,
                    contentDescription = null,
                    tint = Leaf,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "Okane tidak terhubung ke bank mana pun.",
                        style = MaterialTheme.typography.titleSmall,
                        color = SageInk,
                    )
                    Text(
                        "Semua angka di sini berasal dari apa yang kamu catat dan dari " +
                            "notifikasi yang berhasil dibaca — bukan dari saldo rekening " +
                            "kamu yang sebenarnya.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageInk.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Text(
            "Dua langkah berikutnya bisa dilewati. Kamu bisa mengubah semuanya nanti.",
            style = MaterialTheme.typography.bodySmall,
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Box(Modifier.height(8.dp))
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
    val interaction = remember { MutableInteractionSource() }
    val border by animateColorAsState(
        if (wallet.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outlineVariant,
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "walletBorder",
    )

    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f),
        shape = RoundedCornerShape(CalmTokens.tileRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(CalmTokens.hairline, border),
        interactionSource = interaction,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckDisc(checked = wallet.selected)
                Text(
                    wallet.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                AnimatedVisibility(visible = wallet.selected, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Tutup" else "Atur")
                    }
                }
            }

            AnimatedVisibility(
                visible = wallet.selected && expanded,
                enter = expandVertically(OkaneMotion.enter()) + fadeIn(OkaneMotion.enter()),
                exit = shrinkVertically(OkaneMotion.exit()) + fadeOut(OkaneMotion.exit()),
            ) {
                Column {
                    AmountField(
                        label = "Saldo awal (opsional)",
                        value = wallet.initialBalance,
                        onValueChange = onInitialBalance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    AmountField(
                        label = "Biaya admin bulanan (opsional)",
                        value = wallet.monthlyFee,
                        onValueChange = onMonthlyFee,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    Text(
                        // Say so plainly rather than letting the user expect
                        // deductions that will never appear.
                        "Biaya admin baru disimpan, belum dipotong otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }
}

/** A round check that fills with sage when selected; the tick pops in. */
@Composable
private fun CheckDisc(checked: Boolean) {
    val fill by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "checkFill",
    )
    val scale by animateFloatAsState(
        if (checked) 1f else 0f,
        OkaneMotion.press(),
        label = "checkScale",
    )
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            OkaneIcons.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = scale.coerceIn(0f, 1f)
                },
        )
    }
}

// ---------------------------------------------------------------------------
// Slide 3 — categories
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
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
            subtitle = "Sudah dipilih semua. Ketuk untuk melepas yang tidak kamu " +
                "perlukan, atau lewati saja.",
        )

        listOf(
            "Pengeluaran" to TransactionType.OUT,
            "Pemasukan" to TransactionType.IN,
        ).forEach { (heading, type) ->
            SectionHeader(heading, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.filter { it.type == type }.forEach { category ->
                    CalmChip(
                        text = category.name,
                        selected = category.selected,
                        onClick = { onToggle(category.name, category.type) },
                        leadingIcon = if (category.selected) OkaneIcons.Check else null,
                        selectedContainer = if (type == TransactionType.OUT) Peach else SageMist,
                        selectedContent = if (type == TransactionType.OUT) PeachInk else SageInk,
                    )
                }
            }
        }

        // A custom category has to belong to exactly one side, so the side is
        // chosen before the name rather than guessed afterwards.
        SectionHeader("Tambah sendiri", modifier = Modifier.padding(top = 12.dp, start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Pengeluaran" to TransactionType.OUT,
                "Pemasukan" to TransactionType.IN,
            ).forEach { (label, type) ->
                CalmChip(
                    text = label,
                    selected = customSide == type,
                    onClick = { customSide = type },
                )
            }
        }
        AddCustomRow(
            placeholder = "Nama kategori",
            onAdd = { onAddCustom(it, customSide) },
        )
        Box(Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun SlideHeading(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
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
            shape = RoundedCornerShape(CalmTokens.controlRadius),
            colors = calmFieldColors(),
            modifier = Modifier.weight(1f),
        )
        CalmIconButton(
            icon = OkaneIcons.Plus,
            contentDescription = "Tambah",
            container = MaterialTheme.colorScheme.primary,
            tint = MaterialTheme.colorScheme.onPrimary,
            size = 52.dp,
            onClick = {
                onAdd(text)
                text = ""
            },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun calmFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)

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
        shape = RoundedCornerShape(CalmTokens.controlRadius),
        colors = calmFieldColors(),
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
