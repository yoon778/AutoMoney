package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction

class ResolveReviewUseCase(
    private val repository: MoneyRepository
) {
    suspend fun resolve(
        reviewItemId: Long,
        transaction: MoneyTransaction,
        resolution: ReviewResolution,
        userMemo: String?
    ): MoneyTransaction {
        val updated = transaction.resolveReview(
            resolution = resolution,
            userMemo = userMemo
        )
        repository.resolveReviewItemWithTransaction(reviewItemId, updated)
        return updated
    }
}
