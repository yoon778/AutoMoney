package com.choiyoonseo.automoney.ui.assets

import com.choiyoonseo.automoney.domain.assets.CategoryBudgetUsage
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItemType
import com.choiyoonseo.automoney.domain.model.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanCategoryOptionsTest {

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
