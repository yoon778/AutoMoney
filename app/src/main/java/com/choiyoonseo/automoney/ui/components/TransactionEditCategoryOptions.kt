package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.TransactionType

data class TransactionEditCategoryOption(
    val category: Category,
    val label: String = category.displayName
)

val transactionEditExpenseCategoryOptions: List<TransactionEditCategoryOption> = listOf(
    TransactionEditCategoryOption(Category.FOOD),
    TransactionEditCategoryOption(Category.CAFE_SNACK),
    TransactionEditCategoryOption(Category.TRANSPORT),
    TransactionEditCategoryOption(Category.SHOPPING),
    TransactionEditCategoryOption(Category.LIVING),
    TransactionEditCategoryOption(Category.OTHER)
)

val transactionEditIncomeCategoryOptions: List<TransactionEditCategoryOption> = listOf(
    TransactionEditCategoryOption(Category.SALARY),
    TransactionEditCategoryOption(Category.ALLOWANCE),
    TransactionEditCategoryOption(Category.INVESTMENT_RETURN),
    TransactionEditCategoryOption(Category.REIMBURSEMENT),
    TransactionEditCategoryOption(Category.OTHER)
)

fun transactionEditCategoryOptionsFor(
    type: TransactionType,
    enabledExpense: List<Category>? = null,
    enabledIncome: List<Category>? = null
): List<TransactionEditCategoryOption> =
    when (type) {
        TransactionType.INCOME ->
            enabledIncome?.map { TransactionEditCategoryOption(it) } ?: transactionEditIncomeCategoryOptions
        TransactionType.EXPENSE,
        TransactionType.FIXED_EXPENSE,
        TransactionType.WALLET_SPEND ->
            enabledExpense?.map { TransactionEditCategoryOption(it) } ?: transactionEditExpenseCategoryOptions
        else -> emptyList()
    }

fun categoryLabelForEdit(category: Category?): String =
    category?.displayName ?: Category.OTHER.displayName

fun defaultCategoryLabelForEdit(
    type: TransactionType,
    category: Category?,
    enabledExpense: List<Category>? = null,
    enabledIncome: List<Category>? = null
): String {
    val options = transactionEditCategoryOptionsFor(type, enabledExpense, enabledIncome)
    val currentLabel = categoryLabelForEdit(category)
    return options.firstOrNull { it.label == currentLabel }?.label
        ?: options.firstOrNull()?.label
        ?: Category.OTHER.displayName
}

fun isCategoryLabelValidForEdit(
    type: TransactionType,
    label: String,
    enabledExpense: List<Category>? = null,
    enabledIncome: List<Category>? = null
): Boolean =
    transactionEditCategoryOptionsFor(type, enabledExpense, enabledIncome).any { it.label == label }
