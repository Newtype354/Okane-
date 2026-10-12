package com.example.okane.service

import android.app.Notification
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.okane.data.capture.CaptureOutcome
import com.example.okane.data.capture.CaptureProcessor
import com.example.okane.data.local.RawNotification
import com.example.okane.data.prefs.CaptureHealth
import com.example.okane.data.repository.RawCaptureRepository
import com.example.okane.domain.capture.CaptureInput
import com.example.okane.domain.capture.IgnoreReason
import com.example.okane.domain.capture.SourceCatalog
import com.example.okane.domain.capture.TransactionHeuristics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives notifications once the user enables "Notification access" for
 * Okane in system settings.
 *
 * Only notifications from the supported finance apps — or SMS, WhatsApp and
 * email from them — are looked at; everything else is dropped on arrival,
 * unread and unstored. Per notification:
 *
 *   1. [SourceCatalog] attributes it to a source, or it is dropped.
 *   2. [CaptureProcessor] records it (confirmed template on a confirmed
 *      package), turns it into a "Perlu konfirmasi" suggestion, or ignores it.
 *   3. The text is kept as a discovery sample for writing new templates,
 *      unless it is an OTP.
 */
@AndroidEntryPoint
class OkaneNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var processor: CaptureProcessor
    @Inject lateinit var rawCaptureRepository: RawCaptureRepository
    @Inject lateinit var health: CaptureHealth

    // Database writes must leave the main thread (notification callbacks
    // arrive there). SupervisorJob: one failed write must not kill the scope.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val debuggable: Boolean
        get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected")
        health.listenerConnected(System.currentTimeMillis())
        // Whatever arrived while the listener was down and is still in the
        // shade. Repeats are dropped by the processor, so this is safe on
        // every reconnect.
        val active = try {
            activeNotifications
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not read active notifications", e)
            null
        }
        active?.forEach(::handle)
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Notification listener disconnected — requesting rebind")
        requestRebind(ComponentName(this, OkaneNotificationListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = handle(sbn)

    private fun handle(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        // Never process our own notifications (feedback loop protection).
        if (pkg == packageName) return

        // Group summaries ("3 new messages") duplicate their children's
        // content — the children carry the real text, so skip summaries.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // Decided before anything is read further or stored: apps Okane does
        // not track are none of its business.
        val identified = SourceCatalog.identify(pkg, title) ?: return
        val now = System.currentTimeMillis()
        health.seen(identified.source.name, now)

        if (debuggable) {
            Log.d(TAG, "pkg=$pkg | source=${identified.source.name} | channel=${identified.channel}")
        }

        // bigText preferred: EXTRA_TEXT is often truncated for long messages.
        val input = CaptureInput(pkg, title, bigText ?: text, sbn.postTime)
        serviceScope.launch {
            val outcome = processor.process(input)
            if (debuggable) Log.d(TAG, "pkg=$pkg -> $outcome")
            if (outcome is CaptureOutcome.Recorded) health.recorded(now)
            if (keepAsSample(outcome, title, text, bigText)) {
                rawCaptureRepository.add(
                    RawNotification(
                        packageName = pkg,
                        title = title,
                        text = text,
                        bigText = bigText,
                        postTime = sbn.postTime,
                        capturedAt = now,
                    )
                )
            }
        }
    }

    /**
     * Discovery samples: what templates are written from. Everything a
     * supported source posts is useful — promos teach what to ignore — except
     * one-time codes, repeats, and sources the user switched off.
     */
    private fun keepAsSample(outcome: CaptureOutcome, vararg parts: String?): Boolean = when (outcome) {
        CaptureOutcome.Disabled, CaptureOutcome.Repeat -> false
        is CaptureOutcome.Ignored -> outcome.reason != IgnoreReason.OTP &&
            outcome.reason != IgnoreReason.NOT_A_SOURCE &&
            !TransactionHeuristics.isOtp(parts.filterNotNull().joinToString(" "))
        else -> true
    }

    companion object {
        const val TAG = "OkaneListener"
    }
}
