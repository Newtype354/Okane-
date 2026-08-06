package com.example.okane.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.okane.data.local.RawNotification
import com.example.okane.data.prefs.SourcePreferences
import com.example.okane.data.repository.RawCaptureRepository
import com.example.okane.data.repository.TransactionRepository
import com.example.okane.domain.parser.AmountParser
import com.example.okane.domain.parser.NotificationParser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives every notification posted on the device, once the user enables
 * "Notification access" for Okane in system settings.
 *
 * Pipeline per notification:
 *   1. Discovery capture (temporary): financial-looking notifications are
 *      stored as RawNotification so parsers can be written from real text.
 *   2. Parsing: NotificationParser turns known formats into a Transaction
 *      and stores it. With no per-app parsers registered yet, this step
 *      simply does nothing — no fake data can be created.
 */
@AndroidEntryPoint
class OkaneNotificationListenerService : NotificationListenerService() {

    // Filled in by Hilt right after the system creates the service —
    // this is why the service needed no manual constructor wiring.
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var rawCaptureRepository: RawCaptureRepository
    @Inject lateinit var notificationParser: NotificationParser
    @Inject lateinit var sourcePreferences: SourcePreferences

    // Database writes must leave the main thread (notification callbacks
    // arrive there). SupervisorJob: one failed write must not kill the scope.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener CONNECTED")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Notification listener DISCONNECTED — requesting rebind")
        requestRebind(ComponentName(this, OkaneNotificationListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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

        if (LOG_ALL_PACKAGES) {
            Log.d(
                TAG,
                "pkg=$pkg | postTime=${sbn.postTime}\n" +
                    "  title: $title\n  text: $text\n  bigText: $bigText"
            )
        }

        // 1) Discovery capture (temporary, until the 6 parsers exist).
        if (LOG_ALL_PACKAGES && looksFinancial(title, text, bigText)) {
            serviceScope.launch {
                rawCaptureRepository.add(
                    RawNotification(
                        packageName = pkg,
                        title = title,
                        text = text,
                        bigText = bigText,
                        postTime = sbn.postTime,
                        capturedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // 2) Normal pipeline: parse -> store. bigText preferred because
        // EXTRA_TEXT is often truncated for long transaction messages.
        val transaction = notificationParser.parse(
            packageName = pkg,
            title = title,
            text = bigText ?: text,
            postTimeMillis = sbn.postTime
        ) ?: return

        serviceScope.launch {
            // Settings screen toggle: user can mute a source without
            // revoking notification access entirely.
            if (!sourcePreferences.isEnabled(transaction.source)) {
                Log.d(TAG, "Source ${transaction.source} disabled in settings — skipped")
                return@launch
            }
            val rowId = transactionRepository.add(transaction)
            // -1 means the unique index rejected it as a duplicate delivery.
            Log.i(TAG, "Stored transaction rowId=$rowId: ${transaction.source} " +
                "${transaction.type} ${transaction.amount}")
        }
    }

    /**
     * Discovery-phase heuristic: worth capturing if it contains a rupiah
     * amount or common Indonesian transaction words. Broad on purpose —
     * false positives just sit in a local table the user reviews.
     */
    private fun looksFinancial(vararg parts: String?): Boolean {
        val joined = parts.filterNotNull().joinToString(" ")
        if (joined.isBlank()) return false
        if (AmountParser.findRupiah(joined) != null) return true
        val lower = joined.lowercase()
        return FINANCIAL_KEYWORDS.any { it in lower }
    }

    companion object {
        const val TAG = "OkaneListener"

        // Discovery mode: log + capture from ALL apps while we learn the
        // real package names and formats. Set to false in Phase 4's close,
        // when TRACKED_PACKAGES filtering takes over.
        const val LOG_ALL_PACKAGES = true

        // Confirmed package names go here after discovery (never guessed).
        val TRACKED_PACKAGES: Set<String> = setOf(
            "com.gojek.gopay" // GoPay standalone app — confirmed 2026-07-07
        )

        private val FINANCIAL_KEYWORDS = listOf(
            "transaksi", "pembayaran", "pembelian", "transfer", "top up",
            "topup", "saldo", "berhasil", "diterima", "payment", "debit", "kredit"
        )
    }
}
