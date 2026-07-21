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
    fun migration4To12PreservesWalletBalancesAsAssets() {
        helper.createDatabase(WALLET_TEST_DB, 4).apply {
            insertWallet(name = "네이버페이", type = "PREPAID", balanceWon = 12_000)
            insertWallet(name = "포인트", type = "POINT", balanceWon = 3_000)
            insertWallet(name = "현금지갑", type = "CASH_LIKE", balanceWon = 50_000)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            WALLET_TEST_DB,
            12,
            true,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12
        )

        assertEquals(12_000, db.singleLong("SELECT balanceWon FROM asset_accounts WHERE name = '네이버페이'"))
        assertEquals("PAY", db.singleString("SELECT kind FROM asset_accounts WHERE name = '네이버페이'"))
        assertEquals(3_000, db.singleLong("SELECT balanceWon FROM asset_accounts WHERE name = '포인트'"))
        assertEquals("PAY", db.singleString("SELECT kind FROM asset_accounts WHERE name = '포인트'"))
        assertEquals(50_000, db.singleLong("SELECT balanceWon FROM asset_accounts WHERE name = '현금지갑'"))
        assertEquals("CASH", db.singleString("SELECT kind FROM asset_accounts WHERE name = '현금지갑'"))
        assertEquals(0, db.singleLong("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'wallets'"))
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

    @Test
    fun migration8To9AddsNullableFixedExpenseAccountId() {
        helper.createDatabase(THIRD_TEST_DB, 8).apply {
            insertFixedExpense(id = 1)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            THIRD_TEST_DB,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )

        assertEquals("legacy expense", db.singleString("SELECT name FROM fixed_expenses WHERE id = 1"))
        assertNull(db.singleString("SELECT accountId FROM fixed_expenses WHERE id = 1"))
        assertEquals(
            1,
            db.singleLong(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_fixed_expenses_accountId'"
            )
        )
    }

    @Test
    fun migration9To10AddsNullableBudgetCategoryFields() {
        helper.createDatabase(FOURTH_TEST_DB, 9).apply {
            insertMonthlyPlanItem(id = 1)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            FOURTH_TEST_DB,
            10,
            true,
            AppDatabase.MIGRATION_9_10
        )

        assertEquals("legacy budget", db.singleString("SELECT label FROM monthly_plan_items WHERE id = 1"))
        assertNull(db.singleString("SELECT category FROM monthly_plan_items WHERE id = 1"))
        assertNull(db.singleString("SELECT customCategoryId FROM monthly_plan_items WHERE id = 1"))
        assertNull(db.singleString("SELECT customCategoryName FROM monthly_plan_items WHERE id = 1"))
    }

    @Test
    fun migration10To11AddsNullableTransactionBudgetPlanId() {
        helper.createDatabase(FIFTH_TEST_DB, 10).apply {
            insertTransaction(
                id = 1,
                sourceNotificationHash = "legacy-budget-hash",
                hasSettlementTrackingHidden = true
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            FIFTH_TEST_DB,
            11,
            true,
            AppDatabase.MIGRATION_10_11
        )

        assertNull(db.singleString("SELECT budgetPlanId FROM transactions WHERE id = 1"))
    }

    @Test
    fun migration11To12AddsNullableFixedExpensePlanId() {
        helper.createDatabase(SIXTH_TEST_DB, 11).apply {
            insertTransaction(
                id = 1,
                sourceNotificationHash = "legacy-fixed-expense-hash",
                hasSettlementTrackingHidden = true
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            SIXTH_TEST_DB,
            12,
            true,
            AppDatabase.MIGRATION_11_12
        )

        assertNull(db.singleString("SELECT fixedExpensePlanId FROM transactions WHERE id = 1"))
    }

    @Test
    fun migration12To13AddsRefundParentSelfReference() {
        helper.createDatabase(SEVENTH_TEST_DB, 12).apply {
            insertTransaction(
                id = 1,
                sourceNotificationHash = "legacy-refund-parent-hash",
                hasSettlementTrackingHidden = true
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            SEVENTH_TEST_DB,
            13,
            true,
            AppDatabase.MIGRATION_12_13
        )

        assertNull(db.singleString("SELECT refundParentTransactionId FROM transactions WHERE id = 1"))
        assertEquals(1, db.refundParentForeignKeyCount())
        assertEquals(
            1,
            db.singleLong(
                "SELECT COUNT(*) FROM sqlite_master " +
                    "WHERE type = 'index' AND name = 'index_transactions_refundParentTransactionId'"
            )
        )
        db.close()
    }

    private fun SupportSQLiteDatabase.insertTransaction(
        id: Long,
        sourceNotificationHash: String?,
        hasSettlementTrackingHidden: Boolean = false
    ) {
        val extraColumn = if (hasSettlementTrackingHidden) ", settlementTrackingHidden" else ""
        val extraValue = if (hasSettlementTrackingHidden) ", 0" else ""
        execSQL(
            """
            INSERT INTO transactions (
                id, occurredAt, amountWon, direction, type, category, paymentMethod,
                merchant, counterparty, memo, sourceApp, sourceType, sourceNotificationHash,
                status, confidence, monthKey$extraColumn
            ) VALUES (
                ?, '2026-07-01T00:00:00Z', 1000, 'EXPENSE', 'EXPENSE', 'FOOD', 'KB',
                'store', NULL, NULL, 'test', 'NOTIFICATION', ?, 'AUTO_CONFIRMED', 0.9, '2026-07'$extraValue
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

    private fun SupportSQLiteDatabase.insertWallet(
        name: String,
        type: String,
        balanceWon: Long
    ) {
        execSQL(
            "INSERT INTO wallets (name, type, balanceWon) VALUES (?, ?, ?)",
            arrayOf<Any?>(name, type, balanceWon)
        )
    }

    private fun SupportSQLiteDatabase.insertFixedExpense(id: Long) {
        execSQL(
            """
            INSERT INTO fixed_expenses (id, name, amountWon, withdrawalDay, accountName, active)
            VALUES (?, 'legacy expense', 10000, 1, 'legacy account', 1)
            """.trimIndent(),
            arrayOf<Any?>(id)
        )
    }

    private fun SupportSQLiteDatabase.insertMonthlyPlanItem(id: Long) {
        execSQL(
            """
            INSERT INTO monthly_plan_items (id, label, amountWon, type)
            VALUES (?, 'legacy budget', 100000, 'BUDGET')
            """.trimIndent(),
            arrayOf<Any?>(id)
        )
    }

    private fun SupportSQLiteDatabase.singleString(sql: String): String? =
        query(sql).useSingle { cursor -> if (cursor.isNull(0)) null else cursor.getString(0) }

    private fun SupportSQLiteDatabase.singleLong(sql: String): Long =
        query(sql).useSingle { cursor -> cursor.getLong(0) }

    private fun SupportSQLiteDatabase.refundParentForeignKeyCount(): Int =
        query("PRAGMA foreign_key_list(`transactions`)").use { cursor ->
            val fromIndex = cursor.getColumnIndexOrThrow("from")
            val tableIndex = cursor.getColumnIndexOrThrow("table")
            val onDeleteIndex = cursor.getColumnIndexOrThrow("on_delete")
            var count = 0
            while (cursor.moveToNext()) {
                if (
                    cursor.getString(fromIndex) == "refundParentTransactionId" &&
                    cursor.getString(tableIndex) == "transactions" &&
                    cursor.getString(onDeleteIndex) == "SET NULL"
                ) {
                    count += 1
                }
            }
            count
        }

    private inline fun <T> Cursor.useSingle(block: (Cursor) -> T): T {
        use { cursor ->
            assertTrue(cursor.moveToFirst())
            return block(cursor)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val SECOND_TEST_DB = "migration-test-7-8"
        const val THIRD_TEST_DB = "migration-test-8-9"
        const val FOURTH_TEST_DB = "migration-test-9-10"
        const val FIFTH_TEST_DB = "migration-test-10-11"
        const val SIXTH_TEST_DB = "migration-test-11-12"
        const val SEVENTH_TEST_DB = "migration-test-12-13"
        const val WALLET_TEST_DB = "migration-test-4-5-wallets"
    }
}
