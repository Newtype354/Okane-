package com.example.okane.ui.illustration

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import java.time.LocalTime
import kotlin.math.min
import kotlin.random.Random

/** The sky follows the greeting: morning mist, a clear day, a quiet night. */
enum class SkyTime {
    MORNING, DAY, NIGHT;

    companion object {
        /** Same hours as `Greeting`: Ohayou 4–10, Konnichiwa 11–17, Konbanwa otherwise. */
        fun forHour(hour: Int): SkyTime = when (hour) {
            in 4..10 -> MORNING
            in 11..17 -> DAY
            else -> NIGHT
        }

        fun now(): SkyTime = forHour(LocalTime.now().hour)
    }
}

/**
 * Home's whole background: a wide sky over rolling green hills with one tree
 * on the crest, in the spirit of calm wallpaper photography.
 *
 * Drawn in code instead of shipped as a photo, so it costs nothing in APK
 * size, is sharp on every screen and fits any aspect ratio. Every path and
 * brush is built once per size in [drawWithCache]; the picture is static, so
 * after the first frame it is never redrawn, only moved with the pager.
 */
@Composable
fun CalmLandscape(time: SkyTime, modifier: Modifier = Modifier) {
    val palette = remember(time) { paletteFor(time) }
    Spacer(
        modifier.drawWithCache {
            val scene = Scene(size, palette)
            onDrawBehind { scene.draw(this) }
        }
    )
}

private class ScenePalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyHorizon: Color,
    val glow: Color,
    val glowCenter: Offset,
    val far: Color,
    val mist: Color,
    val midTop: Color,
    val midBottom: Color,
    val nearLit: Color,
    val nearShade: Color,
    val ground: Color,
    val groundDeep: Color,
    val canopyLight: Color,
    val canopyDark: Color,
    val trunk: Color,
    val cloud: Color,
    val cloudShade: Color,
    val night: Boolean,
)

private fun paletteFor(time: SkyTime): ScenePalette = when (time) {
    SkyTime.MORNING -> ScenePalette(
        skyTop = Color(0xFF3B7DC1),
        skyMid = Color(0xFF7DB1DF),
        skyHorizon = Color(0xFFF1E3CF),
        glow = Color(0xFFFFE2B8),
        glowCenter = Offset(0.16f, 0.70f),
        far = Color(0xFF9DB9C2),
        mist = Color(0xFFF7F4EE).copy(alpha = 0.78f),
        midTop = Color(0xFF8CBB6C),
        midBottom = Color(0xFF6EA155),
        nearLit = Color(0xFF9CCB6A),
        nearShade = Color(0xFF5D9346),
        ground = Color(0xFF4A8139),
        groundDeep = Color(0xFF305E28),
        canopyLight = Color(0xFF6CA552),
        canopyDark = Color(0xFF2C5E2F),
        trunk = Color(0xFF4D3C2C),
        cloud = Color(0xFFFFFFFF),
        cloudShade = Color(0xFFF3E1D3),
        night = false,
    )
    SkyTime.DAY -> ScenePalette(
        skyTop = Color(0xFF2A70BF),
        skyMid = Color(0xFF4C96DC),
        skyHorizon = Color(0xFFC3E2F4),
        glow = Color(0xFFFFFFFF),
        glowCenter = Offset(0.82f, 0.66f),
        far = Color(0xFF8DB6C9),
        mist = Color(0xFFEFF6FA).copy(alpha = 0.45f),
        midTop = Color(0xFF7DB75B),
        midBottom = Color(0xFF5E9B46),
        nearLit = Color(0xFF94CB5C),
        nearShade = Color(0xFF4F8C3B),
        ground = Color(0xFF3E7832),
        groundDeep = Color(0xFF2A5A25),
        canopyLight = Color(0xFF5E9E46),
        canopyDark = Color(0xFF214F27),
        trunk = Color(0xFF47382A),
        cloud = Color(0xFFFFFFFF),
        cloudShade = Color(0xFFD5E5F2),
        night = false,
    )
    SkyTime.NIGHT -> ScenePalette(
        skyTop = Color(0xFF0E2242),
        skyMid = Color(0xFF22416C),
        skyHorizon = Color(0xFF59749E),
        glow = Color(0xFFB9C8E6),
        glowCenter = Offset(0.80f, 0.36f),
        far = Color(0xFF314D69),
        mist = Color(0xFF8EA3C4).copy(alpha = 0.22f),
        midTop = Color(0xFF2E5640),
        midBottom = Color(0xFF244632),
        nearLit = Color(0xFF3C6C44),
        nearShade = Color(0xFF26482E),
        ground = Color(0xFF1D3B25),
        groundDeep = Color(0xFF142B1B),
        canopyLight = Color(0xFF2F5A36),
        canopyDark = Color(0xFF15301C),
        trunk = Color(0xFF241C16),
        // Opaque on purpose: translucent puffs would stack into blotches.
        cloud = Color(0xFF6E83A8),
        cloudShade = Color(0xFF465C85),
        night = true,
    )
}

/** A puff of cloud: centre and radius in fractions of the width. */
private data class Puff(val x: Float, val y: Float, val r: Float, val shaded: Boolean = false)

// One soft cumulus, built from overlapping radial puffs so its edge fades
// like a photographed cloud instead of being cut out.
private val CloudPuffs = listOf(
    Puff(0.00f, 0.02f, 0.11f),
    Puff(-0.10f, 0.05f, 0.085f),
    Puff(0.11f, 0.05f, 0.09f),
    Puff(0.05f, -0.05f, 0.09f),
    Puff(-0.05f, -0.03f, 0.075f),
    Puff(0.17f, 0.00f, 0.065f),
    Puff(-0.17f, 0.07f, 0.06f),
    Puff(0.00f, 0.075f, 0.10f, shaded = true),
    Puff(-0.12f, 0.085f, 0.075f, shaded = true),
    Puff(0.13f, 0.075f, 0.08f, shaded = true),
)

private class Scene(size: Size, private val p: ScenePalette) {
    private val w = size.width
    private val h = size.height
    private val unit = min(w, h)

    private val sky = Brush.verticalGradient(
        0f to p.skyTop,
        0.42f to p.skyMid,
        0.70f to p.skyHorizon,
        startY = 0f,
        endY = h,
    )
    private val glow = Brush.radialGradient(
        listOf(p.glow.copy(alpha = if (p.night) 0.35f else 0.55f), p.glow.copy(alpha = 0f)),
        center = Offset(p.glowCenter.x * w, p.glowCenter.y * h),
        radius = w * if (p.night) 0.35f else 0.75f,
    )

    // Back to front. Points are (x, y) fractions; each pair is joined by a
    // smooth curve with flat tangents, which is what makes hills roll.
    private val far = hill(0f to 0.694f, 0.24f to 0.672f, 0.50f to 0.688f, 0.76f to 0.664f, 1f to 0.680f)
    private val mid = hill(0f to 0.738f, 0.32f to 0.712f, 0.64f to 0.736f, 1f to 0.714f)
    private val crest = Offset(0.62f * w, 0.716f * h)
    private val near = hill(0f to 0.806f, crest.x / w to crest.y / h, 1f to 0.770f)
    private val fold = hill(0f to 0.858f, 0.34f to 0.832f, 0.72f to 0.852f, 1f to 0.826f)
    private val front = hill(0f to 0.905f, 0.48f to 0.878f, 1f to 0.900f)

    private val mistBand = Brush.verticalGradient(
        0f to p.mist.copy(alpha = 0f),
        0.5f to p.mist,
        1f to p.mist.copy(alpha = 0f),
        startY = 0.645f * h,
        endY = 0.765f * h,
    )
    private val farBrush = Brush.verticalGradient(
        listOf(p.far, lerp(p.far, p.skyHorizon, 0.5f)),
        startY = 0.66f * h,
        endY = 0.72f * h,
    )
    private val midBrush = Brush.verticalGradient(listOf(p.midTop, p.midBottom), startY = 0.70f * h, endY = 0.80f * h)
    private val nearBrush = Brush.verticalGradient(
        listOf(p.nearLit, p.nearShade),
        startY = crest.y,
        endY = 0.92f * h,
    )
    private val sunlit = Brush.radialGradient(
        listOf(p.nearLit.copy(alpha = 0.55f), p.nearLit.copy(alpha = 0f)),
        center = Offset(0.44f * w, 0.74f * h),
        radius = 0.55f * w,
    )
    // A gentle second rise in the meadow, so the slope reads as rolling ground.
    private val foldBrush = Brush.verticalGradient(
        listOf(p.nearShade.copy(alpha = 0.0f), p.nearShade.copy(alpha = 0.45f), p.ground.copy(alpha = 0.6f)),
        startY = 0.82f * h,
        endY = 0.90f * h,
    )
    private val frontBrush = Brush.verticalGradient(listOf(p.ground, p.groundDeep), startY = 0.88f * h, endY = h)

    // The tree on the crest.
    private val canopyR = 0.058f * w
    private val trunkTop = Offset(crest.x, crest.y - canopyR * 1.25f)
    private val canopyCenter = Offset(crest.x, crest.y - canopyR * 1.95f)

    // A broad, slightly flattened crown like a lone field tree: a wide base
    // of overlapping lobes, smaller ones breaking the outline on top.
    private val canopy: Path = listOf(
        Triple(0f, 0.05f, 0.95f),
        Triple(-0.95f, 0.30f, 0.66f),
        Triple(0.98f, 0.28f, 0.68f),
        Triple(-1.62f, 0.52f, 0.40f),
        Triple(1.66f, 0.50f, 0.42f),
        Triple(-0.50f, -0.45f, 0.62f),
        Triple(0.52f, -0.42f, 0.64f),
        Triple(-0.05f, -0.78f, 0.48f),
        Triple(-1.18f, -0.02f, 0.44f),
        Triple(1.22f, -0.04f, 0.42f),
        Triple(-0.35f, 0.62f, 0.55f),
        Triple(0.42f, 0.60f, 0.55f),
    ).map { (dx, dy, r) ->
        Path().apply {
            addOval(
                Rect(
                    center = Offset(canopyCenter.x + dx * canopyR, canopyCenter.y + dy * canopyR),
                    radius = r * canopyR,
                )
            )
        }
    }.reduce { acc, path -> Path.combine(PathOperation.Union, acc, path) }
    private val canopyBrush = Brush.radialGradient(
        listOf(p.canopyLight, p.canopyDark),
        center = Offset(canopyCenter.x - canopyR * 0.7f, canopyCenter.y - canopyR * 0.9f),
        radius = canopyR * 3.0f,
    )
    // Light catching the upper lobes.
    private val canopyGlints = listOf(
        Triple(-0.55f, -0.55f, 0.42f),
        Triple(0.30f, -0.70f, 0.32f),
        Triple(-1.05f, -0.05f, 0.30f),
    ).map { (dx, dy, r) -> Offset(canopyCenter.x + dx * canopyR, canopyCenter.y + dy * canopyR) to r * canopyR }

    // Night only: a scatter of stars and a crescent moon.
    private val stars: List<Triple<Offset, Float, Float>> = if (!p.night) emptyList() else {
        val random = Random(7)
        List(70) {
            Triple(
                Offset(random.nextFloat() * w, random.nextFloat() * 0.56f * h),
                (0.0012f + random.nextFloat() * 0.0028f) * unit,
                0.35f + random.nextFloat() * 0.55f,
            )
        }.filterNot { (at, _, _) ->
            // Keep the band behind the greeting and total clear.
            at.y in 0.06f * h..0.27f * h && at.x in 0.08f * w..0.92f * w
        }
    }
    private val moon: Path? = if (!p.night) null else {
        val c = Offset(0.80f * w, 0.36f * h)
        val r = 0.048f * w
        Path.combine(
            PathOperation.Difference,
            Path().apply { addOval(Rect(c, r)) },
            Path().apply { addOval(Rect(Offset(c.x + r * 0.45f, c.y - r * 0.25f), r * 0.92f)) },
        )
    }

    fun draw(scope: DrawScope) = with(scope) {
        drawRect(sky)
        drawRect(glow)
        stars.forEach { (at, r, alpha) -> drawCircle(Color.White.copy(alpha = alpha), r, at) }
        moon?.let { drawPath(it, Color(0xFFF4F0E2)) }
        drawCloud(Offset(0.30f * w, 0.47f * h))

        drawPath(far, farBrush)
        drawRect(mistBand)
        drawPath(mid, midBrush)
        drawPath(near, nearBrush)
        drawPath(near, sunlit)
        drawTree()
        drawPath(fold, foldBrush)
        drawPath(front, frontBrush)
    }

    private fun DrawScope.drawCloud(center: Offset) {
        CloudPuffs.forEach { puff ->
            val c = Offset(center.x + puff.x * w, center.y + puff.y * w)
            val r = puff.r * w
            // Shaded puffs only tint the underside; they have no solid core,
            // so the cloud reads as one body rather than separate blobs.
            val brush = if (puff.shaded) {
                Brush.radialGradient(
                    0f to p.cloudShade.copy(alpha = p.cloudShade.alpha * 0.55f),
                    1f to p.cloudShade.copy(alpha = 0f),
                    center = c,
                    radius = r,
                )
            } else {
                Brush.radialGradient(
                    0f to p.cloud,
                    0.62f to p.cloud,
                    1f to p.cloud.copy(alpha = 0f),
                    center = c,
                    radius = r,
                )
            }
            drawCircle(brush, radius = r, center = c)
        }
    }

    private fun DrawScope.drawTree() {
        // Soft shadow on the grass, then trunk, then the crown over it.
        drawOval(
            color = Color.Black.copy(alpha = 0.14f),
            topLeft = Offset(crest.x - canopyR * 2.0f, crest.y - canopyR * 0.12f),
            size = Size(canopyR * 4f, canopyR * 0.40f),
        )
        drawLine(
            color = p.trunk,
            start = Offset(crest.x, crest.y + canopyR * 0.06f),
            end = trunkTop,
            strokeWidth = canopyR * 0.22f,
            cap = StrokeCap.Round,
        )
        // Two limbs reaching into the crown.
        drawLine(p.trunk, trunkTop, Offset(trunkTop.x - canopyR * 0.55f, trunkTop.y - canopyR * 0.45f), canopyR * 0.12f, StrokeCap.Round)
        drawLine(p.trunk, trunkTop, Offset(trunkTop.x + canopyR * 0.5f, trunkTop.y - canopyR * 0.5f), canopyR * 0.11f, StrokeCap.Round)
        drawPath(canopy, canopyBrush)
        canopyGlints.forEach { (c, r) ->
            drawCircle(
                Brush.radialGradient(
                    listOf(p.canopyLight.copy(alpha = 0.55f), p.canopyLight.copy(alpha = 0f)),
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )
        }
    }

    private fun hill(vararg points: Pair<Float, Float>): Path = Path().apply {
        val xy = points.map { (x, y) -> Offset(x * w, y * h) }
        moveTo(xy.first().x, xy.first().y)
        xy.zipWithNext().forEach { (a, b) ->
            val midX = (a.x + b.x) / 2f
            cubicTo(midX, a.y, midX, b.y, b.x, b.y)
        }
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
