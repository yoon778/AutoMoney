package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType

data class WalletTopupUsageResult(
    val reviewedTopup: MoneyTransaction,
    val walletSpend: MoneyTransaction?,
    val remainingAmount: MoneyAmount
)

class WalletTopupReviewService {
    fun recordUsage(
        topup: MoneyTransaction,
        usedAmount: MoneyAmount,
        category: Category,
        merchant: String,
        memo: String?
    ): WalletTopupUsageResult {
        require(topup.type == TransactionType.WALLET_TOPUP) {
            "topup transaction must be WALLET_TOPUP"
        }
        require(usedAmount.won <= topup.amount.won) {
            "used amount cannot exceed topup amount"
        }

        val reviewedTopup = topup.copy(
            status = TransactionStatus.USER_EDITED,
            confidence = 1.0,
            memo = buildTopupMemo(topup, usedAmount, memo)
        )

        val walletSpend = if (usedAmount.won == 0L) {
            null
        } else {
            MoneyTransaction(
                occurredAt = topup.occurredAt,
                amount = usedAmount,
                direction = TransactionDirection.EXPENSE,
                type = TransactionType.WALLET_SPEND,
                category = category,
                paymentMethod = topup.merchant ?: topup.paymentMethod,
                merchant = merchant,
                counterparty = null,
                memo = memo,
                sourceApp = topup.sourceApp,
                sourceType = SourceType.MANUAL,
                sourceNotificationHash = null,
                status = TransactionStatus.USER_EDITED,
                confidence = 1.0,
                monthKey = topup.monthKey
            )
        }

        return WalletTopupUsageResult(
            reviewedTopup = reviewedTopup,
            walletSpend = walletSpend,
            remainingAmount = MoneyAmount(topup.amount.won - usedAmount.won)
        )
    }

    private fun buildTopupMemo(topup: MoneyTransaction, usedAmount: MoneyAmount, memo: String?): String {
        val walletName = topup.merchant ?: "선불/포인트"
        val summary = "$walletName 충전 검토: 사용 ${usedAmount.won}원, 잔액 ${topup.amount.won - usedAmount.won}원"
        val cleanMemo = memo?.trim()?.takeIf { it.isNotBlank() }
        return listOfNotNull(summary, cleanMemo).joinToString(" · ")
    }
}
