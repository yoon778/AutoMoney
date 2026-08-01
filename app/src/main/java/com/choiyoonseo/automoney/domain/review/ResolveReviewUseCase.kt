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
        userMemo: String?,
        settlementPartyCount: Int = DEFAULT_SETTLEMENT_PARTY_COUNT,
        settlementMyShareWon: Long? = null
    ): MoneyTransaction {
        val resolved = transaction.resolveReview(
            resolution = resolution,
            userMemo = userMemo
        )
        val updated = if (resolution == ReviewResolution.SETTLEMENT) {
            require(settlementPartyCount in 2..20) {
                "Settlement party count must be between 2 and 20"
            }
            val myShareWon = settlementMyShareWon
                ?: transaction.amount.won / settlementPartyCount
            require(myShareWon in 0..transaction.amount.won) {
                "Settlement share must be within the paid amount"
            }
            resolved.copy(
                settlementPartyCount = settlementPartyCount,
                settlementMyShareWon = myShareWon,
                settlementParentId = null,
                settlementTrackingHidden = false
            )
        } else {
            resolved
        }
        repository.resolveReviewItemWithTransaction(reviewItemId, updated)
        return updated
    }

    private companion object {
        const val DEFAULT_SETTLEMENT_PARTY_COUNT = 2
    }
}
