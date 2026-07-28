package com.choiyoonseo.automoney.ui.transactions

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.ui.settings.defaultEnabledExpenseCategories
import com.choiyoonseo.automoney.ui.settings.defaultEnabledIncomeCategories

data class ManualCategoryOption(
    val category: Category,
    val label: String = category.displayName
)

val manualExpenseCategoryOptions = defaultEnabledExpenseCategories.map(::ManualCategoryOption)

val defaultManualCategoryOption = manualExpenseCategoryOptions.last()

val manualIncomeCategoryOptions = defaultEnabledIncomeCategories.map(::ManualCategoryOption)

val defaultManualIncomeCategoryOption = manualIncomeCategoryOptions.first()
