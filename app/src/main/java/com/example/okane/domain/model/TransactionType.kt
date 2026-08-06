package com.example.okane.domain.model

/**
 * Direction of money. `Keluar` = [OUT], `Masuk` = [IN] in the UI.
 *
 * [TRANSFER] moves money between two of the user's own wallets. It is the
 * reason amounts are stored positive with the direction in a separate column:
 * a signed amount cannot express a transfer at all, because the same row is
 * negative for one wallet and positive for another.
 *
 * A transfer is net-zero across the user's money as a whole, so it must never
 * be counted as income or expense — topping up an e-wallet from a bank account
 * would otherwise inflate both monthly totals for money that never left the
 * user's control.
 *
 * These enums live in a pure package (no Room, no Android) because both the
 * storage layer and the Android-free keypad engine depend on them.
 */
enum class TransactionType { IN, OUT, TRANSFER }

/** How a row got into the database. */
enum class TransactionSource { MANUAL, NOTIFICATION }
