package com.example.okane.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.R
import com.example.okane.ui.illustration.StackedStones
import com.example.okane.ui.theme.OkaneMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// How long the splash stays on screen after its animations finish.
private const val HOLD_AFTER_ANIMATION_MS = 450L

/**
 * Animated splash, shown once right after the system splash.
 *
 * Storyboard: the stones settle one by one into a cairn and the leaves grow
 * beside them, the name rises in, the tagline follows, a short breath, then
 * [onFinished] lets MainActivity fade into the app. Calm, but under two
 * seconds, because it plays on every cold start.
 */
@Composable
fun OkaneSplashScreen(onFinished: () -> Unit) {
    val stones = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val rise = with(LocalDensity.current) { 14.dp.toPx() }

    LaunchedEffect(Unit) {
        launch { stones.animateTo(1f, tween(1250, easing = OkaneMotion.Emphasized)) }
        delay(650)
        launch { title.animateTo(1f, tween(520, easing = OkaneMotion.Emphasized)) }
        delay(180)
        tagline.animateTo(1f, tween(480, easing = OkaneMotion.Emphasized))
        delay(HOLD_AFTER_ANIMATION_MS)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Progress is read in this small wrapper, so each animation frame
            // recomposes only the cairn, not the text around it.
            StackedStonesAnimated(stones)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * rise
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * rise * 0.6f
                }
            )
        }
    }
}

@Composable
private fun StackedStonesAnimated(progress: Animatable<Float, *>) {
    StackedStones(
        progress = progress.value,
        modifier = Modifier.size(width = 168.dp, height = 156.dp),
    )
}
