package com.choiyoonseo.automoney.data.repository

import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.local.entity.AssetAccountEntity
import com.choiyoonseo.automoney.data.local.entity.FixedExpenseEntity
import com.choiyoonseo.automoney.data.local.entity.MonthlyPlanItemEntity
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import com.choiyoonseo.automoney.domain.assets.validatedForSave
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

class RoomAssetRepository(
    private val db: AppDatabase
) : AssetRepository {
    override fun observeAccounts(): Flow<List<AssetAccount>> =
        db.assetDao().observeAccounts().map { accounts -> accounts.map { it.toDomain() } }

    override fun observeFixedExpenses(): Flow<List<FixedExpensePlan>> =
        db.assetDao().observeFixedExpenses().map { plans -> plans.map { it.toDomain() } }

    override fun observeMonthlyPlanItems(month: YearMonth): Flow<List<MonthlyPlanItem>> =
        db.assetDao().observeMonthlyPlanItems(month.toString()).map { items -> items.map { it.toDomain() } }

    override suspend fun saveAccount(account: AssetAccount): Long =
        db.assetDao().insertAccount(account.toEntity())

    override suspend fun saveFixedExpense(plan: FixedExpensePlan): Long =
        db.assetDao().insertFixedExpense(plan.validatedForSave().toEntity())

    override suspend fun saveMonthlyPlanItem(item: MonthlyPlanItem, month: YearMonth): Long =
        db.assetDao().insertMonthlyPlanItem(item.toEntity(month))

    override suspend fun deleteFixedExpense(id: Long) =
        db.assetDao().deleteFixedExpense(id)

    override suspend fun deleteMonthlyPlanItem(id: Long) =
        db.assetDao().deleteMonthlyPlanItem(id)
}

private fun AssetAccountEntity.toDomain(): AssetAccount =
    AssetAccount(
        id = id,
        name = name,
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = bankProvider,
        accountLast4 = accountLast4,
        providerLabel = providerLabel
    )

private fun AssetAccount.toEntity(): AssetAccountEntity =
    AssetAccountEntity(
        id = id,
        name = name,
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = bankProvider,
        accountLast4 = accountLast4,
        providerLabel = providerLabel
    )

private fun FixedExpenseEntity.toDomain(): FixedExpensePlan =
    FixedExpensePlan(
        id = id,
        name = name,
        amountWon = amountWon,
        withdrawalDay = withdrawalDay,
        accountName = accountName,
        accountId = accountId,
        active = active
    )

private fun FixedExpensePlan.toEntity(): FixedExpenseEntity =
    FixedExpenseEntity(
        id = id,
        name = name,
        amountWon = amountWon,
        withdrawalDay = withdrawalDay,
        accountName = accountName,
        accountId = accountId,
        active = active
    )

private fun MonthlyPlanItemEntity.toDomain(): MonthlyPlanItem =
    MonthlyPlanItem(
        id = id,
        label = label,
        amountWon = amountWon,
        type = type,
        category = category,
        customCategoryId = customCategoryId,
        customCategoryName = customCategoryName
    )

private fun MonthlyPlanItem.toEntity(month: YearMonth): MonthlyPlanItemEntity =
    MonthlyPlanItemEntity(
        id = id,
        label = label,
        amountWon = amountWon,
        type = type,
        category = category,
        customCategoryId = customCategoryId,
        customCategoryName = customCategoryName,
        monthKey = month.toString()
    )
