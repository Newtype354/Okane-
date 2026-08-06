package com.example.okane.domain.parser

/**
 * The result a per-app parser extracts from one notification's text.
 * Deliberately small: source, timestamps and raw text are added by
 * NotificationParser, so each per-app parser only solves regex problems.
 */
data class ParsedTransaction(
    // Transaction.TYPE_DEBIT (money out) or Transaction.TYPE_KREDIT (money in).
    val type: String,
    // Whole rupiah.
    val amount: Long,
    // Store name / recipient, best-effort from the text.
    val merchant: String
)
