package com.example.okane.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class OkaneRouteTest {

    @Test
    fun splashComesFirst() {
        assertEquals(OkaneRoute.Splash, okaneRoute(false, false, true, true))
    }

    @Test
    fun onboardingBeforeAnythingElse() {
        assertEquals(OkaneRoute.Onboarding, okaneRoute(true, false, true, true))
    }

    @Test
    fun settingsWinsOverKeypadAsBefore() {
        assertEquals(OkaneRoute.Settings, okaneRoute(true, true, true, true))
    }

    @Test
    fun keypadThenMain() {
        assertEquals(OkaneRoute.Keypad, okaneRoute(true, true, false, true))
        assertEquals(OkaneRoute.Main, okaneRoute(true, true, false, false))
    }
}
