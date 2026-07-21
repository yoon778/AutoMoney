package com.choiyoonseo.automoney.ui.assets

import com.choiyoonseo.automoney.domain.assets.CategoryBudgetUsage
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.ui.settings.expenseCategoryPool

/** 월계획에서 고를 수 있는 분류. 생활비 분류에 투자 계획(Category.STOCK)을 더한다. */
val planCategoryPool: List<Category> =
    expenseCategoryPool.filterNot { it == Category.OTHER } + Category.STOCK + Category.OTHER

/** 사용자에게는 "주식"이 아니라 "투자"로 보여준다. */
fun planCategoryLabel(category: Category): String =
    if (category == Category.STOCK) INVESTMENT_PLAN_LABEL else category.displayName

/** 투자 계획은 생활비 예산과 섞지 않고 따로 보여준다. */
fun CategoryBudgetUsage.isInvestmentPlan(): Boolean =
    plan.category == Category.STOCK && plan.customCategoryId == null

const val INVESTMENT_PLAN_LABEL = "투자"
