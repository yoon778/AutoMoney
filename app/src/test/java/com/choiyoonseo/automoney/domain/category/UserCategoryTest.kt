package com.choiyoonseo.automoney.domain.category

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UserCategoryTest {
    @Test
    fun createTrimsNameAndBuildsStableNormalizedName() {
        val category = UserCategory.create(
            id = 7,
            kind = UserCategoryKind.EXPENSE,
            name = "  반려동물  "
        )

        assertThat(category.id).isEqualTo(7)
        assertThat(category.name).isEqualTo("반려동물")
        assertThat(category.normalizedName).isEqualTo("반려동물")
        assertThat(category.active).isTrue()
    }

    @Test
    fun createRejectsBlankName() {
        val error = runCatching {
            UserCategory.create(kind = UserCategoryKind.INCOME, name = "  ")
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }
}
