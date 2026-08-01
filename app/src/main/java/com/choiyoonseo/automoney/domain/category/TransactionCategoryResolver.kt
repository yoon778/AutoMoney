package com.choiyoonseo.automoney.domain.category

import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.TransactionType

data class TransactionCategoryAssignment(
    val category: Category?,
    val customCategoryId: Long? = null,
    val customCategoryName: String? = null
)

class TransactionCategoryResolver(
    private val userCategoryRepository: UserCategoryRepository?
) {
    suspend fun resolve(categoryText: String, type: TransactionType): TransactionCategoryAssignment {
        val kind = type.categoryKindOrNull() ?: return TransactionCategoryAssignment(category = null)
        val cleanText = categoryText.trim()
        val builtIn = Category.entries.firstOrNull { category ->
            category.displayName == cleanText || category.name.equals(cleanText, ignoreCase = true)
        }
        if (builtIn != null) return TransactionCategoryAssignment(category = builtIn)
        if (cleanText.isBlank() || userCategoryRepository == null) {
            return TransactionCategoryAssignment(category = Category.OTHER)
        }

        val custom = userCategoryRepository.resolveOrCreate(kind, cleanText)
        return TransactionCategoryAssignment(
            category = Category.OTHER,
            customCategoryId = custom.id,
            customCategoryName = custom.name
        )
    }
}

private fun TransactionType.categoryKindOrNull(): UserCategoryKind? = when (this) {
    TransactionType.INCOME -> UserCategoryKind.INCOME
    TransactionType.EXPENSE,
    TransactionType.SPECIAL_EXPENSE,
    TransactionType.FIXED_EXPENSE,
    TransactionType.WALLET_SPEND,
    TransactionType.SAVING,
    TransactionType.INVESTMENT -> UserCategoryKind.EXPENSE
    else -> null
}
