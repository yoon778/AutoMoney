package com.choiyoonseo.automoney.domain.category

import java.util.Locale

enum class UserCategoryKind {
    EXPENSE,
    INCOME
}

@ConsistentCopyVisibility
data class UserCategory private constructor(
    val id: Long,
    val kind: UserCategoryKind,
    val name: String,
    val normalizedName: String,
    val active: Boolean
) {
    companion object {
        fun create(
            id: Long = 0,
            kind: UserCategoryKind,
            name: String,
            active: Boolean = true
        ): UserCategory {
            val cleanName = name.trim()
            require(cleanName.isNotEmpty()) { "Category name must not be blank" }
            return UserCategory(
                id = id,
                kind = kind,
                name = cleanName,
                normalizedName = cleanName.lowercase(Locale.ROOT),
                active = active
            )
        }
    }
}
