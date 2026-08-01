package com.choiyoonseo.automoney.ui.assets

import com.choiyoonseo.automoney.domain.assets.CategoryBudgetUsage
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItemType
import com.choiyoonseo.automoney.domain.model.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanCategoryOptionsTest {

    @Test
    fun `editing fixed expense preserves database id and account link`() {
        val updated = fixedExpensePlanForSave(
            existing = FixedExpensePlan(
                id = 9,
                name = "기존 통신비",
                amountWon = 70_000,
                withdrawalDay = 15,
                accountName = "국민은행",
                accountId = 3,
                active = true
            ),
            name = "통신비",
            amountWon = 80_000,
            withdrawalDay = 20,
            accountName = "국민은행"
        )

        assertThat(updated.id).isEqualTo(9)
        assertThat(updated.accountId).isEqualTo(3)
        assertThat(updated.active).isTrue()
        assertThat(updated.name).isEqualTo("통신비")
        assertThat(updated.amountWon).isEqualTo(80_000)
        assertThat(updated.withdrawalDay).isEqualTo(20)
        assertThat(updated.accountName).isEqualTo("국민은행")
    }

    @Test
    fun `plan pool offers investment and keeps 기타 last`() {
        assertThat(planCategoryPool).contains(Category.STOCK)
        assertThat(planCategoryPool.last()).isEqualTo(Category.OTHER)
    }

    @Test
    fun `stock category is labeled 투자`() {
        assertThat(planCategoryLabel(Category.STOCK)).isEqualTo("투자")
        assertThat(planCategoryLabel(Category.FOOD)).isEqualTo(Category.FOOD.displayName)
    }

    @Test
    fun `investment plan is separated from living budgets`() {
        assertThat(usage(Category.STOCK).isInvestmentPlan()).isTrue()
        assertThat(usage(Category.FOOD).isInvestmentPlan()).isFalse()
        assertThat(usage(Category.STOCK, customCategoryId = 7L).isInvestmentPlan()).isFalse()
    }

    @Test
    fun `editing plan preserves database id`() {
        val updated = monthlyPlanItemForSave(
            existing = MonthlyPlanItem(
                id = 7,
                label = "기존 식비",
                amountWon = 300_000,
                type = MonthlyPlanItemType.BUDGET,
                category = Category.FOOD
            ),
            label = "식비",
            amountWon = 400_000,
            type = MonthlyPlanItemType.BUDGET,
            builtInCategory = Category.FOOD,
            userCategory = null
        )

        assertThat(updated.id).isEqualTo(7)
        assertThat(updated.label).isEqualTo("식비")
        assertThat(updated.amountWon).isEqualTo(400_000)
        assertThat(updated.category).isEqualTo(Category.FOOD)
    }

    private fun usage(category: Category, customCategoryId: Long? = null) = CategoryBudgetUsage(
        plan = MonthlyPlanItem(
            label = "계획",
            amountWon = 100_000,
            type = MonthlyPlanItemType.BUDGET,
            category = category,
            customCategoryId = customCategoryId
        ),
        spentWon = 0,
        remainingWon = 100_000,
        usedRatio = 0f
    )
}
