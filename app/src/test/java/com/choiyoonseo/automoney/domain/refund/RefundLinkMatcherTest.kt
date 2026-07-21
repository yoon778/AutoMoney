package com.choiyoonseo.automoney.domain.refund

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import org.junit.Test

class RefundLinkMatcherTest {
    private val matcher = RefundLinkMatcher()

    @Test
    fun linksKbankCashbackToOnlyRecentPayment() {
        val payment = expense(id = 1, won = 6_000, at = "2026-07-21T01:00:00Z")
        val refund = refund(id = 2, won = 6, at = "2026-07-21T01:00:03Z")

        assertThat(matcher.match(refund, listOf(payment), emptyList()))
            .isEqualTo(RefundMatchDecision.Match(1))
    }

    @Test
    fun ambiguousCandidatesStayUnlinked() {
        val refund = refund(id = 3, won = 6, at = "2026-07-21T01:03:00Z")
        val candidates = listOf(
            expense(id = 1, won = 6_000, at = "2026-07-21T01:00:00Z"),
            expense(id = 2, won = 8_000, at = "2026-07-21T00:59:00Z")
        )

        assertThat(matcher.match(refund, candidates, emptyList()))
            .isEqualTo(RefundMatchDecision.Ambiguous)
    }

    @Test
    fun rejectsRefundAboveRemainingAmount() {
        val payment = expense(id = 1, won = 10_000, at = "2026-07-01T00:00:00Z")
        val prior = refund(
            id = 2,
            won = 9_000,
            at = "2026-07-02T00:00:00Z",
            parentId = 1,
            status = TransactionStatus.AUTO_CONFIRMED
        )
        val incoming = refund(id = 3, won = 2_000, at = "2026-07-03T00:00:00Z")

        assertThat(matcher.match(incoming, listOf(payment), listOf(prior)))
            .isEqualTo(RefundMatchDecision.NoMatch)
    }

    @Test
    fun prefersMatchingMerchantWhenSeveralPaymentsExist() {
        val refund = refund(
            id = 3,
            won = 3_000,
            at = "2026-07-21T01:10:00Z",
            merchant = "스타벅스"
        )
        val candidates = listOf(
            expense(id = 1, won = 6_000, at = "2026-07-21T01:00:00Z", merchant = "스타 벅스"),
            expense(id = 2, won = 8_000, at = "2026-07-21T00:59:00Z", merchant = "편의점")
        )

        assertThat(matcher.match(refund, candidates, emptyList()))
            .isEqualTo(RefundMatchDecision.Match(1))
    }

    @Test
    fun ignoresReviewRefundWhenCalculatingRemainingAmount() {
        val payment = expense(id = 1, won = 10_000, at = "2026-07-01T00:00:00Z")
        val reviewRefund = refund(
            id = 2,
            won = 9_000,
            at = "2026-07-02T00:00:00Z",
            parentId = 1,
            status = TransactionStatus.NEEDS_REVIEW
        )
        val incoming = refund(id = 3, won = 2_000, at = "2026-07-03T00:00:00Z")

        assertThat(matcher.match(incoming, listOf(payment), listOf(reviewRefund)))
            .isEqualTo(RefundMatchDecision.Match(1))
    }
}

private fun expense(
    id: Long,
    won: Long,
    at: String,
    merchant: String = "스타벅스"
): MoneyTransaction = transaction(
    id = id,
    won = won,
    at = at,
    type = TransactionType.EXPENSE,
    direction = TransactionDirection.EXPENSE,
    merchant = merchant,
    status = TransactionStatus.AUTO_CONFIRMED
)

private fun refund(
    id: Long,
    won: Long,
    at: String,
    parentId: Long? = null,
    merchant: String = "캐시백",
    status: TransactionStatus = TransactionStatus.NEEDS_REVIEW
): MoneyTransaction = transaction(
    id = id,
    won = won,
    at = at,
    type = TransactionType.REFUND,
    direction = TransactionDirection.NEUTRAL,
    merchant = merchant,
    status = status,
    refundParentTransactionId = parentId
)

private fun transaction(
    id: Long,
    won: Long,
    at: String,
    type: TransactionType,
    direction: TransactionDirection,
    merchant: String,
    status: TransactionStatus,
    refundParentTransactionId: Long? = null
): MoneyTransaction {
    val occurredAt = Instant.parse(at)
    return MoneyTransaction(
        id = id,
        occurredAt = occurredAt,
        amount = MoneyAmount(won),
        direction = direction,
        type = type,
        category = if (type == TransactionType.EXPENSE) Category.CAFE_SNACK else Category.REIMBURSEMENT,
        paymentMethod = "케이뱅크",
        merchant = merchant,
        counterparty = null,
        memo = null,
        sourceApp = "com.kbankwith.smartbank",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash-$id",
        status = status,
        confidence = 1.0,
        monthKey = YearMonth.from(occurredAt.atZone(java.time.ZoneOffset.UTC)),
        refundParentTransactionId = refundParentTransactionId
    )
}
