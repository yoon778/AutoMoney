package com.choiyoonseo.automoney.ui.settings

import com.choiyoonseo.automoney.domain.model.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryPreferenceStoreTest {
    @Test
    fun nullStoredNamesReturnDefaults() {
        assertThat(resolveEnabledCategories(null, expenseCategoryPool, defaultEnabledExpenseCategories))
            .isEqualTo(defaultEnabledExpenseCategories)
    }

    @Test
    fun storedNamesFilterPoolInPoolOrder() {
        val enabled = resolveEnabledCategories(
            setOf(Category.HOBBY.name, Category.FOOD.name),
            expenseCategoryPool,
            defaultEnabledExpenseCategories
        )
        assertThat(enabled).containsExactly(Category.FOOD, Category.HOBBY, Category.OTHER).inOrder()
    }

    @Test
    fun otherIsAlwaysIncluded() {
        val enabled = resolveEnabledCategories(
            setOf(Category.SALARY.name),
            incomeCategoryPool,
            defaultEnabledIncomeCategories
        )
        assertThat(enabled).contains(Category.OTHER)
    }

    @Test
    fun emptySelectionFallsBackToDefaults() {
        val enabled = resolveEnabledCategories(
            emptySet(),
            incomeCategoryPool,
            defaultEnabledIncomeCategories
        )
        assertThat(enabled).isEqualTo(defaultEnabledIncomeCategories)
    }
}
