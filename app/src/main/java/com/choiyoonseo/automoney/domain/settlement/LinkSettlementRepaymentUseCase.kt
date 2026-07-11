package com.choiyoonseo.automoney.domain.settlement

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus

class LinkSettlementRepaymentUseCase(
    private val repository: MoneyRepository
) {
    suspend fun link(
        reviewItemId: Long,
        repaymentTransaction: MoneyTransaction,
        settlementTransactionId: Long
    ): MoneyTransaction {
        require(reviewItemId > 0) { "Review item id must be positive" }
        require(settlementTransactionId > 0) { "Settlement transaction id must be positive" }
        require(repaymentTransaction.direction == TransactionDirection.INCOME) {
            "Settlement repayment must be an income transaction"
        }
        require(repaymentTransaction.settlementParentId == null) {
            "Settlement repayment is already linked"
        }

        val linked = repaymentTransaction.copy(
            settlementParentId = settlementTransactionId,
            status = TransactionStatus.USER_EDITED
        )
        repository.resolveReviewItemWithTransaction(reviewItemId, linked)
        return linked
    }
}
