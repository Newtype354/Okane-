package com.example.okane.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.okane.domain.Sources
import com.example.okane.ui.theme.OkaneTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSecondaryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backReturnsToWalletAndExistingSourceToggleStillWorks() {
        var backPressed = false
        var toggled: Pair<String, Boolean>? = null
        composeRule.setContent {
            OkaneTheme {
                SettingsSecondaryScreen(
                    postNotificationsGranted = true,
                    listenerAccessGranted = true,
                    sourceToggles = Sources.ALL.associateWith { true },
                    capturedSampleCount = 0,
                    onOpenListenerSettings = {},
                    onSourceToggled = { source, enabled -> toggled = source to enabled },
                    onDumpSamples = {},
                    onBack = { backPressed = true },
                )
            }
        }

        composeRule.onNodeWithTag("settingsSourceSwitch-${Sources.GOPAY}")
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(Sources.GOPAY to false, toggled) }

        composeRule.onNodeWithContentDescription("Kembali ke Dompetku").performClick()
        composeRule.runOnIdle { assertTrue(backPressed) }
    }
}
