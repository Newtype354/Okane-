package com.example.okane.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * One motion vocabulary for the whole app, so every transition feels like the
 * same product: soft deceleration, no bounce on layout, a gentle spring only
 * where something is pressed.
 *
 * Durations sit in the 200–450 ms band. Shorter reads as abrupt, longer as
 * sluggish; calm comes from the easing curve, not from slowness.
 */
object OkaneMotion {
    /** Material "emphasized decelerate": fast start, long soft landing. */
    val Emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Standard ease for things that both enter and leave. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Exits accelerate away so the incoming content owns the moment. */
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val SHORT = 180
    const val MEDIUM = 300
    const val LONG = 420

    fun <T> enter(duration: Int = MEDIUM, delay: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, delayMillis = delay, easing = Emphasized)

    fun <T> exit(duration: Int = SHORT): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, easing = Exit)

    fun <T> standard(duration: Int = MEDIUM): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, easing = Standard)

    /** Press feedback: quick and slightly springy, settles without wobble. */
    fun <T> press(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow * 2)

    /** Layout-level springs (indicators, offsets): critically damped. */
    val layoutOffset: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    val layoutSize: FiniteAnimationSpec<IntSize> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}
