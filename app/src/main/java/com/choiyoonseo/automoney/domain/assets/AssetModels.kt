package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.countsAsPlannedUse
import com.choiyoonseo.automoney.domain.report.effectiveExpenseWon

enum class AssetAccountKind(val label: String) {
    BANK("은행"),
    SECURITIES("증권"),
    PAY("페이"),
    CASH("현금"),
    OTHER("기타")
}

data class AssetAccount(
    val id: Long = 0,
    val name: String,
    val balanceWon: Long,
    val kind: AssetAccountKind = AssetAccountKind.BANK,
    val bankProvider: BankProvider? = null,
    val accountLast4: String? = null,
    val providerLabel: String? = null
)

fun AssetAccount.validatedForSave(): AssetAccount {
    val cleanName = name.trim()
    require(cleanName.isNotBlank()) { "계좌 이름을 입력해 주세요." }
    require(balanceWon >= 0) { "잔액은 0원 이상이어야 해요." }
    if (kind == AssetAccountKind.BANK) {
        require((bankProvider == null) == (accountLast4 == null)) {
            "은행과 계좌번호 끝 네 자리를 함께 입력해 주세요."
        }
    } else {
        require(bankProvider == null && accountLast4 == null) {
            "은행 계좌가 아닌 자산에는 계좌 정보를 저장할 수 없어요."
        }
    }
    val cleanProvider = providerLabel?.trim()?.takeIf { it.isNotBlank() }
        ?.takeIf { kind == AssetAccountKind.SECURITIES || kind == AssetAccountKind.PAY }
    return copy(name = cleanName, providerLabel = cleanProvider)
}

data class FixedExpensePlan(
    val id: Long = 0,
    val name: String,
    val amountWon: Long,
    val withdrawalDay: Int,
    val accountName: String,
    val accountId: Long? = null,
    val active: Boolean = true
)

val fixedExpenseWithdrawalDayOptions: IntRange = 1..31

fun FixedExpensePlan.validatedForSave(): FixedExpensePlan {
    val cleanName = name.trim()
    val cleanAccountName = accountName.trim()
    require(cleanName.isNotBlank()) { "고정지출 이름을 입력해 주세요." }
    require(amountWon >= 0) { "고정지출 금액은 0원 이상이어야 해요." }
    require(withdrawalDay in fixedExpenseWithdrawalDayOptions) { "출금일은 1일부터 31일 사이여야 해요." }
    require(cleanAccountName.isNotBlank()) { "출금 계좌를 입력해 주세요." }
    return copy(name = cleanName, accountName = cleanAccountName)
}

enum class MonthlyPlanItemType(val label: String) {
    INCOME("수입"),
    BUDGET("예산")
}

data class MonthlyPlanItem(
    val id: Long = 0,
    val label: String,
    val amountWon: Long,
    val type: MonthlyPlanItemType,
    val category: Category? = null,
    val customCategoryId: Long? = null,
    val customCategoryName: String? = null
)

data class CategoryBudgetUsage(
    val plan: MonthlyPlanItem,
    val spentWon: Long,
    val remainingWon: Long,
    val usedRatio: Float
)

fun buildCategoryBudgetUsages(
    plans: List<MonthlyPlanItem>,
    transactions: List<MoneyTransaction>
): List<CategoryBudgetUsage> {
    val budgets = plans.filter { it.type == MonthlyPlanItemType.BUDGET }
    return budgets
    .map { plan ->
        val spentWon = transactions
            .filter { it.countsAsPlannedUse() && plan.matchesBudget(it, budgets) }
            .sumOf { it.effectiveExpenseWon() }
        CategoryBudgetUsage(
            plan = plan,
            spentWon = spentWon,
            remainingWon = plan.amountWon - spentWon,
            usedRatio = ratioOf(spentWon, plan.amountWon)
        )
    }
}

fun calculateUnbudgetedExpenseWon(
    plans: List<MonthlyPlanItem>,
    transactions: List<MoneyTransaction>
): Long {
    val budgets = plans.filter { it.type == MonthlyPlanItemType.BUDGET }
    return transactions
        .filter { transaction ->
            transaction.fixedExpensePlanId == null &&
                transaction.countsAsActualExpense() &&
                budgets.none { it.matchesBudget(transaction, budgets) }
        }
        .sumOf { it.effectiveExpenseWon() }
}

private fun MonthlyPlanItem.matchesBudget(
    transaction: MoneyTransaction,
    budgets: List<MonthlyPlanItem>
): Boolean {
    if (transaction.fixedExpensePlanId != null) return false
    val explicitBudget = transaction.budgetPlanId
        ?.takeIf { it > 0 }
        ?.let { id -> budgets.firstOrNull { it.id == id } }
    return explicitBudget?.id == id || explicitBudget == null && matchesBudgetCategory(transaction)
}

private fun MonthlyPlanItem.matchesBudgetCategory(transaction: MoneyTransaction): Boolean =
    when {
        customCategoryId != null -> transaction.customCategoryId == customCategoryId
        category != null -> transaction.category == category && transaction.customCategoryId == null
        else -> false
    }

data class AssetOverview(
    val totalAssetsWon: Long,
    val totalFixedExpenseWon: Long,
    val totalIncomeWon: Long,
    val totalBudgetWon: Long,
    val plannedRemainingWon: Long,
    val fixedExpenseRatio: Float,
    val budgetRatio: Float,
    val spentThisMonthWon: Long,
    val budgetUsedRatio: Float
)

fun buildAssetOverview(
    accounts: List<AssetAccount>,
    fixedExpenses: List<FixedExpensePlan>,
    monthlyPlanItems: List<MonthlyPlanItem>,
    spentThisMonthWon: Long = 0L
): AssetOverview {
    val totalIncomeWon = monthlyPlanItems
        .filter { it.type == MonthlyPlanItemType.INCOME }
        .sumOf { it.amountWon }
    val totalBudgetWon = monthlyPlanItems
        .filter { it.type == MonthlyPlanItemType.BUDGET }
        .sumOf { it.amountWon }
    val totalFixedExpenseWon = fixedExpenses
        .filter { it.active }
        .sumOf { it.amountWon }

    return AssetOverview(
        totalAssetsWon = accounts.sumOf { it.balanceWon },
        totalFixedExpenseWon = totalFixedExpenseWon,
        totalIncomeWon = totalIncomeWon,
        totalBudgetWon = totalBudgetWon,
        plannedRemainingWon = totalIncomeWon - totalFixedExpenseWon - totalBudgetWon,
        fixedExpenseRatio = ratioOf(totalFixedExpenseWon, totalIncomeWon),
        budgetRatio = ratioOf(totalBudgetWon, totalIncomeWon),
        spentThisMonthWon = spentThisMonthWon,
        budgetUsedRatio = ratioOf(spentThisMonthWon, totalBudgetWon)
    )
}

private fun ratioOf(amount: Long, total: Long): Float =
    if (total <= 0) 0f else (amount.toDouble() / total).coerceIn(0.0, 1.0).toFloat()
