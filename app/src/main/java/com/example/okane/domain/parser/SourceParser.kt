package com.example.okane.domain.parser

/**
 * One implementation per financial app (GoPayParser, LivinParser, ...).
 *
 * Rules for implementations:
 *  - Pure logic only (no Android classes) so plain JVM unit tests work.
 *  - Return null for ANYTHING that is not clearly a transaction
 *    (promos, OTP codes, balance info, group summaries...).
 *    A missed transaction is recoverable; a false one corrupts the data.
 */
interface SourceParser {

    /** Human-readable source stored on transactions, e.g. "GoPay". */
    val source: String

    /**
     * Package name(s) this parser handles. A set because some services ship
     * multiple app variants (e.g. a super-app and a standalone app).
     * Values must be CONFIRMED from the device, never guessed.
     */
    val packageNames: Set<String>

    /**
     * Extracts a transaction from a notification's title + text.
     * @param text the richest text available (bigText when present).
     */
    fun parse(title: String?, text: String?): ParsedTransaction?
}
