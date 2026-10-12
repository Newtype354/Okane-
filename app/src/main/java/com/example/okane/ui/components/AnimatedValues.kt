package com.example.okane.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
