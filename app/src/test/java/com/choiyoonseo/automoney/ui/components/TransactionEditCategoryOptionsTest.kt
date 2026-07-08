package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionEditCategoryOptionsTest {
    @Test
    fun transactionEditCategoryOptionsExposeAllCategories() {
        assertThat(transactionEditCategoryOptions.map { it.category })
            .containsExactlyElementsIn(Category.entries)
            .inOrder()
    }

    @Test
    fun categoryLabelForEditUsesCategoryDisplayNameOrOther() {
        assertThat(categoryLabelForEdit(Category.CAFE_SNACK)).isEqualTo("카페/간식")
        assertThat(categoryLabelForEdit(null)).isEqualTo("기타")
    }
}
