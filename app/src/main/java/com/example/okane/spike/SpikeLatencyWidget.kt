package com.example.okane.spike

/*
 * THROWAWAY — Phase 0 latency spike. Delete this whole package once the
 * decision gate is settled.
 *
 * Why this exists: a home screen widget is not drawn by our app. It is
 * RemoteViews rendered inside the launcher's process. A keypad tap therefore
 * costs a full inter-process round trip:
 *
 *   launcher (tap) -> IPC -> our process wakes -> ActionCallback runs
 *   -> DataStore write -> recompose -> translate to RemoteViews
 *   -> IPC back -> launcher inflates and draws
 *
 * A calculator keypad only works if that loop is fast. This spike measures it
 * with one button so we can decide before building the real thing.
 *
 * WHAT COUNTS AS THE RESULT: tap-to-pixel, measured by filming the device at
 * 240 fps and counting frames from finger contact to the display changing.
 * The Log timestamps below are a SECONDARY signal only. They bracket just the
 * in-our-process segment (callback start -> update() returns) and exclude the
 * launcher-side wakeup before it and the RemoteViews serialisation, IPC,
 * inflation, and draw after it. Do not decide the gate on them; use them to
 * work out where the time went once the video number is known.
 *
 * DISPLAY IS BUILT FOR FRAME COUNTING, NOT FOR LOOKS: every tap inverts the
 * whole widget between near-black and near-white. A full-field luminance flip
 * is unambiguous on a single video frame, whereas the shape of a changing
 * digit can be arguable for a frame or two. The large tap counter changes on
 * the same frame and lets each filmed tap be matched to its logcat line.
 */

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.action.ActionParameters
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button

/** Logcat tag to filter on: `adb logcat -s OkaneSpike`. */
const val SPIKE_TAG = "OkaneSpike"

/** Counter lives in DataStore, the same store the real widget will use. */
private val COUNT_KEY = intPreferencesKey("spike_count")

// Deliberately at the extremes of the luminance range so the flip survives
// video compression and auto-exposure on the filming phone.
private val DARK = Color(0xFF000000)
private val LIGHT = Color(0xFFFFFFFF)

object SpikeLatencyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SpikeContent() }
    }

    @Composable
    private fun SpikeContent() {
        val count = currentState(COUNT_KEY) ?: 0

        // Odd taps render inverted. Both the background and the text swap, so
        // the frame where the update lands is obvious at any exposure.
        val inverted = count % 2 == 1
        val background = if (inverted) LIGHT else DARK
        val foreground = if (inverted) DARK else LIGHT

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(background)
                .padding(8.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            Text(
                text = "$count",
                style = TextStyle(
                    color = ColorProvider(foreground),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Button(
                text = "TAP ME",
                onClick = actionRunCallback<SpikeIncrementAction>(),
            )
        }
    }
}

/**
 * Runs in our process every time the button is tapped. The two timestamps
 * are the secondary signal described in the file header — the in-process
 * segment only, never the number the decision gate is judged on.
 */
class SpikeIncrementAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val t0 = SystemClock.elapsedRealtime()

        var newCount = 0
        updateAppWidgetState(context, glanceId) { prefs ->
            newCount = (prefs[COUNT_KEY] ?: 0) + 1
            prefs[COUNT_KEY] = newCount
        }
        SpikeLatencyWidget.update(context, glanceId)

        val t1 = SystemClock.elapsedRealtime()

        // Single-line, fixed-format so the numbers can be scraped from logcat.
        // tap=N matches the digit filmed on screen, so a video frame count and
        // its in-process delta can be lined up per trial.
        Log.i(SPIKE_TAG, "SPIKE tap=$newCount t0=$t0 t1=$t1 delta=${t1 - t0}")
    }
}

class SpikeLatencyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget get() = SpikeLatencyWidget
}
