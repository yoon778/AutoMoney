package com.choiyoonseo.automoney.domain.refund

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.temporal.ChronoUnit

class LinkRefundUseCase(
    private val repository: MoneyRepository,
    private val matcher: RefundLinkMatcher = RefundLinkMatcher()
) {
    suspend fun autoLink(refundId: Long): RefundMatchDecision {
        val context = loadContext(refundId) ?: return RefundMatchDecision.NoMatch
        val decision = matcher.match(
            refund = context.refund,
            candidates = context.window,
            linkedRefunds = context.window
        )
        if (decision is RefundMatchDecision.Match) {
            repository.linkRefundAndResolve(
                refundId = refundId,
                paymentId = decision.paymentId,
                userConfirmed = false
            )
        }
        return decision
    }

    suspend fun candidates(refundId: Long): List<MoneyTransaction> {
        val context = loadContext(refundId) ?: return emptyList()
        return matcher.eligibleCandidates(
            refund = context.refund,
            candidates = context.window,
            linkedRefunds = context.window
        )
    }

    suspend fun linkConfirmed(refundId: Long, paymentId: Long) {
        repository.linkRefundAndResolve(
            refundId = refundId,
            paymentId = paymentId,
            userConfirmed = true
        )
    }

    private suspend fun loadContext(refundId: Long): RefundContext? {
        val refund = repository.findTransaction(refundId)
            ?.takeIf { it.type == TransactionType.REFUND }
            ?: return null
        val sourceApp = refund.sourceApp ?: return null
        val window = repository.refundMatchWindow(
            sourceApp = sourceApp,
            from = refund.occurredAt.minus(30, ChronoUnit.DAYS),
            to = refund.occurredAt
        )
        return RefundContext(refund, window)
    }
}

private data class RefundContext(
    val refund: MoneyTransaction,
    val window: List<MoneyTransaction>
)
