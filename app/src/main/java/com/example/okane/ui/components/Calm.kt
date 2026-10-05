package com.example.okane.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.okane.ui.theme.OkaneMotion

// The calm design system's building blocks. Screens compose these instead of
// styling Material components ad hoc, so spacing, corners and motion stay
// identical everywhere. Change a value here and the whole app follows.

object CalmTokens {
    val cardRadius = 24.dp
    val tileRadius = 20.dp
    val controlRadius = 16.dp
    val screenPadding = 20.dp
    val hairline = 1.dp
}

/**
 * Press feedback: the element sinks slightly under the finger and springs
 * back. Applied through a graphicsLayer lambda, so the animation only touches
 * the draw phase and never recomposes or re-measures the content.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = OkaneMotion.press(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Staggered entrance: fades the element in while it rises a few dp, delayed by
 * [index]. Plays once per saveable scope — returning to a screen restores it
 * instantly instead of replaying, which would quickly feel tiresome.
 */
fun Modifier.calmEntrance(index: Int, key: String = "entrance-$index"): Modifier = composed {
    var played by rememberSaveable(key) { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    val rise = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!played) {
            progress.animateTo(1f, OkaneMotion.enter(duration = 520, delay = 70 * index))
            played = true
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * rise
    }
}

/**
 * The one card: soft corners, a hairline instead of a shadow, optional tint.
 * Clickable cards get the press-scale feedback.
 */
@Composable
fun CalmCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = RoundedCornerShape(CalmTokens.cardRadius),
    border: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val stroke = if (border) {
        BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.pressScale(interaction, 0.985f),
            shape = shape,
            color = color,
            border = stroke,
            interactionSource = interaction,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Pastel shortcut tile: icon on top, a title and a quiet subtitle. */
@Composable
fun PastelTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.pressScale(interaction, 0.95f),
        shape = RoundedCornerShape(CalmTokens.tileRadius),
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interaction,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A section title with an optional quiet text action on the right. */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickableQuiet(onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/** Round icon button on a white disc with a hairline, like the reference's avatar. */
@Composable
fun CalmIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 44.dp,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size).pressScale(interaction, 0.92f),
        shape = CircleShape,
        color = container,
        contentColor = tint,
        border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/** Primary action: a solid sage pill. */
@Composable
fun CalmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    CalmButton(
        onClick = onClick,
        enabled = enabled,
        container = container,
        content = content,
        border = null,
        modifier = modifier,
    ) {
        ButtonLabel(text, icon)
    }
}

/** Secondary action: soft tonal pill, same footprint as the primary. */
@Composable
fun CalmSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    content: Color = MaterialTheme.colorScheme.onSurface,
    bordered: Boolean = true,
) {
    CalmButton(
        onClick = onClick,
        enabled = enabled,
        container = container,
        content = content,
        border = if (bordered) {
            BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
        modifier = modifier,
    ) {
        ButtonLabel(text, icon)
    }
}

/** Base pill button: animated enabled colours, press feedback, 52dp tall. */
@Composable
fun CalmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    border: BorderStroke? = null,
    label: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    // Enabled state fades rather than snapping, so a button that becomes
    // available while typing an amount does not flicker.
    val containerColor by animateColorAsState(
        if (enabled) container else container.copy(alpha = 0.32f),
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "buttonContainer",
    )
    val contentColor by animateColorAsState(
        if (enabled) content else content.copy(alpha = 0.7f),
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "buttonContent",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp).pressScale(interaction, 0.96f),
        shape = RoundedCornerShape(CalmTokens.controlRadius),
        color = containerColor,
        contentColor = contentColor,
        border = border,
        interactionSource = interaction,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { label() }
    }
}

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Pill chip. Selection cross-fades its colours instead of snapping. */
@Composable
fun CalmChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selectedContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedContent: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    enabled: Boolean = true,
) {
    val container by animateColorAsState(
        if (selected) selectedContainer else MaterialTheme.colorScheme.surface,
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        if (selected) selectedContent else MaterialTheme.colorScheme.onSurface,
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "chipContent",
    )
    val borderColor by animateColorAsState(
        if (selected) selectedContainer else MaterialTheme.colorScheme.outlineVariant,
        OkaneMotion.standard(OkaneMotion.SHORT),
        label = "chipBorder",
    )
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 40.dp).pressScale(interaction, 0.94f),
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = BorderStroke(CalmTokens.hairline, borderColor),
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Floating add button: a sage disc with a soft press. */
@Composable
fun CalmFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.size(60.dp).pressScale(interaction, 0.9f),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(26.dp))
        }
    }
}

/** Soft circular badge behind an icon (transaction rows, wallet avatars). */
@Composable
fun IconBadge(
    icon: ImageVector,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

/**
 * The bottom sheet every picker uses: white, generously rounded, a small
 * handle and a lighter scrim than Material's default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF1E2420).copy(alpha = 0.32f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        content = content,
    )
}

/** Inline text action with a bounded ripple. */
private fun Modifier.clickableQuiet(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
