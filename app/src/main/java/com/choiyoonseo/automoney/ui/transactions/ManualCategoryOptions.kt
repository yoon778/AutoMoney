package com.choiyoonseo.automoney.ui.transactions

import com.choiyoonseo.automoney.domain.model.Category

data class ManualCategoryOption(
    val category: Category,
    val label: String = category.displayName
)

val manualExpenseCategoryOptions = listOf(
    ManualCategoryOption(Category.FOOD),
    ManualCategoryOption(Category.CAFE_SNACK),
    ManualCategoryOption(Category.TRANSPORT),
    ManualCategoryOption(Category.SHOPPING),
    ManualCategoryOption(Category.LIVING),
    ManualCategoryOption(Category.OTHER)
)

val defaultManualCategoryOption = manualExpenseCategoryOptions.last()

val manualIncomeCategoryOptions = listOf(
    ManualCategoryOption(Category.SALARY),
    ManualCategoryOption(Category.ALLOWANCE),
    ManualCategoryOption(Category.INVESTMENT_RETURN),
    ManualCategoryOption(Category.REIMBURSEMENT),
    ManualCategoryOption(Category.OTHER)
)

val defaultManualIncomeCategoryOption = manualIncomeCategoryOptions.first()
