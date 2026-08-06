package com.example.okane.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.R
import com.example.okane.ui.theme.SkyBlue
import com.example.okane.ui.theme.White
import kotlinx.coroutines.delay

// How long the splash stays on screen after its animations finish.
private const val HOLD_AFTER_ANIMATION_MS = 600L

/**
 * Animated branding splash, shown once right after the system splash.
 * Sequence: logo pops in with a spring bounce -> name + tagline fade in ->
 * short hold -> [onFinished] tells MainActivity to crossfade to the app.
 */
@Composable
fun OkaneSplashScreen(onFinished: () -> Unit) {
    // Animatable = a value we can drive step by step inside a coroutine.
    // remember keeps the same instance across recompositions.
    val logoScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    // Runs once when the splash enters composition; each animateTo suspends
    // until that animation is done, giving us a simple sequential storyboard.
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        textAlpha.animateTo(1f, tween(durationMillis = 500))
        delay(HOLD_AFTER_ANIMATION_MS)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Same purple gradient as the dashboard hero card.
            .background(
                Brush.linearGradient(
                    listOf(SkyBlue, MaterialTheme.colorScheme.primaryContainer)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SplashLogo(
                discColor = MaterialTheme.colorScheme.onPrimaryContainer,
                coinColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(104.dp)
                    .scale(logoScale.value)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp,
                color = White,
                modifier = Modifier.alpha(textAlpha.value)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = White.copy(alpha = 0.8f),
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}

/**
 * The Okane mark drawn in code: a white "O" ring with a teal coin.
 * Mirrors res/drawable/ic_splash_logo.xml so the hand-off from the system
 * splash to this screen shows the same logo.
 */
@Composable
private fun SplashLogo(
    discColor: Color,
    coinColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val ringRadius = size.minDimension * 0.34f
        val ringStroke = size.minDimension * 0.12f
        drawCircle(
            color = White,
            radius = ringRadius,
            style = Stroke(width = ringStroke)
        )

        val coinCenter = center + Offset(ringRadius * 0.75f, ringRadius * 0.60f)
        val coinRadius = size.minDimension * 0.14f
        // Dark disc first: separates the coin from the ring it overlaps.
        drawCircle(
            color = discColor,
            radius = coinRadius + ringStroke * 0.35f,
            center = coinCenter
        )
        drawCircle(
            color = coinColor,
            radius = coinRadius,
            center = coinCenter
        )
    }
}
