package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction

class RecordWalletTopupUsageUseCase(
    private val repository: MoneyRepository,
    private val reviewService: WalletTopupReviewService = WalletTopupReviewService()
) {
    suspend fun recordUsage(
        topup: MoneyTransaction,
        usedAmount: MoneyAmount,
        category: Category,
        merchant: String,
        memo: String?
    ): WalletTopupUsageResult {
        val result = reviewService.recordUsage(
            topup = topup,
            usedAmount = usedAmount,
            category = category,
            merchant = merchant,
            memo = memo
        )

        val reviewedTopup = persistReviewedTopup(result.reviewedTopup)
        val walletSpend = result.walletSpend?.let { spend ->
            val id = repository.saveTransaction(spend)
            spend.copy(id = id)
        }

        return result.copy(
            reviewedTopup = reviewedTopup,
            walletSpend = walletSpend
        )
    }

    private suspend fun persistReviewedTopup(topup: MoneyTransaction): MoneyTransaction {
        if (topup.id == 0L) {
            val id = repository.saveTransaction(topup)
            return topup.copy(id = id)
        }

        repository.updateTransaction(topup)
        return topup
    }
}
