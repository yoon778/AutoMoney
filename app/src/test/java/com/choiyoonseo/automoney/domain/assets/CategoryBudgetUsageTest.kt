package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import org.junit.Test

class CategoryBudgetUsageTest {
    @Test
    fun tracksOnlyMatchingCategoryExpenses() {
        val usages = buildCategoryBudgetUsages(
            plans = listOf(
                MonthlyPlanItem(
                    label = "식비",
                    amountWon = 400_000,
                    type = MonthlyPlanItemType.BUDGET,
                    category = Category.FOOD
                )
            ),
            transactions = listOf(
                transaction(120_000, Category.FOOD),
                transaction(30_000, Category.HOBBY),
                transaction(50_000, Category.FOOD, TransactionType.SAVING),
                transaction(10_000, null)
            )
        )

        assertThat(usages.single().spentWon).isEqualTo(120_000)
        assertThat(usages.single().remainingWon).isEqualTo(280_000)
        assertThat(usages.single().usedRatio).isEqualTo(0.3f)
    }

    @Test
    fun tracksCustomCategoryByIdAndClampsOverBudgetRatio() {
        val usages = buildCategoryBudgetUsages(
            plans = listOf(
                MonthlyPlanItem(
                    label = "데이트비용",
                    amountWon = 100_000,
                    type = MonthlyPlanItemType.BUDGET,
                    category = Category.OTHER,
                    customCategoryId = 7,
                    customCategoryName = "데이트비용"
                )
            ),
            transactions = listOf(
                transaction(120_000, Category.OTHER, customCategoryId = 7),
                transaction(30_000, Category.OTHER, customCategoryId = 8)
            )
        )

        assertThat(usages.single().spentWon).isEqualTo(120_000)
        assertThat(usages.single().remainingWon).isEqualTo(-20_000)
        assertThat(usages.single().usedRatio).isEqualTo(1f)
    }

    @Test
    fun autoClassifiedNotificationExpenseCountsImmediately() {
        val usages = buildCategoryBudgetUsages(
            plans = listOf(foodBudget),
            transactions = listOf(
                transaction(
                    80_000,
                    Category.FOOD,
                    sourceType = SourceType.NOTIFICATION,
                    status = TransactionStatus.AUTO_CONFIRMED
                )
            )
        )

        assertThat(usages.single().spentWon).isEqualTo(80_000)
    }

    @Test
    fun recategorizedTransactionMovesBetweenBudgets() {
        val hobbyBudget = MonthlyPlanItem(
            label = "여가",
            amountWon = 200_000,
            type = MonthlyPlanItemType.BUDGET,
            category = Category.HOBBY
        )
        val original = transaction(50_000, Category.FOOD)
        val recategorized = original.copy(category = Category.HOBBY)

        val after = buildCategoryBudgetUsages(
            plans = listOf(foodBudget, hobbyBudget),
            transactions = listOf(recategorized)
        )

        assertThat(after.first { it.plan.category == Category.FOOD }.spentWon).isEqualTo(0)
        assertThat(after.first { it.plan.category == Category.HOBBY }.spentWon).isEqualTo(50_000)
    }

    @Test
    fun customCategoryChangeMovesUsageToMatchingBudget() {
        val dateBudget = MonthlyPlanItem(
            label = "데이트비용",
            amountWon = 100_000,
            type = MonthlyPlanItemType.BUDGET,
            category = Category.OTHER,
            customCategoryId = 7,
            customCategoryName = "데이트비용"
        )
        val moved = transaction(40_000, Category.FOOD)
            .copy(category = Category.OTHER, customCategoryId = 7)

        val usages = buildCategoryBudgetUsages(
            plans = listOf(foodBudget, dateBudget),
            transactions = listOf(moved)
        )

        assertThat(usages.first { it.plan.customCategoryId == 7L }.spentWon).isEqualTo(40_000)
        assertThat(usages.first { it.plan.category == Category.FOOD }.spentWon).isEqualTo(0)
    }

    @Test
    fun explicitBudgetOverridesCategoryMatch() {
        val hobbyBudget = MonthlyPlanItem(
            id = 2,
            label = "여가",
            amountWon = 200_000,
            type = MonthlyPlanItemType.BUDGET,
            category = Category.HOBBY
        )

        val usages = buildCategoryBudgetUsages(
            plans = listOf(foodBudget.copy(id = 1), hobbyBudget),
            transactions = listOf(transaction(50_000, Category.FOOD).copy(budgetPlanId = 2))
        )

        assertThat(usages.first { it.plan.id == 1L }.spentWon).isEqualTo(0)
        assertThat(usages.first { it.plan.id == 2L }.spentWon).isEqualTo(50_000)
    }

    @Test
    fun deletedBudgetOverrideFallsBackToCategoryMatch() {
        val usages = buildCategoryBudgetUsages(
            plans = listOf(foodBudget.copy(id = 1)),
            transactions = listOf(transaction(50_000, Category.FOOD).copy(budgetPlanId = 999))
        )

        assertThat(usages.single().spentWon).isEqualTo(50_000)
    }

    @Test
    fun sumsActualExpensesOutsideEveryBudget() {
        val outsideWon = calculateUnbudgetedExpenseWon(
            plans = listOf(foodBudget.copy(id = 1)),
            transactions = listOf(
                transaction(30_000, Category.HOBBY),
                transaction(20_000, null),
                transaction(10_000, Category.HOBBY, TransactionType.TRANSFER),
                transaction(40_000, Category.FOOD).copy(budgetPlanId = 999)
            )
        )

        assertThat(outsideWon).isEqualTo(50_000)
    }

    private val foodBudget = MonthlyPlanItem(
        label = "식비",
        amountWon = 400_000,
        type = MonthlyPlanItemType.BUDGET,
        category = Category.FOOD
    )

    private fun transaction(
        amountWon: Long,
        category: Category?,
        type: TransactionType = TransactionType.EXPENSE,
        customCategoryId: Long? = null,
        sourceType: SourceType = SourceType.MANUAL,
        status: TransactionStatus = TransactionStatus.USER_EDITED
    ) = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-12T00:00:00Z"),
        amount = MoneyAmount(amountWon),
        direction = type.defaultDirection,
        type = type,
        category = category,
        paymentMethod = null,
        merchant = null,
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = sourceType,
        sourceNotificationHash = null,
        status = status,
        confidence = 1.0,
        monthKey = YearMonth.of(2026, 7),
        customCategoryId = customCategoryId
    )
}
