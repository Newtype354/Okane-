package com.example.okane.ui.wallets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material wallet glyph kept local so the app does not need the large extended-icons artifact. */
val WalletMaterialIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Wallet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(21f, 18f)
            verticalLineToRelative(1f)
            curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
            horizontalLineTo(5f)
            curveToRelative(-1.11f, 0f, -2f, -0.9f, -2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, 0.89f, -2f, 2f, -2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
            verticalLineToRelative(1f)
            horizontalLineTo(5f)
            verticalLineToRelative(13f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-1f)
            close()
            moveTo(20f, 7f)
            horizontalLineTo(6f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(6f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(9f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(20f, 15f)
            horizontalLineTo(6f)
            verticalLineTo(9f)
            horizontalLineToRelative(14f)
            close()
            moveTo(16f, 10.5f)
            curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
            reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
            reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
            reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
            close()
        }
    }.build()
}

val ArchiveMaterialIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Archive",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20.54f, 5.23f)
            lineToRelative(-1.39f, -1.68f)
            curveTo(18.88f, 3.21f, 18.47f, 3f, 18f, 3f)
            horizontalLineTo(6f)
            curveToRelative(-0.47f, 0f, -0.88f, 0.21f, -1.16f, 0.55f)
            lineTo(3.46f, 5.23f)
            curveTo(3.17f, 5.57f, 3f, 6.02f, 3f, 6.5f)
            verticalLineTo(19f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(6.5f)
            curveToRelative(0f, -0.48f, -0.17f, -0.93f, -0.46f, -1.27f)
            close()
            moveTo(6.24f, 5f)
            horizontalLineToRelative(11.52f)
            lineToRelative(0.81f, 0.97f)
            horizontalLineTo(5.44f)
            close()
            moveTo(5f, 19f)
            verticalLineTo(8f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(11f)
            close()
            moveTo(13.45f, 10f)
            horizontalLineToRelative(-2.9f)
            verticalLineToRelative(3f)
            horizontalLineTo(8f)
            lineToRelative(4f, 4f)
            lineToRelative(4f, -4f)
            horizontalLineToRelative(-2.55f)
            close()
        }
    }.build()
}

