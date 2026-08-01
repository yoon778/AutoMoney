package com.choiyoonseo.automoney.data.repository

import androidx.room.withTransaction
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

    override fun observeFixedExpenses(month: YearMonth): Flow<List<FixedExpensePlan>> =
        db.assetDao().observeFixedExpenses(month.toString()).map { plans -> plans.map { it.toDomain() } }

    override fun observeMonthlyPlanItems(month: YearMonth): Flow<List<MonthlyPlanItem>> =
        db.assetDao().observeMonthlyPlanItems(month.toString()).map { items -> items.map { it.toDomain() } }

    override suspend fun saveAccount(account: AssetAccount): Long =
        db.assetDao().insertAccount(account.toEntity())

    override suspend fun saveFixedExpense(plan: FixedExpensePlan, month: YearMonth): Long =
        db.withTransaction {
            val validated = plan.validatedForSave()
            if (validated.id == 0L) {
                val insertedId = db.assetDao().insertFixedExpense(validated.toNewEntity(month))
                db.assetDao().insertFixedExpense(
                    validated.toNewEntity(month).copy(id = insertedId, seriesId = insertedId)
                )
                insertedId
            } else {
                val existing = requireNotNull(db.assetDao().fixedExpenseById(validated.id)) {
                    "수정할 고정지출을 찾을 수 없어요."
                }
                val existingStart = YearMonth.parse(existing.effectiveFromMonth)
                require(!month.isBefore(existingStart)) { "고정지출 시작 월보다 이전 달은 수정할 수 없어요." }

                db.assetDao().endFixedExpenseVersionsFrom(
                    existing.seriesId,
                    month.toString(),
                    month.minusMonths(1).toString()
                )
                if (month == existingStart) {
                    db.assetDao().insertFixedExpense(
                        validated.toVersionEntity(
                            id = existing.id,
                            seriesId = existing.seriesId,
                            month = month
                        )
                    )
                } else {
                    db.assetDao().insertFixedExpense(
                        existing.copy(effectiveToMonth = month.minusMonths(1).toString())
                    )
                    db.assetDao().insertFixedExpense(
                        validated.toVersionEntity(seriesId = existing.seriesId, month = month)
                    )
                }
            }
        }

    override suspend fun saveMonthlyPlanItem(item: MonthlyPlanItem, month: YearMonth): Long =
        db.assetDao().insertMonthlyPlanItem(item.toEntity(month))

    override suspend fun deleteFixedExpense(id: Long, month: YearMonth) {
        db.withTransaction {
            val existing = db.assetDao().fixedExpenseById(id) ?: return@withTransaction
            val existingStart = YearMonth.parse(existing.effectiveFromMonth)
            require(!month.isBefore(existingStart)) { "고정지출 시작 월보다 이전 달은 삭제할 수 없어요." }

            db.assetDao().endFixedExpenseVersionsFrom(
                existing.seriesId,
                month.toString(),
                month.minusMonths(1).toString()
            )
            if (month.isAfter(existingStart)) {
                db.assetDao().insertFixedExpense(
                    existing.copy(effectiveToMonth = month.minusMonths(1).toString())
                )
            }
        }
    }

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

private fun FixedExpensePlan.toNewEntity(month: YearMonth): FixedExpenseEntity =
    FixedExpenseEntity(
        id = 0,
        seriesId = 0,
        name = name,
        amountWon = amountWon,
        withdrawalDay = withdrawalDay,
        accountName = accountName,
        accountId = accountId,
        active = active,
        effectiveFromMonth = month.toString()
    )

private fun FixedExpensePlan.toVersionEntity(
    id: Long = 0,
    seriesId: Long,
    month: YearMonth
): FixedExpenseEntity =
    FixedExpenseEntity(
        id = id,
        seriesId = seriesId,
        name = name,
        amountWon = amountWon,
        withdrawalDay = withdrawalDay,
        accountName = accountName,
        accountId = accountId,
        active = active,
        effectiveFromMonth = month.toString()
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
