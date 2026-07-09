package com.choiyoonseo.automoney.ui.transactions

import com.choiyoonseo.automoney.domain.model.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ManualCategoryOptionsTest {
    @Test
    fun manualExpenseCategoryOptionsExposeCommonCategoriesFirst() {
        assertThat(manualExpenseCategoryOptions.map { it.label }).containsExactly(
            "식비",
            "카페/간식",
            "교통비",
            "쇼핑",
            "생활",
            "기타"
        ).inOrder()
        assertThat(manualExpenseCategoryOptions.map { it.category }).containsExactly(
            Category.FOOD,
            Category.CAFE_SNACK,
            Category.TRANSPORT,
            Category.SHOPPING,
            Category.LIVING,
            Category.OTHER
        ).inOrder()
    }

    @Test
    fun defaultManualCategoryOptionIsOther() {
        assertThat(defaultManualCategoryOption.label).isEqualTo("기타")
        assertThat(defaultManualCategoryOption.category).isEqualTo(Category.OTHER)
    }

    @Test
    fun manualIncomeCategoryOptionsExposeCommonIncomeCategoriesFirst() {
        assertThat(manualIncomeCategoryOptions.map { it.label }).containsExactly(
            "월급",
            "용돈",
            "투자성과",
            "환급",
            "기타"
        ).inOrder()
        assertThat(manualIncomeCategoryOptions.map { it.category }).containsExactly(
            Category.SALARY,
            Category.ALLOWANCE,
            Category.INVESTMENT_RETURN,
            Category.REIMBURSEMENT,
            Category.OTHER
        ).inOrder()
    }

    @Test
    fun defaultManualIncomeCategoryOptionIsSalary() {
        assertThat(defaultManualIncomeCategoryOption.label).isEqualTo("월급")
        assertThat(defaultManualIncomeCategoryOption.category).isEqualTo(Category.SALARY)
    }
}
