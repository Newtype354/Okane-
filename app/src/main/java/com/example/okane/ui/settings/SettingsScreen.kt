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
import com.example.okane.ui.components.CalmCard
import com.example.okane.ui.components.CalmIconButton
import com.example.okane.ui.components.CalmPrimaryButton
import com.example.okane.ui.components.CalmSecondaryButton
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.components.IconBadge
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.StatusChip
import com.example.okane.ui.icons.OkaneIcons
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.Lavender
import com.example.okane.ui.theme.LavenderInk
import com.example.okane.ui.theme.Mint
import com.example.okane.ui.theme.Mist
import com.example.okane.ui.theme.MistInk
import com.example.okane.ui.theme.OkaneTheme
import com.example.okane.ui.theme.SageInk

@Composable
fun SettingsSecondaryScreen(
    postNotificationsGranted: Boolean,
    listenerAccessGranted: Boolean,
    sourceToggles: Map<String, Boolean>,
    capturedSampleCount: Int,
    onOpenListenerSettings: () -> Unit,
    onSourceToggled: (String, Boolean) -> Unit,
    onDumpSamples: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
            onDumpSamples = onDumpSamples,
            modifier = Modifier.weight(1f),
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
    onDumpSamples: () -> Unit,
    modifier: Modifier = Modifier
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
        }

        // --- Per-app toggles ---
        SectionHeader("Sumber yang dicatat", modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        CalmCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
            Text(
                "Matikan sumber untuk berhenti mencatat transaksinya.",
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
                    Text(
                        source,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
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

        // --- Discovery (temporary section, removed after parser setup) ---
        SectionHeader("Pengumpulan sampel · tahap persiapan", modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        CalmCard(
            modifier = Modifier.fillMaxWidth(),
            color = Lavender,
            border = false,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        ) {
            Text(
                "$capturedSampleCount notifikasi bernuansa keuangan tersimpan",
                style = MaterialTheme.typography.titleSmall,
                color = LavenderInk,
            )
            Text(
                "Okane mengumpulkan contoh notifikasi asli untuk membangun parser. " +
                    "Hanya disimpan di HP ini. Tekan tombol di bawah lalu salin dari " +
                    "Logcat (tag \"OkaneSamples\").",
                style = MaterialTheme.typography.bodySmall,
                color = LavenderInk.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp)
            )
            CalmSecondaryButton(
                text = "Cetak sampel ke Logcat",
                onClick = onDumpSamples,
                bordered = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
        }
        Box(Modifier.padding(bottom = 24.dp))
    }
}

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
            onDumpSamples = {}
        )
    }
}
