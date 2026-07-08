package com.choiyoonseo.automoney.data.repository

import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun observeAccounts(): Flow<List<AssetAccount>>
    fun observeFixedExpenses(): Flow<List<FixedExpensePlan>>
    fun observeMonthlyPlanItems(): Flow<List<MonthlyPlanItem>>
    suspend fun saveAccount(account: AssetAccount): Long
    suspend fun saveFixedExpense(plan: FixedExpensePlan): Long
    suspend fun saveMonthlyPlanItem(item: MonthlyPlanItem): Long
}
