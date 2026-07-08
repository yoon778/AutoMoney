package com.choiyoonseo.automoney.domain.assets

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
    val kind: AssetAccountKind = AssetAccountKind.BANK
)

fun updateAssetAccount(
    account: AssetAccount,
    name: String,
    balanceWon: Long,
    kind: AssetAccountKind
): AssetAccount {
    val cleanName = name.trim()
    require(cleanName.isNotBlank()) { "계좌 이름을 입력해 주세요." }
    require(balanceWon >= 0) { "잔액은 0원 이상이어야 해요." }
    return account.copy(
        name = cleanName,
        balanceWon = balanceWon,
        kind = kind
    )
}

data class FixedExpensePlan(
    val id: Long = 0,
    val name: String,
    val amountWon: Long,
    val withdrawalDay: Int,
    val accountName: String,
    val active: Boolean = true
)

enum class MonthlyPlanItemType(val label: String) {
    INCOME("수입"),
    BUDGET("예산")
}

data class MonthlyPlanItem(
    val id: Long = 0,
    val label: String,
    val amountWon: Long,
    val type: MonthlyPlanItemType
)

data class AssetOverview(
    val totalAssetsWon: Long,
    val totalFixedExpenseWon: Long,
    val totalIncomeWon: Long,
    val totalBudgetWon: Long,
    val plannedRemainingWon: Long
)

fun buildAssetOverview(
    accounts: List<AssetAccount>,
    fixedExpenses: List<FixedExpensePlan>,
    monthlyPlanItems: List<MonthlyPlanItem>
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
        plannedRemainingWon = totalIncomeWon - totalFixedExpenseWon - totalBudgetWon
    )
}
