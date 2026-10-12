package com.example.okane.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.okane.ui.illustration.SkyTime
import com.example.okane.ui.theme.OkaneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsOnlyTheGreetingAndTheTotal() {
        composeRule.setContent {
            OkaneTheme {
                DashboardScreen(greeting = "Konnichiwa", totalMoney = 10_133_000, skyTime = SkyTime.DAY)
            }
        }

        composeRule.onNodeWithTag("homeGreeting").assertIsDisplayed()
        composeRule.onNodeWithText("Konnichiwa").assertIsDisplayed()
        composeRule.onNodeWithTag("homeTotal").assertIsDisplayed()
        composeRule.onNodeWithText("Rp10.133.000").assertIsDisplayed()
        composeRule.onNodeWithText("Total uangmu").assertIsDisplayed()

        // The old Home content is gone for good.
        composeRule.onNodeWithText("Catat cepat").assertDoesNotExist()
        composeRule.onNodeWithTag("homeSettings").assertDoesNotExist()
        composeRule.onNodeWithTag("captureInboxCard").assertDoesNotExist()
    }
}
