package com.choiyoonseo.automoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choiyoonseo.automoney.data.local.entity.AssetAccountEntity
import com.choiyoonseo.automoney.data.local.entity.FixedExpenseEntity
import com.choiyoonseo.automoney.data.local.entity.MonthlyPlanItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM asset_accounts ORDER BY balanceWon DESC")
    fun observeAccounts(): Flow<List<AssetAccountEntity>>

    @Query("SELECT * FROM asset_accounts ORDER BY balanceWon DESC")
    suspend fun accountsOnce(): List<AssetAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(entity: AssetAccountEntity): Long

    @Query("SELECT * FROM fixed_expenses ORDER BY withdrawalDay ASC, amountWon DESC")
    fun observeFixedExpenses(): Flow<List<FixedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpense(entity: FixedExpenseEntity): Long

    @Query("SELECT * FROM monthly_plan_items ORDER BY type ASC, amountWon DESC")
    fun observeMonthlyPlanItems(): Flow<List<MonthlyPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyPlanItem(entity: MonthlyPlanItemEntity): Long
}
