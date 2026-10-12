package com.example.okane.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the migrations for real and validates them against the exported schema.
 *
 * `runMigrationsAndValidate` fails if the tables a migration produces differ
 * from `schemas/…/<version>.json` in any column, type, nullability or index —
 * the failure mode that would otherwise surface as a crash on a user's device,
 * after their data is already at risk.
 *
 * NEEDS A CONNECTED DEVICE OR EMULATOR:
 *   ./gradlew connectedDebugAndroidTest
 *
 * The same SQL is also exercised on the JVM against plain SQLite by
 * `tools/verify_sql.py`, which needs no device.
 */
@RunWith(AndroidJUnit4::class)
class OkaneDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OkaneDatabase::class.java
    )

    private fun seedV2() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO transactions
                    (source, type, amount, merchant, note, timestamp, createdAt)
                VALUES ('GoPay', 'DEBIT', 50000, 'WARUNG BU SITI', 'raw text',
                        1751871000000, 111)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transactions
                    (source, type, amount, merchant, note, timestamp, createdAt)
                VALUES ('Jenius', 'KREDIT', 2500000, 'Transfer masuk', 'raw 2',
                        1751784600000, 222)
                """.trimIndent()
            )
            // The discovery samples a destructive fallback would have wiped.
            db.execSQL(
                """
                INSERT INTO raw_notifications
                    (packageName, title, text, bigText, postTime, capturedAt)
                VALUES ('com.gojek.gopay', 'Top up berhasil',
                        'Top up Rp10.001 via Mandiri udah masuk.', NULL, 1, 2)
                """.trimIndent()
            )
        }
    }

    @Test
    fun migratesAllTheWayTo4_preservingAndRemappingData() {
        seedV2()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 4, true, MIGRATION_2_3, MIGRATION_3_4
        )

        // Exactly one wallet, created by the migration so walletId can be
        // NOT NULL, and flagged default so the app always has one.
        db.query("SELECT id, name, isDefault FROM wallets").use { c ->
            assertEquals(1, c.count)
            assertTrue(c.moveToFirst())
            assertEquals("Tunai", c.getString(1))
            assertEquals(1, c.getInt(2))
        }

        db.query(
            "SELECT amount, type, walletId, toWalletId, categoryId, occurredAt, " +
                "createdAt, source, note, sourceApp, merchant FROM transactions ORDER BY id"
        ).use { c ->
            assertEquals(2, c.count)

            assertTrue(c.moveToFirst())
            assertEquals(50000L, c.getLong(0))
            assertEquals("OUT", c.getString(1))         // DEBIT -> OUT
            assertEquals(1L, c.getLong(2))              // assigned to the default wallet
            assertTrue(c.isNull(3))                     // not a transfer
            assertTrue(c.isNull(4))                     // no category
            assertEquals(1751871000000L, c.getLong(5))  // timestamp -> occurredAt
            assertEquals(111L, c.getLong(6))
            assertEquals("NOTIFICATION", c.getString(7))
            assertEquals("raw text", c.getString(8))
            assertEquals("GoPay", c.getString(9))       // old source -> sourceApp
            assertEquals("WARUNG BU SITI", c.getString(10))

            assertTrue(c.moveToNext())
            assertEquals("IN", c.getString(1))          // KREDIT -> IN
            assertEquals("Jenius", c.getString(9))
        }

        // The whole reason these are real migrations and not a wipe.
        db.query("SELECT packageName FROM raw_notifications").use { c ->
            assertEquals(1, c.count)
            assertTrue(c.moveToFirst())
            assertEquals("com.gojek.gopay", c.getString(0))
        }

        // Categories are seeded by onboarding, not by a migration, so that
        // upgrading users are not frozen on an older default list.
        db.query("SELECT COUNT(*) FROM categories").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
    }

    @Test
    fun balanceIsComputedCorrectlyWithTransfersBothWays() {
        helper.createDatabase(TEST_DB, 2).close()
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 4, true, MIGRATION_2_3, MIGRATION_3_4
        )

        db.execSQL(
            """
            INSERT INTO wallets (id, name, initialBalance, monthlyFee, isDefault,
                                 sortOrder, isArchived)
            VALUES (10, 'BCA', 1000000, 0, 0, 1, 0), (11, 'OVO', 5000, 0, 0, 2, 0)
            """.trimIndent()
        )

        fun tx(amount: Long, type: String, from: Long, to: Long?) = db.execSQL(
            """
            INSERT INTO transactions (amount, type, walletId, toWalletId, categoryId,
                                      occurredAt, createdAt, source, note,
                                      sourceApp, merchant)
            VALUES ($amount, '$type', $from, ${to?.toString() ?: "NULL"}, NULL,
                    1, 1, 'MANUAL', NULL, NULL, NULL)
            """.trimIndent()
        )

        // Wallet 1 is 'Tunai', created by MIGRATION_3_4 with initialBalance 0.
        tx(50_000, "IN", 1, null)
        tx(20_000, "OUT", 1, null)
        tx(200_000, "TRANSFER", 10, 1)   // BCA -> Tunai
        tx(30_000, "TRANSFER", 1, 10)    // Tunai -> BCA

        // 0 + 50000 - 20000 + 200000 - 30000
        assertEquals(200_000L, balanceOf(db, 1))
        // 1000000 + 30000 - 200000
        assertEquals(830_000L, balanceOf(db, 10))
        // Untouched wallet falls back to initialBalance — proves the IFNULLs.
        assertEquals(5_000L, balanceOf(db, 11))
    }

    /** Mirrors WalletDao.observeBalance; kept literal so a drift shows up here. */
    private fun balanceOf(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        walletId: Long,
    ): Long = db.query(
        """
        SELECT w.initialBalance
          + IFNULL((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'IN'), 0)
          - IFNULL((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'OUT'), 0)
          + IFNULL((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.toWalletId = w.id AND t.type = 'TRANSFER'), 0)
          - IFNULL((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.walletId = w.id AND t.type = 'TRANSFER'), 0)
        FROM wallets w WHERE w.id = $walletId
        """.trimIndent()
    ).use { c ->
        assertTrue(c.moveToFirst())
        c.getLong(0)
    }

    @Test
    fun manualRowsAreNotBlockedByTheNotificationUniqueIndex() {
        helper.createDatabase(TEST_DB, 2).close()
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 4, true, MIGRATION_2_3, MIGRATION_3_4
        )

        // Two identical manual entries: sourceApp is NULL, and SQLite treats
        // NULLs as distinct in a unique index, so both must be stored.
        repeat(2) {
            db.execSQL(
                """
                INSERT INTO transactions (amount, type, walletId, toWalletId,
                                          categoryId, occurredAt, createdAt,
                                          source, note, sourceApp, merchant)
                VALUES (25000, 'OUT', 1, NULL, NULL, 1751871000000, 1751871000000,
                        'MANUAL', NULL, NULL, NULL)
                """.trimIndent()
            )
        }
        db.query("SELECT COUNT(*) FROM transactions").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2, c.getInt(0))
        }
    }

    @Test
    fun duplicateNotificationsAreStillRejected() {
        helper.createDatabase(TEST_DB, 2).close()
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 4, true, MIGRATION_2_3, MIGRATION_3_4
        )

        val insert = """
            INSERT OR IGNORE INTO transactions (amount, type, walletId, toWalletId,
                                                categoryId, occurredAt, createdAt,
                                                source, note, sourceApp, merchant)
            VALUES (10001, 'IN', 1, NULL, NULL, 1751871000000, 1, 'NOTIFICATION',
                    NULL, 'GoPay', 'Top up via Mandiri')
        """.trimIndent()
        db.execSQL(insert)
        db.execSQL(insert)   // same notification delivered twice

        db.query("SELECT COUNT(*) FROM transactions").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
    }

    @Test
    fun migratesTo5_addingTheInboxWithoutTouchingData() {
        seedV2()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 5, true, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

        db.query("SELECT COUNT(*) FROM transactions").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM raw_notifications").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }

        // The inbox starts empty and accepts a suggestion with a NULL title.
        db.execSQL(
            """
            INSERT INTO capture_suggestions
                (sourceApp, packageName, channel, title, text, amount, direction, type,
                 walletId, toWalletId, counterparty, postedAt, createdAt, status)
            VALUES ('Bale', 'com.google.android.apps.messaging', 'SMS', NULL,
                    'Dana masuk Rp2.000.000', 2000000, 'IN', 'IN', 1, NULL, NULL, 1, 1, 'PENDING')
            """.trimIndent()
        )
        db.query("SELECT COUNT(*) FROM capture_suggestions WHERE status = 'PENDING'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "okane-migration-test.db"
    }
}
