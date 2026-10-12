package com.example.okane.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room database.
 *
 * version 2: raw_notifications (discovery capture) + unique index on transactions.
 * version 3: manual entry. transactions rebuilt around IN/OUT +
 *            MANUAL/NOTIFICATION enums, categoryId and occurredAt; categories added.
 * version 4: wallets. transactions gain walletId/toWalletId and the TRANSFER
 *            type; categories gain type + isArchived; wallets table added.
 * version 5: capture_suggestions, the "Perlu konfirmasi" inbox for
 *            notifications that need the user's confirmation.
 *
 * Every step is a real migration. Destructive fallback is off, because the
 * database holds data that cannot be recreated: manually entered transactions,
 * and the raw_notifications discovery samples captured on-device.
 *
 * exportSchema = true: writes app/schemas/<version>.json on every build. That
 * file is the reference the hand-written migrations are checked against, so it
 * must stay on and the generated JSON should be kept.
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        WalletEntity::class,
        RawNotification::class,
        CaptureSuggestionEntity::class,
    ],
    version = 5,
    exportSchema = true
)
abstract class OkaneDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun walletDao(): WalletDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun captureDao(): CaptureDao
}
