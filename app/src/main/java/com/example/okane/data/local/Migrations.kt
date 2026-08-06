package com.example.okane.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * Migrations are frozen history. Each one must keep producing the schema its
 * target version had, so nothing here may reference DefaultCategories,
 * DefaultWallets or the entity classes — those move on, migrations do not.
 * The DDL is spelled out literally for the same reason.
 *
 * Every CREATE TABLE below must match Room's own generated DDL exactly, or
 * Room throws on open. `app/schemas/<version>.json` is the reference; it is
 * regenerated on every build, so diff against it after any entity change.
 *
 * Destructive fallback is off (see DatabaseModule): raw_notifications holds
 * discovery samples the user collected on-device and cannot recover, and the
 * transactions table now holds hand-entered rows.
 */

/**
 * v2 → v3: manual entry.
 *
 * SQLite cannot retype or rename columns in place, so the transactions table
 * is rebuilt the standard way: create, copy, drop, rename.
 *
 * This step deliberately creates the categories table **empty**. Seeding is
 * owned by onboarding, which is idempotent and knows the current default set;
 * seeding here as well would leave users on an older default list forever.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `categoryId` INTEGER,
                `occurredAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `note` TEXT,
                `sourceApp` TEXT,
                `merchant` TEXT
            )
            """.trimIndent()
        )

        // Existing rows all came from notifications, so:
        //   old `source` (the app name) -> `sourceApp`
        //   new `source`                -> 'NOTIFICATION'
        //   old `timestamp`             -> `occurredAt`
        //   KREDIT/DEBIT                -> IN/OUT
        // Room stores enums as their constant name in a TEXT column, which is
        // why the CASE below writes strings rather than ordinals.
        db.execSQL(
            """
            INSERT INTO `transactions_new`
                (`id`, `amount`, `type`, `categoryId`, `occurredAt`,
                 `createdAt`, `source`, `note`, `sourceApp`, `merchant`)
            SELECT
                `id`,
                `amount`,
                CASE `type` WHEN 'KREDIT' THEN 'IN' ELSE 'OUT' END,
                NULL,
                `timestamp`,
                `createdAt`,
                'NOTIFICATION',
                `note`,
                `source`,
                `merchant`
            FROM `transactions`
            """.trimIndent()
        )

        // Dropping the old table also drops its unique index.
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_transactions_sourceApp_type_amount_occurredAt` " +
                "ON `transactions` (`sourceApp`, `type`, `amount`, `occurredAt`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `isOther` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * v3 → v4: wallets and transfers.
 *
 * Three changes, none destructive:
 *
 * 1. `wallets` is created and a default `Tunai` is inserted, because
 *    `transactions.walletId` is NOT NULL and every pre-existing row needs a
 *    wallet to belong to. Onboarding later adds to this list rather than
 *    replacing it.
 * 2. `transactions` gains `walletId` (every existing row assigned to that
 *    default wallet) and `toWalletId`, plus indices for the balance query.
 * 3. `categories` gains `type` and `isArchived` and loses `isOther`. Existing
 *    rows are kept, not re-seeded — `Gaji` is the only default that is income;
 *    anything else a user had is far more likely to be an expense.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- wallets -------------------------------------------------------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wallets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `initialBalance` INTEGER NOT NULL,
                `monthlyFee` INTEGER NOT NULL,
                `isDefault` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `wallets`
                (`name`, `initialBalance`, `monthlyFee`, `isDefault`,
                 `sortOrder`, `isArchived`)
            VALUES ('Tunai', 0, 0, 1, 0, 0)
            """.trimIndent()
        )

        // --- transactions --------------------------------------------------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `walletId` INTEGER NOT NULL,
                `toWalletId` INTEGER,
                `categoryId` INTEGER,
                `occurredAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `note` TEXT,
                `sourceApp` TEXT,
                `merchant` TEXT
            )
            """.trimIndent()
        )
        // Every pre-existing row is assigned to the wallet just created, and
        // keeps source = NOTIFICATION (or MANUAL) exactly as it was.
        db.execSQL(
            """
            INSERT INTO `transactions_new`
                (`id`, `amount`, `type`, `walletId`, `toWalletId`, `categoryId`,
                 `occurredAt`, `createdAt`, `source`, `note`, `sourceApp`, `merchant`)
            SELECT
                `id`,
                `amount`,
                `type`,
                (SELECT `id` FROM `wallets` WHERE `isDefault` = 1 LIMIT 1),
                NULL,
                `categoryId`,
                `occurredAt`,
                `createdAt`,
                `source`,
                `note`,
                `sourceApp`,
                `merchant`
            FROM `transactions`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_transactions_sourceApp_type_amount_occurredAt` " +
                "ON `transactions` (`sourceApp`, `type`, `amount`, `occurredAt`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_walletId` " +
                "ON `transactions` (`walletId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_toWalletId` " +
                "ON `transactions` (`toWalletId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` " +
                "ON `transactions` (`categoryId`)"
        )

        // --- categories ----------------------------------------------------
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `categories_new`
                (`id`, `name`, `type`, `sortOrder`, `isArchived`)
            SELECT
                `id`,
                `name`,
                CASE WHEN `name` = 'Gaji' THEN 'IN' ELSE 'OUT' END,
                `sortOrder`,
                0
            FROM `categories`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `categories`")
        db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
    }
}

/** Registered in DatabaseModule; kept together so none is forgotten. */
val OKANE_MIGRATIONS = arrayOf(MIGRATION_2_3, MIGRATION_3_4)
