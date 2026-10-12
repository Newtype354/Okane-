package com.example.okane.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class OkaneRouteTest {

    @Test
    fun onboardingBeforeAnythingElse() {
        assertEquals(OkaneRoute.Onboarding, okaneRoute(false, true, true))
    }

    @Test
    fun settingsWinsOverKeypadAsBefore() {
        assertEquals(OkaneRoute.Settings, okaneRoute(true, true, true))
    }

    @Test
    fun keypadThenMain() {
        assertEquals(OkaneRoute.Keypad, okaneRoute(true, false, true))
        assertEquals(OkaneRoute.Main, okaneRoute(true, false, false))
    }
}
