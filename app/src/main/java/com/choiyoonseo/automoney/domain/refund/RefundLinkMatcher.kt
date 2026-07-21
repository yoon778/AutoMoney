package com.choiyoonseo.automoney.domain.refund

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.report.isReportableTransaction
import java.time.Duration
import java.time.temporal.ChronoUnit

sealed interface RefundMatchDecision {
    data class Match(val paymentId: Long) : RefundMatchDecision
    data object NoMatch : RefundMatchDecision
    data object Ambiguous : RefundMatchDecision
}

class RefundLinkMatcher {
    fun eligibleCandidates(
        refund: MoneyTransaction,
        candidates: List<MoneyTransaction>,
        linkedRefunds: List<MoneyTransaction>
    ): List<MoneyTransaction> {
        require(refund.type == TransactionType.REFUND) { "Refund transaction required" }
        val refundedByPayment = linkedRefunds
            .filter { it.type == TransactionType.REFUND && it.isReportableTransaction() }
            .mapNotNull { linkedRefund ->
                linkedRefund.refundParentTransactionId?.let { parentId ->
                    parentId to linkedRefund.amount.won
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }

        return candidates.filter { payment ->
            val remainingWon = payment.amount.won - (refundedByPayment[payment.id] ?: 0L)
            payment.id > 0 &&
                payment.type in refundableTypes &&
                payment.isReportableTransaction() &&
                payment.sourceApp == refund.sourceApp &&
                !payment.occurredAt.isAfter(refund.occurredAt) &&
                payment.occurredAt >= refund.occurredAt.minus(30, ChronoUnit.DAYS) &&
                refund.amount.won <= remainingWon
        }
    }

    fun match(
        refund: MoneyTransaction,
        candidates: List<MoneyTransaction>,
        linkedRefunds: List<MoneyTransaction>
    ): RefundMatchDecision {
        val eligible = eligibleCandidates(refund, candidates, linkedRefunds)
        val merchantMatches = eligible
            .filter { normalizeMerchant(it.merchant) == normalizeMerchant(refund.merchant) }
            .takeIf { refund.merchant?.isNotBlank() == true && it.isNotEmpty() }
        val immediateMatches = eligible.filter { payment ->
            Duration.between(payment.occurredAt, refund.occurredAt) <= immediateCashbackWindow
        }.takeIf { it.size == 1 }
        val narrowed = merchantMatches ?: immediateMatches ?: eligible

        return when (narrowed.size) {
            0 -> RefundMatchDecision.NoMatch
            1 -> RefundMatchDecision.Match(narrowed.single().id)
            else -> RefundMatchDecision.Ambiguous
        }
    }
}

private val refundableTypes = setOf(
    TransactionType.EXPENSE,
    TransactionType.FIXED_EXPENSE,
    TransactionType.WALLET_SPEND,
    TransactionType.SAVING,
    TransactionType.INVESTMENT
)
private val immediateCashbackWindow: Duration = Duration.ofMinutes(2)
private val merchantNoise = Regex("[^가-힣a-z0-9]")

private fun normalizeMerchant(value: String?): String =
    value.orEmpty().lowercase().replace(merchantNoise, "")
