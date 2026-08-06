package com.example.okane.data.local

/**
 * One financial transaction parsed from a banking/e-wallet notification.
 *
 * NO LONGER A ROOM ENTITY. [TransactionEntity] is the table; this is the
 * notification path's own model, produced by NotificationParser and mapped to
 * a row inside TransactionRepository.
 *
 * The indirection is deliberate. `source` here means the app name ("GoPay"),
 * whereas `TransactionEntity.source` means MANUAL vs NOTIFICATION — two
 * different ideas that happen to share a word. Keeping this class lets
 * OkaneNotificationListenerService, NotificationParser and every parser test
 * stay byte-for-byte unchanged while the storage schema moves underneath them.
 *
 * Duplicate protection moved with the table: see the unique index on
 * [TransactionEntity].
 */
data class Transaction(
    // 0 means "not saved yet"; the row id is assigned on insert.
    val id: Long = 0,

    // Which app produced this, e.g. "GoPay", "Livin".
    val source: String,

    // TYPE_DEBIT (money out) or TYPE_KREDIT (money in). Stored as a plain
    // String to keep the schema simple; use the constants below to avoid typos.
    val type: String,

    // Amount in whole rupiah (IDR has no decimals in practice).
    // Long, not Int: balances can exceed Int's ~2.1 billion limit.
    val amount: Long,

    // Store name or recipient parsed from the notification.
    val merchant: String,

    // Raw notification text, kept for debugging bad parses later.
    val note: String,

    // When the transaction happened (epoch milliseconds).
    val timestamp: Long,

    // When this app recorded it (epoch milliseconds).
    val createdAt: Long
) {
    companion object {
        const val TYPE_DEBIT = "DEBIT"
        const val TYPE_KREDIT = "KREDIT"
    }
}
