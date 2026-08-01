package com.choiyoonseo.automoney.data.repository

import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface AssetRepository {
    fun observeAccounts(): Flow<List<AssetAccount>>
    fun observeFixedExpenses(month: YearMonth): Flow<List<FixedExpensePlan>>
    fun observeMonthlyPlanItems(month: YearMonth): Flow<List<MonthlyPlanItem>>
    suspend fun saveAccount(account: AssetAccount): Long
    suspend fun saveFixedExpense(plan: FixedExpensePlan, month: YearMonth): Long
    suspend fun saveMonthlyPlanItem(item: MonthlyPlanItem, month: YearMonth): Long
    suspend fun deleteFixedExpense(id: Long, month: YearMonth)
    suspend fun deleteMonthlyPlanItem(id: Long)
}
