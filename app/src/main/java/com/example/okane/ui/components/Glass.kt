package com.example.okane.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.okane.ui.theme.DeepNavy
import com.example.okane.ui.theme.White

enum class GlassVariant { Navigation, FloatingAction, Modal, Subtle }

/** Centralized geometry and rendering policy for Okane's restrained glass layer. */
object GlassTokens {
    val navigationRadius = 28.dp
    val floatingActionRadius = 28.dp
    val modalRadius = 28.dp
    val subtleRadius = 22.dp
    val borderWidth = 1.dp
    val navigationElevation = 10.dp
    val floatingActionElevation = 12.dp
    val modalElevation = 16.dp
    val subtleElevation = 2.dp
    val intendedBlurRadius = 20.dp

    // Compose's built-in blur affects the surface and its children, not only
    // the pixels behind it. Keep this false until a safe background-only
    // primitive is available; the more opaque fallback is used on every API.
    const val realBackgroundBlurEnabled = false
}

data class GlassStyle(
    val fill: Color,
    val highlight: Color,
    val border: Color,
    val content: Color,
    val shape: Shape,
    val elevation: Dp,
)

@Composable
fun glassStyle(variant: GlassVariant): GlassStyle {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = when (variant) {
        GlassVariant.Navigation -> RoundedCornerShape(GlassTokens.navigationRadius)
        GlassVariant.FloatingAction -> CircleShape
        GlassVariant.Modal -> RoundedCornerShape(
            topStart = GlassTokens.modalRadius,
            topEnd = GlassTokens.modalRadius,
            bottomStart = GlassTokens.modalRadius,
            bottomEnd = GlassTokens.modalRadius,
        )
        GlassVariant.Subtle -> RoundedCornerShape(GlassTokens.subtleRadius)
    }
    val elevation = when (variant) {
        GlassVariant.Navigation -> GlassTokens.navigationElevation
        GlassVariant.FloatingAction -> GlassTokens.floatingActionElevation
        GlassVariant.Modal -> GlassTokens.modalElevation
        GlassVariant.Subtle -> GlassTokens.subtleElevation
    }
    val fillAlpha = when (variant) {
        GlassVariant.Modal -> if (isDark) 0.92f else 0.88f
        GlassVariant.Subtle -> if (isDark) 0.82f else 0.76f
        else -> if (isDark) 0.88f else 0.82f
    }
    val base = if (isDark) DeepNavy else White
    return GlassStyle(
        fill = base.copy(alpha = fillAlpha),
        highlight = White.copy(alpha = if (isDark) 0.11f else 0.34f),
        border = White.copy(alpha = if (isDark) 0.20f else 0.42f),
        content = MaterialTheme.colorScheme.onSurface,
        shape = shape,
        elevation = elevation,
    )
}

/**
 * A non-blurring glass fallback with full-opacity content. The gradient is a
 * small top highlight, not a tint over child content.
 */
@Composable
fun GlassSurface(
    variant: GlassVariant,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val style = glassStyle(variant)
    Box(
        modifier = modifier
            .shadow(style.elevation, style.shape, clip = false)
            .clip(style.shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(style.highlight.compositeOver(style.fill), style.fill),
                    startY = 0f,
                    endY = 180f,
                )
            )
            .border(BorderStroke(GlassTokens.borderWidth, style.border), style.shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val style = glassStyle(GlassVariant.FloatingAction)
    Surface(
        onClick = onClick,
        modifier = modifier.shadow(style.elevation, CircleShape, clip = false),
        shape = CircleShape,
        color = style.highlight.compositeOver(style.fill),
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(GlassTokens.borderWidth, style.border),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}
