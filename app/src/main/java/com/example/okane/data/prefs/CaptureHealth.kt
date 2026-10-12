package com.example.okane.data.prefs

import android.content.Context
import com.example.okane.domain.Sources
import com.example.okane.domain.capture.CaptureHealthState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Timestamps the listener leaves behind, so Settings can show whether
 * automatic capture is actually alive. SharedPreferences because they must
 * survive the process being killed, which is exactly the failure they reveal;
 * the StateFlow mirror lets an open Settings screen update live, since the
 * listener runs in the same process.
 */
@Singleton
class CaptureHealth @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("okane_capture_health", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<CaptureHealthState> = _state.asStateFlow()

    fun listenerConnected(at: Long) = write { putLong(CONNECTED, at) }

    /** A notification from [source] arrived, whatever became of it. */
    fun seen(source: String, at: Long) = write {
        putLong(LAST_NOTIFICATION, at)
        putLong(SEEN_PREFIX + source, at)
    }

    fun recorded(at: Long) = write { putLong(LAST_RECORDED, at) }

    private fun write(edit: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(edit).apply()
        _state.value = read()
    }

    private fun read() = CaptureHealthState(
        listenerConnectedAt = prefs.long(CONNECTED),
        lastNotificationAt = prefs.long(LAST_NOTIFICATION),
        lastSeenBySource = Sources.ALL.mapNotNull { source ->
            prefs.long(SEEN_PREFIX + source)?.let { source to it }
        }.toMap(),
        lastRecordedAt = prefs.long(LAST_RECORDED),
    )

    private fun android.content.SharedPreferences.long(key: String): Long? =
        if (contains(key)) getLong(key, 0L) else null

    private companion object {
        const val CONNECTED = "listenerConnectedAt"
        const val LAST_NOTIFICATION = "lastNotificationAt"
        const val LAST_RECORDED = "lastRecordedAt"
        const val SEEN_PREFIX = "seen:"
    }
}
