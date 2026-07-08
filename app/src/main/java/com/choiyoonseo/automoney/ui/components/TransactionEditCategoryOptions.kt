package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.Category

data class TransactionEditCategoryOption(
    val category: Category,
    val label: String = category.displayName
)

val transactionEditCategoryOptions: List<TransactionEditCategoryOption> =
    Category.entries.map { category -> TransactionEditCategoryOption(category) }

fun categoryLabelForEdit(category: Category?): String =
    category?.displayName ?: Category.OTHER.displayName
