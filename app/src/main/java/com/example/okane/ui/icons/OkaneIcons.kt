package com.example.okane.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Thin, rounded line icons in the calm style of the reference design.
 *
 * Kept local rather than pulling in material-icons-extended (a very large
 * artifact): each icon is a few SVG path strings on a 24-unit grid, stroked at
 * 1.75 units with round caps and joins. `Icon` tints them like any vector.
 */
object OkaneIcons {
    val Home: ImageVector by lazy {
        lineIcon(
            "Home",
            "M4.5 10.2 L12 4.2 L19.5 10.2 V18.6 A1.9 1.9 0 0 1 17.6 20.5 H6.4 A1.9 1.9 0 0 1 4.5 18.6 Z",
            "M9.75 20.5 V15.4 A2.25 2.25 0 0 1 14.25 15.4 V20.5",
        )
    }

    /** Riwayat: a clock face, the passage of time. */
    val History: ImageVector by lazy {
        lineIcon(
            "History",
            "M12 3.75 A8.25 8.25 0 1 1 3.75 12 A8.25 8.25 0 0 1 12 3.75 Z",
            "M12 7.75 V12 L14.9 13.8",
        )
    }

    val Wallet: ImageVector by lazy {
        lineIcon(
            "Wallet",
            "M4.25 7.75 A2.5 2.5 0 0 1 6.75 5.25 H16.5 A1.5 1.5 0 0 1 18 6.75 V8.25",
            "M4.25 7.75 V17.25 A2.5 2.5 0 0 0 6.75 19.75 H18.25 A1.5 1.5 0 0 0 19.75 18.25 V9.75 A1.5 1.5 0 0 0 18.25 8.25 H6.75 A2.5 2.5 0 0 1 4.25 5.75",
            "M15.6 14 H16.4",
        )
    }

    /** Pengaturan: three sliders, quieter than a cog. */
    val Settings: ImageVector by lazy {
        lineIcon(
            "Settings",
            "M4.5 7 H13", "M17.5 7 H19.5", "M15.25 4.75 V9.25",
            "M4.5 12 H7", "M11.5 12 H19.5", "M9.25 9.75 V14.25",
            "M4.5 17 H13", "M17.5 17 H19.5", "M15.25 14.75 V19.25",
        )
    }

    val Plus: ImageVector by lazy { lineIcon("Plus", "M12 5.5 V18.5", "M5.5 12 H18.5") }

    val Pencil: ImageVector by lazy {
        lineIcon(
            "Pencil",
            "M5 19 L5.6 15.6 L15.9 5.3 A1.9 1.9 0 0 1 18.7 8.1 L8.4 18.4 Z",
            "M14.2 7 L17 9.8",
        )
    }

    /** Keluar: money leaves, up and away. */
    val ArrowUpRight: ImageVector by lazy { lineIcon("Out", "M7 17 L17 7", "M9.5 7 H17 V14.5") }

    /** Masuk: money arrives, down and in. */
    val ArrowDownLeft: ImageVector by lazy { lineIcon("In", "M17 7 L7 17", "M7 9.5 V17 H14.5") }

    val Transfer: ImageVector by lazy {
        lineIcon(
            "Transfer",
            "M4.5 8.5 H18.5", "M15.5 5.5 L18.5 8.5 L15.5 11.5",
            "M19.5 15.5 H5.5", "M8.5 12.5 L5.5 15.5 L8.5 18.5",
        )
    }

    val Leaf: ImageVector by lazy {
        lineIcon(
            "Leaf",
            "M5.5 18.5 C5.5 10.5 10.5 5.5 18.5 5.5 C18.5 13.5 13.5 18.5 5.5 18.5 Z",
            "M5.5 18.5 L13 11",
        )
    }

    val Calendar: ImageVector by lazy {
        lineIcon(
            "Calendar",
            "M6.5 5.75 H17.5 A2 2 0 0 1 19.5 7.75 V17.75 A2 2 0 0 1 17.5 19.75 H6.5 A2 2 0 0 1 4.5 17.75 V7.75 A2 2 0 0 1 6.5 5.75 Z",
            "M4.5 10 H19.5", "M8.5 3.75 V7.25", "M15.5 3.75 V7.25",
        )
    }

    val Close: ImageVector by lazy { lineIcon("Close", "M6.5 6.5 L17.5 17.5", "M17.5 6.5 L6.5 17.5") }

    val ChevronLeft: ImageVector by lazy { lineIcon("Back", "M14.5 5.5 L8 12 L14.5 18.5") }

    val ChevronRight: ImageVector by lazy { lineIcon("Forward", "M9.5 5.5 L16 12 L9.5 18.5") }

    val ChevronDown: ImageVector by lazy { lineIcon("Expand", "M6.5 9.5 L12 15 L17.5 9.5") }

    val Check: ImageVector by lazy { lineIcon("Check", "M5.5 12.5 L10 17 L18.5 7.5") }

    val Archive: ImageVector by lazy {
        lineIcon(
            "Archive",
            "M5 5.5 H19 A1 1 0 0 1 20 6.5 V8.5 A1 1 0 0 1 19 9.5 H5 A1 1 0 0 1 4 8.5 V6.5 A1 1 0 0 1 5 5.5 Z",
            "M5.5 9.5 V17.5 A1.75 1.75 0 0 0 7.25 19.25 H16.75 A1.75 1.75 0 0 0 18.5 17.5 V9.5",
            "M10 13 H14",
        )
    }

    val Restore: ImageVector by lazy {
        lineIcon(
            "Restore",
            "M4.75 12 A7.25 7.25 0 1 0 7 6.75",
            "M4.75 4.5 V8.25 H8.5",
        )
    }

    val Backspace: ImageVector by lazy {
        lineIcon(
            "Backspace",
            "M9.2 5.75 H18.5 A1.75 1.75 0 0 1 20.25 7.5 V16.5 A1.75 1.75 0 0 1 18.5 18.25 H9.2 L3.75 12 Z",
            "M11.75 9.5 L16.25 14.5", "M16.25 9.5 L11.75 14.5",
        )
    }

    val Bell: ImageVector by lazy {
        lineIcon(
            "Bell",
            "M6.25 16.25 V11 A5.75 5.75 0 0 1 17.75 11 V16.25 L19 17.75 H5 Z",
            "M10.25 20.25 H13.75",
        )
    }

    val Star: ImageVector by lazy {
        lineIcon(
            "Star",
            "M12 4.5 L14.2 9.1 L19.2 9.7 L15.5 13.1 L16.5 18.1 L12 15.6 L7.5 18.1 L8.5 13.1 L4.8 9.7 L9.8 9.1 Z",
        )
    }

    val Info: ImageVector by lazy {
        lineIcon(
            "Info",
            "M12 3.75 A8.25 8.25 0 1 1 3.75 12 A8.25 8.25 0 0 1 12 3.75 Z",
            "M12 11 V16", "M12 8 V8.1",
        )
    }

    private fun lineIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
