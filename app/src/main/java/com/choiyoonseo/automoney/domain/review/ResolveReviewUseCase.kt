package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction

const val MIN_SETTLEMENT_PARTY_COUNT = 2
const val MAX_SETTLEMENT_PARTY_COUNT = 20

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
            require(settlementPartyCount in MIN_SETTLEMENT_PARTY_COUNT..MAX_SETTLEMENT_PARTY_COUNT) {
                "Settlement party count must be between $MIN_SETTLEMENT_PARTY_COUNT and $MAX_SETTLEMENT_PARTY_COUNT"
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
        const val DEFAULT_SETTLEMENT_PARTY_COUNT = MIN_SETTLEMENT_PARTY_COUNT
    }
}
