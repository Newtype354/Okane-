package com.example.okane.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.okane.ui.theme.OkaneMotion
import kotlin.math.roundToLong

/**
 * An amount that glides to its new value instead of jumping.
 *
 * The last value shown is saved, so returning to a screen after recording a
 * transaction animates from the old total to the new one — the change is seen
 * rather than inferred. [startFromZero] makes the very first appearance count
 * up from Rp0 (Home's hero); elsewhere the first frame is already exact.
 *
 * Values are whole rupiah up to 12 digits, beyond Float precision, so the
 * animation runs on a 0..1 fraction and the amount is interpolated in Double.
 */
@Composable
fun AnimatedAmountText(
    value: Long,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    startFromZero: Boolean = false,
) {
    var lastShown by rememberSaveable { mutableLongStateOf(if (startFromZero) 0L else value) }
    var from by remember { mutableLongStateOf(lastShown) }
    val fraction = remember { Animatable(1f) }

    LaunchedEffect(value) {
        if (value == lastShown && fraction.value == 1f) return@LaunchedEffect
        from = interpolate(from, lastShown, fraction.value)
        lastShown = value
        fraction.snapTo(0f)
        fraction.animateTo(1f, tween(durationMillis = 800, easing = OkaneMotion.Emphasized))
    }

    Text(
        text = format(interpolate(from, lastShown, fraction.value)),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}

private fun interpolate(from: Long, to: Long, fraction: Float): Long =
    from + ((to - from) * fraction.toDouble()).roundToLong()

/**
 * A soft progress ring: pale track, rounded sage arc. The arc sweeps to its
 * value on first show and glides when the value changes.
 */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 7.dp,
) {
    var target by rememberSaveable { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(progress) { target = progress.coerceIn(0f, 1f) }
    val sweep by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900, easing = OkaneMotion.Emphasized),
        label = "ringSweep",
    )
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        if (sweep > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
