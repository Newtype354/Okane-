package com.example.okane.ui.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.example.okane.ui.theme.Leaf
import com.example.okane.ui.theme.LeafDeep
import com.example.okane.ui.theme.PebbleLight
import com.example.okane.ui.theme.PebbleMid
import com.example.okane.ui.theme.PebbleShadow
import com.example.okane.ui.theme.White

/*
 * Calm illustrations drawn in code rather than shipped as bitmaps: they stay
 * crisp at every density, cost no APK size, recolour with the palette, and can
 * be animated piece by piece (the splash builds the cairn stone by stone).
 *
 * Every drawing takes a `progress` in 0..1. 1 is the finished picture; the
 * splash and the Home entrance drive it from 0.
 */

/** One pebble in the cairn, in fractions of the canvas. */
private data class Pebble(
    val centerX: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
)

// Bottom to top. Slight horizontal offsets keep the stack from looking stamped.
private val Cairn = listOf(
    Pebble(centerX = 0.47f, bottom = 0.93f, width = 0.62f, height = 0.25f),
    Pebble(centerX = 0.49f, bottom = 0.69f, width = 0.46f, height = 0.20f),
    Pebble(centerX = 0.47f, bottom = 0.50f, width = 0.33f, height = 0.16f),
    Pebble(centerX = 0.48f, bottom = 0.355f, width = 0.20f, height = 0.11f),
)

/**
 * Stacked stones (a cairn) with a leaf sprig rising beside them: the classic
 * image of balance and calm, used as the Home hero art and on the splash.
 *
 * @param progress 0..1; stones settle in one after another, then the leaves grow.
 * @param showLeaves the splash shows the bare cairn first.
 */
@Composable
fun StackedStones(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    showLeaves: Boolean = true,
) {
    val leafPath = remember { leafShape() }
    Canvas(modifier) {
        drawGroundShadow(progress)
        if (showLeaves) {
            val leafProgress = segment(progress, 0.55f, 1f)
            drawSprig(
                leafPath = leafPath,
                base = Offset(size.width * 0.80f, size.height * 0.94f),
                height = size.height * 0.80f,
                lean = 0.18f,
                progress = leafProgress,
            )
        }
        Cairn.forEachIndexed { index, pebble ->
            val start = index * 0.14f
            val local = segment(progress, start, start + 0.42f)
            if (local > 0f) drawPebble(pebble, local, isTop = index == Cairn.lastIndex)
        }
    }
}

/** A leaf sprig on its own, for the quote card and quiet empty states. */
@Composable
fun LeafSprig(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    tint: Color = Leaf,
    deepTint: Color = LeafDeep,
) {
    val leafPath = remember { leafShape() }
    Canvas(modifier) {
        drawSprig(
            leafPath = leafPath,
            base = Offset(size.width * 0.38f, size.height * 0.98f),
            height = size.height * 0.92f,
            lean = 0.30f,
            progress = progress,
            tint = tint,
            deepTint = deepTint,
        )
    }
}

/** Three small pebbles side by side, for empty states. */
@Composable
fun Pebbles(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawOval(
            brush = Brush.radialGradient(
                listOf(PebbleShadow.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.9f),
                radius = size.width * 0.45f,
            ),
            topLeft = Offset(size.width * 0.05f, size.height * 0.80f),
            size = Size(size.width * 0.9f, size.height * 0.18f),
        )
        drawPebble(Pebble(0.30f, 0.90f, 0.34f, 0.30f), 1f, isTop = false)
        drawPebble(Pebble(0.68f, 0.90f, 0.40f, 0.36f), 1f, isTop = false)
        drawPebble(Pebble(0.50f, 0.62f, 0.26f, 0.22f), 1f, isTop = true)
    }
}

// ---------------------------------------------------------------------------

private fun DrawScope.drawGroundShadow(progress: Float) {
    val alpha = segment(progress, 0f, 0.3f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(PebbleShadow.copy(alpha = 0.55f * alpha), Color.Transparent),
            center = Offset(size.width * 0.47f, size.height * 0.94f),
            radius = size.width * 0.42f,
        ),
        topLeft = Offset(size.width * 0.08f, size.height * 0.88f),
        size = Size(size.width * 0.78f, size.height * 0.11f),
    )
}

private fun DrawScope.drawPebble(pebble: Pebble, local: Float, isTop: Boolean) {
    val w = size.width * pebble.width
    val h = size.height * pebble.height
    // Settling: each stone drops a short distance and fades in, decelerating.
    val eased = 1f - (1f - local) * (1f - local) * (1f - local)
    val drop = (1f - eased) * size.height * 0.18f
    val left = size.width * pebble.centerX - w / 2f
    val top = size.height * pebble.bottom - h - drop
    val corner = CornerRadius(w * 0.5f, h * 0.52f)

    // Contact shadow on whatever sits underneath.
    drawOval(
        color = PebbleShadow.copy(alpha = 0.30f * eased),
        topLeft = Offset(left + w * 0.12f, top + h * 0.80f),
        size = Size(w * 0.76f, h * 0.30f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(PebbleLight, PebbleMid, PebbleShadow),
            startY = top,
            endY = top + h * 1.05f,
        ),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = corner,
        alpha = eased,
    )
    // Soft highlight, upper left: the light source of the whole scene.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(White.copy(alpha = if (isTop) 0.85f else 0.7f), Color.Transparent),
            center = Offset(left + w * 0.36f, top + h * 0.30f),
            radius = w * 0.34f,
        ),
        topLeft = Offset(left + w * 0.06f, top + h * 0.04f),
        size = Size(w * 0.62f, h * 0.52f),
        alpha = eased,
    )
}

/** Leaf outline in local units: base at origin, tip at (1, 0). */
private fun leafShape(): Path = Path().apply {
    moveTo(0f, 0f)
    cubicTo(0.25f, -0.42f, 0.72f, -0.40f, 1f, 0f)
    cubicTo(0.72f, 0.40f, 0.25f, 0.42f, 0f, 0f)
    close()
}

private fun DrawScope.drawSprig(
    leafPath: Path,
    base: Offset,
    height: Float,
    lean: Float,
    progress: Float,
    tint: Color = Leaf,
    deepTint: Color = LeafDeep,
) {
    if (progress <= 0f) return
    val grown = 1f - (1f - progress) * (1f - progress)
    val tip = Offset(base.x + height * lean * 0.55f, base.y - height * grown)
    val control = Offset(base.x - height * lean * 0.25f, base.y - height * grown * 0.55f)

    val stem = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(control.x, control.y, tip.x, tip.y)
    }
    drawPath(
        stem,
        color = deepTint,
        style = Stroke(width = height * 0.018f, cap = StrokeCap.Round),
    )

    // Leaves alternate sides along the stem and open as the stem grows past them.
    val leaves = 7
    for (i in 0 until leaves) {
        val t = 0.22f + i * (0.74f / (leaves - 1))
        val open = segment(grown, t * 0.7f, t * 0.7f + 0.3f)
        if (open <= 0f) continue
        val point = quadraticPoint(base, control, tip, t)
        val side = if (i % 2 == 0) 1f else -1f
        val isTip = i == leaves - 1
        val angle = if (isTip) -80f + lean * 40f else -90f + side * (52f - i * 3f)
        val length = height * (0.25f - i * 0.016f) * open
        translate(point.x, point.y) {
            rotate(degrees = angle, pivot = Offset.Zero) {
                drawLeaf(leafPath, length, tint, deepTint)
            }
        }
    }
}

private fun DrawScope.drawLeaf(leafPath: Path, length: Float, tint: Color, deepTint: Color) {
    if (length <= 0f) return
    // Scale the canvas instead of the path: no allocation per frame while the
    // sprig grows, and the gradient and midrib scale with the leaf for free.
    scale(scaleX = length, scaleY = length, pivot = Offset.Zero) {
        drawPath(
            leafPath,
            brush = Brush.linearGradient(
                colors = listOf(tint, deepTint),
                start = Offset.Zero,
                end = Offset(1f, 0.2f),
            ),
        )
        drawLine(
            color = White.copy(alpha = 0.45f),
            start = Offset(0.08f, 0f),
            end = Offset(0.82f, 0f),
            strokeWidth = 0.035f,
            cap = StrokeCap.Round,
        )
    }
}

private fun quadraticPoint(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x,
        u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y,
    )
}

/** Maps [value] in [start]..[end] to 0..1, clamped. */
private fun segment(value: Float, start: Float, end: Float): Float =
    ((value - start) / (end - start)).coerceIn(0f, 1f)
