package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.ui.settings.defaultEnabledExpenseCategories
import com.choiyoonseo.automoney.ui.settings.defaultEnabledIncomeCategories

data class TransactionEditCategoryOption(
    val category: Category,
    val label: String = category.displayName
)

val transactionEditExpenseCategoryOptions: List<TransactionEditCategoryOption> =
    defaultEnabledExpenseCategories.map(::TransactionEditCategoryOption)

val transactionEditIncomeCategoryOptions: List<TransactionEditCategoryOption> =
    defaultEnabledIncomeCategories.map(::TransactionEditCategoryOption)

fun categoryLabelForEdit(category: Category?): String =
    category?.displayName ?: Category.OTHER.displayName

fun categoryLabelsFor(
    type: TransactionType,
    expenseLabels: List<String>,
    incomeLabels: List<String>
): List<String> = when (type) {
    TransactionType.INCOME -> incomeLabels
    TransactionType.EXPENSE,
    TransactionType.SPECIAL_EXPENSE,
    TransactionType.FIXED_EXPENSE,
    TransactionType.WALLET_SPEND -> expenseLabels
    else -> emptyList()
}

fun defaultCategoryLabelFor(
    type: TransactionType,
    category: Category?,
    customCategoryName: String?,
    expenseLabels: List<String>,
    incomeLabels: List<String>
): String {
    val options = categoryLabelsFor(type, expenseLabels, incomeLabels)
    val current = customCategoryName ?: categoryLabelForEdit(category)
    return options.firstOrNull { it == current }
        ?: options.firstOrNull()
        ?: Category.OTHER.displayName
}
