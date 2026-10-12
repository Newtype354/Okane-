package com.example.okane.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.okane.domain.Sources
import com.example.okane.domain.capture.CaptureHealthState
import com.example.okane.domain.capture.KnownTemplates
import com.example.okane.domain.capture.PackageTrust
import com.example.okane.domain.capture.SourceCatalog
import com.example.okane.ui.components.CalmCard
import com.example.okane.ui.components.CalmIconButton
import com.example.okane.ui.components.CalmPrimaryButton
import com.example.okane.ui.components.CalmSecondaryButton
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.components.IconBadge
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.StatusChip
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.Lavender
import com.example.okane.ui.theme.LavenderInk
import com.example.okane.ui.theme.Mint
import com.example.okane.ui.theme.Mist
import com.example.okane.ui.theme.MistInk
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.theme.Peach
import com.example.okane.ui.theme.PeachInk
import com.example.okane.ui.theme.SageInk

@Composable
fun SettingsSecondaryScreen(
    postNotificationsGranted: Boolean,
    listenerAccessGranted: Boolean,
    sourceToggles: Map<String, Boolean>,
    capturedSampleCount: Int,
    onOpenListenerSettings: () -> Unit,
    onSourceToggled: (String, Boolean) -> Unit,
    onShareSamples: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    health: CaptureHealthState = CaptureHealthState(),
    batteryUnrestricted: Boolean = true,
    batteryTip: String? = null,
    onOpenBatterySettings: () -> Unit = {},
    now: Long = System.currentTimeMillis(),
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = CalmTokens.screenPadding, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalmIconButton(
                icon = OkaneIcons.ChevronLeft,
                contentDescription = "Kembali ke Dompetku",
                onClick = onBack,
            )
            Text(
                "Pengaturan",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
        SettingsScreen(
            postNotificationsGranted = postNotificationsGranted,
            listenerAccessGranted = listenerAccessGranted,
            sourceToggles = sourceToggles,
            capturedSampleCount = capturedSampleCount,
            onOpenListenerSettings = onOpenListenerSettings,
            onSourceToggled = onSourceToggled,
            onShareSamples = onShareSamples,
            modifier = Modifier.weight(1f),
            health = health,
            batteryUnrestricted = batteryUnrestricted,
            batteryTip = batteryTip,
            onOpenBatterySettings = onOpenBatterySettings,
            now = now,
        )
    }
}

@Composable
fun SettingsScreen(
    postNotificationsGranted: Boolean,
    listenerAccessGranted: Boolean,
    sourceToggles: Map<String, Boolean>,
    capturedSampleCount: Int,
    onOpenListenerSettings: () -> Unit,
    onSourceToggled: (String, Boolean) -> Unit,
    onShareSamples: () -> Unit,
    modifier: Modifier = Modifier,
    health: CaptureHealthState = CaptureHealthState(),
    batteryUnrestricted: Boolean = true,
    batteryTip: String? = null,
    onOpenBatterySettings: () -> Unit = {},
    now: Long = System.currentTimeMillis(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CalmTokens.screenPadding, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Permissions ---
        SectionHeader("Izin", modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        CalmCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            PermissionRow(
                icon = OkaneIcons.Bell,
                title = "Akses notifikasi",
                subtitle = "Agar Okane bisa membaca notifikasi transaksi",
                granted = listenerAccessGranted,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 14.dp),
            )
            PermissionRow(
                icon = OkaneIcons.Info,
                title = "Notifikasi Okane",
                subtitle = "Untuk pemberitahuan dari Okane sendiri",
                granted = postNotificationsGranted,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 14.dp),
            )
            PermissionRow(
                icon = OkaneIcons.Battery,
                title = "Tanpa batasan baterai",
                subtitle = "Agar Okane tidak dimatikan diam-diam oleh sistem",
                granted = batteryUnrestricted,
            )

            if (!listenerAccessGranted) {
                Text(
                    "Okane belum bisa melihat notifikasi. Buka pengaturan, cari " +
                        "\"Okane\", lalu aktifkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            CalmPrimaryButton(
                text = "Buka pengaturan akses notifikasi",
                onClick = onOpenListenerSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
            if (!batteryUnrestricted) {
                CalmSecondaryButton(
                    text = "Buka pengaturan baterai",
                    onClick = onOpenBatterySettings,
                    icon = OkaneIcons.Battery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("settingsBattery")
                )
            }
            batteryTip?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        // --- Health ---
        SectionHeader("Kesehatan pencatatan otomatis", modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        HealthCard(
            health = health,
            listenerAccessGranted = listenerAccessGranted,
            now = now,
        )

        // --- Per-app toggles ---
        SectionHeader("Sumber yang dicatat", modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        CalmCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
            Text(
                "Format yang sudah dikenal dicatat otomatis. Selebihnya masuk ke " +
                    "\"Perlu konfirmasi\" di Home. Matikan sumber untuk berhenti membacanya.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Sources.ALL.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(source, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            sourceStatus(source, health.lastSeenBySource[source], now),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        // Missing key -> true, matching SourcePreferences'
                        // "tracked unless turned off" default.
                        checked = sourceToggles[source] ?: true,
                        onCheckedChange = { onSourceToggled(source, it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        // A horizontal drag that starts on the control belongs
                        // to the control area, not to root-page navigation.
                        // Taps and vertical scrolling remain unaffected.
                        modifier = Modifier
                            .testTag("settingsSourceSwitch-$source")
                            .pointerInput(source) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                }
                            }
                    )
                }
            }
        }

        // --- Samples: what new formats are learned from ---
        SectionHeader("Contoh notifikasi", modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        CalmCard(
            modifier = Modifier.fillMaxWidth(),
            color = Lavender,
            border = false,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        ) {
            Text(
                "$capturedSampleCount notifikasi keuangan tersimpan",
                style = MaterialTheme.typography.titleSmall,
                color = LavenderInk,
            )
            Text(
                "Hanya dari aplikasi keuangan yang didukung, tanpa kode OTP, dan hanya " +
                    "di HP ini. Bagikan untuk dibuatkan format baru; nomor rekening dan " +
                    "nama penerima disensor. Periksa dulu sebelum mengirim.",
                style = MaterialTheme.typography.bodySmall,
                color = LavenderInk.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp)
            )
            CalmSecondaryButton(
                text = "Bagikan sampel",
                onClick = onShareSamples,
                icon = OkaneIcons.Share,
                bordered = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .testTag("settingsShareSamples")
            )
        }
        Box(Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun HealthCard(
    health: CaptureHealthState,
    listenerAccessGranted: Boolean,
    now: Long,
) {
    val last = health.lastNotificationAt
    val quiet = last == null || now - last > QUIET_AFTER_MILLIS
    CalmCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settingsHealth"),
        color = if (listenerAccessGranted && !quiet) Mint else Peach,
        border = false,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        val ink = if (listenerAccessGranted && !quiet) SageInk else PeachInk
        Text(
            when {
                !listenerAccessGranted -> "Belum bisa membaca notifikasi"
                last == null -> "Menunggu notifikasi pertama"
                quiet -> "Sudah lama tidak ada notifikasi"
                else -> "Berjalan normal"
            },
            style = MaterialTheme.typography.titleSmall,
            color = ink,
        )
        HealthLine("Notifikasi keuangan terakhir", last?.let { Formatters.relative(it, now) }, ink)
        HealthLine("Terakhir dicatat otomatis", health.lastRecordedAt?.let { Formatters.relative(it, now) }, ink)
        HealthLine("Layanan tersambung", health.listenerConnectedAt?.let { Formatters.relative(it, now) }, ink)
        if (listenerAccessGranted && quiet) {
            Text(
                "Jika kamu bertransaksi tapi tidak ada yang masuk, kemungkinan Okane " +
                    "dihentikan oleh penghemat baterai. Lihat pengaturan baterai di atas.",
                style = MaterialTheme.typography.bodySmall,
                color = ink.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun HealthLine(label: String, value: String?, ink: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.padding(top = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = ink.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
        Text(value ?: "belum ada", style = MaterialTheme.typography.bodySmall, color = ink)
    }
}

/** "Otomatis untuk top up · terlihat 5 menit lalu" and the like. */
internal fun sourceStatus(source: String, lastSeen: Long?, now: Long): String {
    val mode = if (KnownTemplates.ALL.any { it.source == source } &&
        SourceCatalog.byName(source)?.packages?.values?.any { it == PackageTrust.CONFIRMED_ON_DEVICE } == true
    ) {
        "Sebagian otomatis"
    } else {
        "Lewat konfirmasi"
    }
    val seen = lastSeen?.let { "terlihat " + Formatters.relative(it, now) } ?: "belum terlihat di HP ini"
    return "$mode · $seen"
}

/** Three days without a single finance notification usually means the listener died. */
private const val QUIET_AFTER_MILLIS = 3L * 24 * 60 * 60 * 1000

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
            icon = icon,
            container = if (granted) Mint else Mist,
            tint = if (granted) SageInk else MistInk,
            size = 40.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusChip(
            text = if (granted) "Aktif" else "Mati",
            color = if (granted) IncomeGreen else MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    OkaneTheme {
        SettingsScreen(
            postNotificationsGranted = true,
            listenerAccessGranted = false,
            sourceToggles = Sources.ALL.associateWith { true },
            capturedSampleCount = 7,
            onOpenListenerSettings = {},
            onSourceToggled = { _, _ -> },
            onShareSamples = {}
        )
    }
}
