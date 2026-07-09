package com.choiyoonseo.automoney.domain.assets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssetOverviewCalculatorTest {
    @Test
    fun assetOverviewAddsAccountsFixedExpensesAndMonthlyPlans() {
        val overview = buildAssetOverview(
            accounts = listOf(
                AssetAccount(name = "국민은행", balanceWon = 3_799_195),
                AssetAccount(name = "카카오뱅크", balanceWon = 110_067)
            ),
            fixedExpenses = listOf(
                FixedExpensePlan(name = "통신비", amountWon = 70_000, withdrawalDay = 15, accountName = "국민은행"),
                FixedExpensePlan(name = "넷플릭스", amountWon = 17_000, withdrawalDay = 20, accountName = "카카오뱅크")
            ),
            monthlyPlanItems = listOf(
                MonthlyPlanItem(label = "월급", amountWon = 2_500_000, type = MonthlyPlanItemType.INCOME),
                MonthlyPlanItem(label = "식비", amountWon = 450_000, type = MonthlyPlanItemType.BUDGET),
                MonthlyPlanItem(label = "교통비", amountWon = 80_000, type = MonthlyPlanItemType.BUDGET)
            )
        )

        assertThat(overview.totalAssetsWon).isEqualTo(3_909_262)
        assertThat(overview.totalFixedExpenseWon).isEqualTo(87_000)
        assertThat(overview.totalIncomeWon).isEqualTo(2_500_000)
        assertThat(overview.totalBudgetWon).isEqualTo(530_000)
        assertThat(overview.plannedRemainingWon).isEqualTo(1_883_000)
    }

    @Test
    fun assetOverviewIgnoresInactiveFixedExpenses() {
        val overview = buildAssetOverview(
            accounts = emptyList(),
            fixedExpenses = listOf(
                FixedExpensePlan(name = "끝난 구독", amountWon = 12_000, withdrawalDay = 1, accountName = "국민은행", active = false)
            ),
            monthlyPlanItems = emptyList()
        )

        assertThat(overview.totalFixedExpenseWon).isEqualTo(0)
    }

    @Test
    fun fixedExpenseWithdrawalDayOptionsCoverEveryMonthlyDay() {
        assertThat(fixedExpenseWithdrawalDayOptions.toList())
            .containsExactlyElementsIn((1..31).toList())
            .inOrder()
    }

    @Test
    fun fixedExpensePlanValidationRejectsInvalidWithdrawalDay() {
        val error = runCatching {
            FixedExpensePlan(name = "통신비", amountWon = 70_000, withdrawalDay = 0, accountName = "국민은행")
                .validatedForSave()
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains("출금일")
    }

    @Test
    fun assetOverviewBalanceHelperDoesNotClaimPreviousMonthComparison() {
        assertThat(assetOverviewBalanceHelper(accountCount = 2))
            .isEqualTo("2개 계좌 · 현재 등록 잔액 기준")
    }
}
