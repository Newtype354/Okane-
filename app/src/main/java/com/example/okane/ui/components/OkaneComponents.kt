package com.example.okane.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Older call sites (onboarding, settings) keep these names; they now render
// with the calm design system in Calm.kt.

/** A white card with a hairline border and soft corners. */
@Composable
fun OkaneCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    CalmCard(modifier = modifier, contentPadding = contentPadding, content = content)
}

/** Quiet label above a group of content, in sentence case. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * A small pill that communicates state at a glance (permission granted,
 * income / expense type, …), tinted by [color] over a soft wash of itself.
 */
@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Primary filled button: sage pill with press feedback. */
@Composable
fun OkaneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    CalmButton(onClick = onClick, modifier = modifier) { LabelRow(content) }
}

/** Secondary variant of [OkaneButton]: white pill with a hairline. */
@Composable
fun OkaneOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    CalmButton(
        onClick = onClick,
        modifier = modifier,
        container = MaterialTheme.colorScheme.surface,
        content = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(CalmTokens.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) { LabelRow(content) }
}

@Composable
private fun LabelRow(content: @Composable RowScope.() -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
