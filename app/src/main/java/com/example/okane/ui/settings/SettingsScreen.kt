package com.example.okane.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.okane.domain.Sources
import com.example.okane.ui.components.OkaneButton
import com.example.okane.ui.components.OkaneCard
import com.example.okane.ui.components.OkaneOutlinedButton
import com.example.okane.ui.components.SectionHeader
import com.example.okane.ui.components.StatusChip
import com.example.okane.ui.theme.IncomeGreen
import com.example.okane.ui.theme.OkaneTheme

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pengaturan", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Dompetku",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        SettingsScreen(
            postNotificationsGranted = postNotificationsGranted,
            listenerAccessGranted = listenerAccessGranted,
            sourceToggles = sourceToggles,
            capturedSampleCount = capturedSampleCount,
            onOpenListenerSettings = onOpenListenerSettings,
            onSourceToggled = onSourceToggled,
            onDumpSamples = onDumpSamples,
            modifier = Modifier.padding(innerPadding),
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
    val grantedColor = IncomeGreen
    val deniedColor = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Permissions ---
        OkaneCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader("Permissions")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notification access",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    text = if (listenerAccessGranted) "Granted" else "Off",
                    color = if (listenerAccessGranted) grantedColor else deniedColor
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Post notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    text = if (postNotificationsGranted) "Granted" else "Off",
                    color = if (postNotificationsGranted) grantedColor else deniedColor
                )
            }

            if (!listenerAccessGranted) {
                Text(
                    "Okane can't see any notifications yet. Open settings, " +
                        "find \"Okane\", and turn it on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            OkaneButton(
                onClick = onOpenListenerSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Text("Open notification access settings")
            }
        }

        // --- Per-app toggles ---
        OkaneCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader("Tracked apps")
            Text(
                "Turn a source off to stop recording its transactions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Sources.ALL.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
        OkaneCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader("Sample collection · setup phase")
            Text(
                "Captured: $capturedSampleCount financial-looking notifications",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                "Okane is collecting real notification samples to build its " +
                    "parsers. Stored only on this phone. Press below and copy " +
                    "them from Logcat (tag \"OkaneSamples\").",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            OkaneOutlinedButton(
                onClick = onDumpSamples,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Text("Print captured samples to Logcat")
            }
        }
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
