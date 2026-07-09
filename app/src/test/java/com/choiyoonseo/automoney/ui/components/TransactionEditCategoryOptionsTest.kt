package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionEditCategoryOptionsTest {
    @Test
    fun transactionEditExpenseCategoryOptionsExposeExpenseCategoriesOnly() {
        assertThat(transactionEditCategoryOptionsFor(TransactionType.EXPENSE).map { it.category })
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
    fun transactionEditIncomeCategoryOptionsExposeIncomeCategoriesOnly() {
        assertThat(transactionEditCategoryOptionsFor(TransactionType.INCOME).map { it.category })
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
    fun transactionEditNeutralCategoryOptionsAreEmpty() {
        assertThat(transactionEditCategoryOptionsFor(TransactionType.TRANSFER)).isEmpty()
        assertThat(transactionEditCategoryOptionsFor(TransactionType.EXCLUDED)).isEmpty()
    }

    @Test
    fun categoryLabelForEditUsesCategoryDisplayNameOrOther() {
        assertThat(categoryLabelForEdit(Category.CAFE_SNACK)).isEqualTo("카페/간식")
        assertThat(categoryLabelForEdit(null)).isEqualTo("기타")
    }

    @Test
    fun defaultCategoryLabelForEditKeepsValidCurrentCategory() {
        assertThat(defaultCategoryLabelForEdit(TransactionType.INCOME, Category.SALARY))
            .isEqualTo("월급")
        assertThat(defaultCategoryLabelForEdit(TransactionType.INCOME, Category.FOOD))
            .isEqualTo("월급")
    }
}
