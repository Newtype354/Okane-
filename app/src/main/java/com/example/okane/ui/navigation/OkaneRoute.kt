package com.example.okane.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.example.okane.ui.theme.OkaneMotion

/**
 * The app's top-level surfaces. Still no navigation library: MainActivity
 * derives one of these from a few booleans and animates between them.
 */
enum class OkaneRoute { Onboarding, Main, Keypad, Settings }

/**
 * The routing precedence, kept pure so it can be unit-tested.
 *
 * There is no animated splash route: the system splash covers start-up, and
 * Home's own landscape is the first thing worth looking at. Skipping a
 * second, decorative splash saves almost two seconds on every cold start.
 */
fun okaneRoute(
    onboardingCompleted: Boolean,
    showSettings: Boolean,
    showKeypad: Boolean,
): OkaneRoute = when {
    !onboardingCompleted -> OkaneRoute.Onboarding
    showSettings -> OkaneRoute.Settings
    showKeypad -> OkaneRoute.Keypad
    else -> OkaneRoute.Main
}

/**
 * How each surface arrives and leaves, as Material "fade through": the old
 * surface fades out quickly, the new one fades in just after it while moving,
 * so the two are never seen stacked half-transparent.
 *
 * - The keypad rises from below like a sheet and sinks back down.
 * - Settings slides in from the side, a step deeper, and slides back.
 *
 * On the way back the leaving surface stays on top, so it visibly moves away
 * instead of the destination popping over it.
 */
fun AnimatedContentTransitionScope<OkaneRoute>.okaneRouteTransition(): ContentTransform {
    val from = initialState
    val to = targetState
    val transform = when {
        to == OkaneRoute.Keypad ->
            (slideInVertically(OkaneMotion.enter(OkaneMotion.LONG)) { it / 6 } +
                fadeIn(fadeThroughIn())) togetherWith
                (fadeOut(fadeThroughOut()) +
                    scaleOut(OkaneMotion.exit(OkaneMotion.MEDIUM), targetScale = 0.96f))

        from == OkaneRoute.Keypad ->
            (fadeIn(fadeThroughIn()) +
                scaleIn(OkaneMotion.enter(OkaneMotion.LONG), initialScale = 0.96f)) togetherWith
                (slideOutVertically(OkaneMotion.exit(OkaneMotion.MEDIUM)) { it / 6 } +
                    fadeOut(fadeThroughOut()))

        to == OkaneRoute.Settings ->
            (slideInHorizontally(OkaneMotion.enter(OkaneMotion.LONG)) { it / 5 } +
                fadeIn(fadeThroughIn())) togetherWith
                (slideOutHorizontally(OkaneMotion.exit(OkaneMotion.MEDIUM)) { -it / 12 } +
                    fadeOut(fadeThroughOut()))

        from == OkaneRoute.Settings ->
            (slideInHorizontally(OkaneMotion.enter(OkaneMotion.LONG)) { -it / 12 } +
                fadeIn(fadeThroughIn())) togetherWith
                (slideOutHorizontally(OkaneMotion.exit(OkaneMotion.MEDIUM)) { it / 5 } +
                    fadeOut(fadeThroughOut()))

        else ->
            fadeIn(fadeThroughIn(OkaneMotion.LONG)) togetherWith fadeOut(fadeThroughOut())
    }
    if (from == OkaneRoute.Keypad || from == OkaneRoute.Settings) {
        transform.targetContentZIndex = -1f
    }
    return transform using SizeTransform(clip = false)
}

/** Incoming content waits for most of the outgoing fade before appearing. */
private fun fadeThroughIn(duration: Int = OkaneMotion.MEDIUM) =
    tween<Float>(durationMillis = duration, delayMillis = 90, easing = OkaneMotion.Emphasized)

private fun fadeThroughOut() =
    tween<Float>(durationMillis = 140, easing = OkaneMotion.Exit)
