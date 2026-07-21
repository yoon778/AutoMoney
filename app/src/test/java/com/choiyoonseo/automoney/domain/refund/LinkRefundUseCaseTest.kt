package com.choiyoonseo.automoney.domain.refund

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LinkRefundUseCaseTest {
    @Test
    fun autoLinkPersistsUniqueImmediatePaymentMatch() = runTest {
        val repository = RefundRepository(listOf(payment(), refund()))

        val decision = LinkRefundUseCase(repository).autoLink(refundId = 2)

        assertThat(decision).isEqualTo(RefundMatchDecision.Match(paymentId = 1))
        assertThat(repository.linkCalls).containsExactly(LinkCall(2, 1, false))
    }

    @Test
    fun candidatesReturnsOnlyEligiblePayments() = runTest {
        val repository = RefundRepository(
            listOf(payment(), refund(), payment().copy(id = 3, sourceApp = "other-bank"))
        )

        val candidates = LinkRefundUseCase(repository).candidates(refundId = 2)

        assertThat(candidates.map { it.id }).containsExactly(1L)
    }

    @Test
    fun confirmedLinkUsesUserConfirmedStatus() = runTest {
        val repository = RefundRepository(listOf(payment(), refund()))

        LinkRefundUseCase(repository).linkConfirmed(refundId = 2, paymentId = 1)

        assertThat(repository.linkCalls).containsExactly(LinkCall(2, 1, true))
    }
}

private data class LinkCall(val refundId: Long, val paymentId: Long, val userConfirmed: Boolean)

private class RefundRepository(private val rows: List<MoneyTransaction>) : MoneyRepository {
    val linkCalls = mutableListOf<LinkCall>()

    override suspend fun findTransaction(id: Long): MoneyTransaction? = rows.find { it.id == id }
    override suspend fun refundMatchWindow(
        sourceApp: String,
        from: Instant,
        to: Instant
    ): List<MoneyTransaction> = rows.filter {
        it.sourceApp == sourceApp && !it.occurredAt.isBefore(from) && !it.occurredAt.isAfter(to)
    }
    override suspend fun linkRefundAndResolve(refundId: Long, paymentId: Long, userConfirmed: Boolean) {
        linkCalls += LinkCall(refundId, paymentId, userConfirmed)
    }
    override suspend fun recentNotificationTransactions(limit: Int) = rows.takeLast(limit)
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(rows)
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()
    override suspend fun saveTransaction(transaction: MoneyTransaction): Long = transaction.id
    override suspend fun updateTransaction(transaction: MoneyTransaction) = Unit
    override suspend fun deleteTransaction(transactionId: Long) = Unit
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long) = Unit
    override suspend fun saveRule(rule: Rule): Long = 1
}

private fun payment() = transaction(1, 6_000, TransactionType.EXPENSE, "2026-07-21T01:00:00Z")
private fun refund() = transaction(2, 6, TransactionType.REFUND, "2026-07-21T01:00:03Z")

private fun transaction(id: Long, amount: Long, type: TransactionType, occurredAt: String) = MoneyTransaction(
    id = id,
    occurredAt = Instant.parse(occurredAt),
    amount = MoneyAmount(amount),
    direction = type.defaultDirection,
    type = type,
    category = null,
    paymentMethod = null,
    merchant = null,
    counterparty = null,
    memo = null,
    sourceApp = "com.kbankwith.smartbank",
    sourceType = SourceType.NOTIFICATION,
    sourceNotificationHash = "hash-$id",
    status = if (type == TransactionType.REFUND) TransactionStatus.NEEDS_REVIEW else TransactionStatus.AUTO_CONFIRMED,
    confidence = 1.0,
    monthKey = YearMonth.of(2026, 7)
)
