package com.example.okane.domain.parser

import com.example.okane.data.local.Transaction

/**
 * Entry point of the parsing layer: turns one raw notification into a
 * ready-to-store Transaction, or null when it isn't a known transaction.
 *
 * Routing: package name -> the one SourceParser responsible for that app.
 * The parsers list is injectable so unit tests can pass fakes.
 */
class NotificationParser(
    private val parsers: List<SourceParser> = defaultParsers()
) {

    /**
     * @param packageName  app that posted the notification
     * @param title        notification title (may be null)
     * @param text         richest text available — Phase 4 passes
     *                     (bigText ?: text) from the listener
     * @param postTimeMillis when the notification was posted; used as the
     *                     transaction timestamp (arrives seconds after the
     *                     real transaction — close enough for tracking)
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        postTimeMillis: Long
    ): Transaction? {
        val parser = parsers.firstOrNull { packageName in it.packageNames }
            ?: return null // app we don't track
        val parsed = parser.parse(title, text)
            ?: return null // tracked app, but not a transaction notification

        return Transaction(
            source = parser.source,
            type = parsed.type,
            amount = parsed.amount,
            merchant = parsed.merchant,
            // Raw text preserved so bad parses can be debugged later.
            note = "title=$title | text=$text",
            timestamp = postTimeMillis,
            createdAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * The real per-app parsers are registered here as they get written —
         * each one only after its notification format is confirmed with real
         * samples from the device (never guessed).
         */
        fun defaultParsers(): List<SourceParser> = listOf(
            GoPayParser()
            // ShopeePayParser(), LivinParser(), JeniusParser(),
            // WondrParser(), BaleParser() — pending confirmed samples
        )
    }
}
