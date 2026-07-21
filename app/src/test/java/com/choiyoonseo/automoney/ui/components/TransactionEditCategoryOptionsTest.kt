package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionEditCategoryOptionsTest {
    private val expenseLabels = transactionEditExpenseCategoryOptions.map { it.label }
    private val incomeLabels = transactionEditIncomeCategoryOptions.map { it.label }

    @Test
    fun fallbackExpenseOptionsExposeExpenseCategoriesOnly() {
        assertThat(transactionEditExpenseCategoryOptions.map { it.category })
            .containsExactly(
                Category.FOOD,
                Category.CAFE_SNACK,
                Category.TRANSPORT,
                Category.SHOPPING,
                Category.LIVING,
                Category.OTHER
            )
            .inOrder()
    }

    @Test
    fun fallbackIncomeOptionsExposeIncomeCategoriesOnly() {
        assertThat(transactionEditIncomeCategoryOptions.map { it.category })
            .containsExactly(
                Category.SALARY,
                Category.ALLOWANCE,
                Category.INVESTMENT_RETURN,
                Category.REIMBURSEMENT,
                Category.OTHER
            )
            .inOrder()
    }

    @Test
    fun neutralTypesHaveNoCategoryLabels() {
        assertThat(categoryLabelsFor(TransactionType.TRANSFER, expenseLabels, incomeLabels)).isEmpty()
        assertThat(categoryLabelsFor(TransactionType.EXCLUDED, expenseLabels, incomeLabels)).isEmpty()
    }

    @Test
    fun categoryLabelForEditUsesCategoryDisplayNameOrOther() {
        assertThat(categoryLabelForEdit(Category.CAFE_SNACK)).isEqualTo("카페/간식")
        assertThat(categoryLabelForEdit(null)).isEqualTo("기타")
    }

    @Test
    fun defaultCategoryLabelKeepsValidCurrentCategory() {
        assertThat(
            defaultCategoryLabelFor(TransactionType.INCOME, Category.SALARY, null, expenseLabels, incomeLabels)
        ).isEqualTo("월급")
        assertThat(
            defaultCategoryLabelFor(TransactionType.INCOME, Category.FOOD, null, expenseLabels, incomeLabels)
        ).isEqualTo("월급")
    }

    @Test
    fun defaultCategoryLabelPrefersCustomCategoryName() {
        assertThat(
            defaultCategoryLabelFor(
                TransactionType.EXPENSE,
                Category.OTHER,
                customCategoryName = "반려동물",
                expenseLabels = expenseLabels + "반려동물",
                incomeLabels = incomeLabels
            )
        ).isEqualTo("반려동물")
    }
}
