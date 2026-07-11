package com.choiyoonseo.automoney.data.local

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migration2To7PreservesLegacyRowsAndValidatesSchema() {
        helper.createDatabase(TEST_DB, 2).apply {
            insertTransaction(id = 1, sourceNotificationHash = "same-hash")
            insertTransaction(id = 2, sourceNotificationHash = "same-hash")
            insertTransaction(id = 3, sourceNotificationHash = "unique-hash")
            insertReviewItem(id = 10, transactionId = 1, resolvedAt = "2026-07-01T00:00:00Z")
            insertReviewItem(id = 11, transactionId = 1, resolvedAt = null)
            insertReviewItem(id = 12, transactionId = 999, resolvedAt = null)
            insertAssetAccount(id = 20)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )

        assertEquals("same-hash", db.singleString("SELECT sourceNotificationHash FROM transactions WHERE id = 1"))
        assertNull(db.singleString("SELECT sourceNotificationHash FROM transactions WHERE id = 2"))
        assertEquals(1, db.singleLong("SELECT COUNT(*) FROM review_items"))
        assertEquals(1, db.singleLong("SELECT transactionId FROM review_items"))
        assertEquals(11, db.singleLong("SELECT id FROM review_items"))
        assertEquals(0, db.singleLong("SELECT COUNT(*) FROM review_items WHERE transactionId = 999"))
        assertEquals(0, db.singleLong("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'wallets'"))
        assertNull(db.singleString("SELECT bankProvider FROM asset_accounts WHERE id = 20"))
        assertNull(db.singleString("SELECT accountLast4 FROM asset_accounts WHERE id = 20"))
        assertNull(db.singleString("SELECT linkedAssetAccountId FROM transactions WHERE id = 1"))
        assertNull(db.singleString("SELECT balanceImpact FROM transactions WHERE id = 1"))
    }

    @Test
    fun migration7To8AddsCustomCategoryAndSettlementFields() {
        helper.createDatabase(SECOND_TEST_DB, 7).apply {
            insertTransaction(id = 1, sourceNotificationHash = "legacy-hash")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            SECOND_TEST_DB,
            8,
            true,
            AppDatabase.MIGRATION_7_8
        )

        assertEquals(0, db.singleLong("SELECT COUNT(*) FROM user_categories"))
        assertNull(db.singleString("SELECT customCategoryName FROM transactions WHERE id = 1"))
        assertNull(db.singleString("SELECT settlementMyShareWon FROM transactions WHERE id = 1"))
        assertEquals(0, db.singleLong("SELECT settlementTrackingHidden FROM transactions WHERE id = 1"))
        assertEquals(
            1,
            db.singleLong(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_transactions_settlementParentId'"
            )
        )
    }

    private fun SupportSQLiteDatabase.insertTransaction(
        id: Long,
        sourceNotificationHash: String?
    ) {
        execSQL(
            """
            INSERT INTO transactions (
                id, occurredAt, amountWon, direction, type, category, paymentMethod,
                merchant, counterparty, memo, sourceApp, sourceType, sourceNotificationHash,
                status, confidence, monthKey
            ) VALUES (
                ?, '2026-07-01T00:00:00Z', 1000, 'EXPENSE', 'EXPENSE', 'FOOD', 'KB',
                'store', NULL, NULL, 'test', 'NOTIFICATION', ?, 'AUTO_CONFIRMED', 0.9, '2026-07'
            )
            """.trimIndent(),
            arrayOf<Any?>(id, sourceNotificationHash)
        )
    }

    private fun SupportSQLiteDatabase.insertReviewItem(
        id: Long,
        transactionId: Long,
        resolvedAt: String?
    ) {
        execSQL(
            """
            INSERT INTO review_items (id, transactionId, reason, createdAt, resolvedAt)
            VALUES (?, ?, 'WALLET_TOPUP', '2026-07-01T00:00:00Z', ?)
            """.trimIndent(),
            arrayOf<Any?>(id, transactionId, resolvedAt)
        )
    }

    private fun SupportSQLiteDatabase.insertAssetAccount(id: Long) {
        execSQL(
            """
            INSERT INTO asset_accounts (id, name, balanceWon, kind)
            VALUES (?, 'legacy account', 10000, 'BANK')
            """.trimIndent(),
            arrayOf<Any?>(id)
        )
    }

    private fun SupportSQLiteDatabase.singleString(sql: String): String? =
        query(sql).useSingle { cursor -> if (cursor.isNull(0)) null else cursor.getString(0) }

    private fun SupportSQLiteDatabase.singleLong(sql: String): Long =
        query(sql).useSingle { cursor -> cursor.getLong(0) }

    private inline fun <T> Cursor.useSingle(block: (Cursor) -> T): T {
        use { cursor ->
            assertTrue(cursor.moveToFirst())
            return block(cursor)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val SECOND_TEST_DB = "migration-test-7-8"
    }
}
