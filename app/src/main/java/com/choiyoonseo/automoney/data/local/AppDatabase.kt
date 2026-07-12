package com.choiyoonseo.automoney.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.choiyoonseo.automoney.data.local.dao.AssetDao
import com.choiyoonseo.automoney.data.local.dao.ReviewItemDao
import com.choiyoonseo.automoney.data.local.dao.RuleDao
import com.choiyoonseo.automoney.data.local.dao.TransactionDao
import com.choiyoonseo.automoney.data.local.dao.UserCategoryDao
import com.choiyoonseo.automoney.data.local.entity.AssetAccountEntity
import com.choiyoonseo.automoney.data.local.entity.FixedExpenseEntity
import com.choiyoonseo.automoney.data.local.entity.MonthlyPlanItemEntity
import com.choiyoonseo.automoney.data.local.entity.ReviewItemEntity
import com.choiyoonseo.automoney.data.local.entity.RuleEntity
import com.choiyoonseo.automoney.data.local.entity.TransactionEntity
import com.choiyoonseo.automoney.data.local.entity.UserCategoryEntity

@Database(
    entities = [
        TransactionEntity::class,
        ReviewItemEntity::class,
        RuleEntity::class,
        AssetAccountEntity::class,
        FixedExpenseEntity::class,
        MonthlyPlanItemEntity::class,
        UserCategoryEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun reviewItemDao(): ReviewItemDao
    abstract fun ruleDao(): RuleDao
    abstract fun assetDao(): AssetDao
    abstract fun userCategoryDao(): UserCategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS asset_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        balanceWon INTEGER NOT NULL,
                        kind TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fixed_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amountWon INTEGER NOT NULL,
                        withdrawalDay INTEGER NOT NULL,
                        accountName TEXT NOT NULL,
                        active INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS monthly_plan_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        amountWon INTEGER NOT NULL,
                        type TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE transactions
                    SET sourceNotificationHash = NULL
                    WHERE sourceNotificationHash IS NOT NULL
                        AND id NOT IN (
                            SELECT MIN(id)
                            FROM transactions
                            WHERE sourceNotificationHash IS NOT NULL
                            GROUP BY sourceNotificationHash
                        )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_sourceNotificationHash
                    ON transactions(sourceNotificationHash)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS review_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        transactionId INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        resolvedAt TEXT,
                        FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO review_items_new (id, transactionId, reason, createdAt, resolvedAt)
                    SELECT id, transactionId, reason, createdAt, resolvedAt
                    FROM review_items
                    WHERE EXISTS (
                        SELECT 1
                        FROM transactions
                        WHERE transactions.id = review_items.transactionId
                    )
                        AND id IN (
                            SELECT COALESCE(
                                MAX(CASE WHEN resolvedAt IS NULL THEN id END),
                                MAX(id)
                            )
                            FROM review_items
                            GROUP BY transactionId
                        )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE review_items")
                db.execSQL("ALTER TABLE review_items_new RENAME TO review_items")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_review_items_transactionId
                    ON review_items(transactionId)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_transactions_monthKey_occurredAt
                    ON transactions(monthKey, occurredAt)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_transactions_occurredAt
                    ON transactions(occurredAt)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_review_items_resolvedAt_createdAt
                    ON review_items(resolvedAt, createdAt)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_rules_enabled
                    ON rules(enabled)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS wallets")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE asset_accounts ADD COLUMN bankProvider TEXT")
                db.execSQL("ALTER TABLE asset_accounts ADD COLUMN accountLast4 TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN linkedAssetAccountId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN balanceImpact TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_linkedAssetAccountId " +
                        "ON transactions(linkedAssetAccountId)"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE asset_accounts ADD COLUMN providerLabel TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        kind TEXT NOT NULL,
                        name TEXT NOT NULL,
                        normalizedName TEXT NOT NULL,
                        active INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_user_categories_kind_normalizedName " +
                        "ON user_categories(kind, normalizedName)"
                )
                db.execSQL("ALTER TABLE transactions ADD COLUMN customCategoryId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN customCategoryName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN settlementPartyCount INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN settlementMyShareWon INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN settlementParentId INTEGER")
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN settlementTrackingHidden INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_customCategoryId " +
                        "ON transactions(customCategoryId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_settlementParentId " +
                        "ON transactions(settlementParentId)"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fixed_expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amountWon INTEGER NOT NULL,
                        withdrawalDay INTEGER NOT NULL,
                        accountName TEXT NOT NULL,
                        accountId INTEGER,
                        active INTEGER NOT NULL,
                        FOREIGN KEY(accountId) REFERENCES asset_accounts(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO fixed_expenses_new (id, name, amountWon, withdrawalDay, accountName, active)
                    SELECT id, name, amountWon, withdrawalDay, accountName, active
                    FROM fixed_expenses
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE fixed_expenses")
                db.execSQL("ALTER TABLE fixed_expenses_new RENAME TO fixed_expenses")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_fixed_expenses_accountId ON fixed_expenses(accountId)"
                )
            }
        }
    }
}
