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
import com.choiyoonseo.automoney.data.local.dao.WalletDao
import com.choiyoonseo.automoney.data.local.entity.AssetAccountEntity
import com.choiyoonseo.automoney.data.local.entity.FixedExpenseEntity
import com.choiyoonseo.automoney.data.local.entity.MonthlyPlanItemEntity
import com.choiyoonseo.automoney.data.local.entity.ReviewItemEntity
import com.choiyoonseo.automoney.data.local.entity.RuleEntity
import com.choiyoonseo.automoney.data.local.entity.TransactionEntity
import com.choiyoonseo.automoney.data.local.entity.WalletEntity

@Database(
    entities = [
        TransactionEntity::class,
        ReviewItemEntity::class,
        RuleEntity::class,
        WalletEntity::class,
        AssetAccountEntity::class,
        FixedExpenseEntity::class,
        MonthlyPlanItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun reviewItemDao(): ReviewItemDao
    abstract fun ruleDao(): RuleDao
    abstract fun walletDao(): WalletDao
    abstract fun assetDao(): AssetDao

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
    }
}
