package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResolveReviewUseCaseTest {
    @Test
    fun resolveUsesAtomicRepositoryCall() = runTest {
        val repository = AtomicReviewRepository()
        val useCase = ResolveReviewUseCase(repository)
        val transaction = reviewTransaction()

        val updated = useCase.resolve(
            reviewItemId = 9,
            transaction = transaction,
            resolution = ReviewResolution.EXCLUDE,
            userMemo = "own account move"
        )

        assertThat(repository.atomicReviewItemId).isEqualTo(9)
        assertThat(repository.atomicTransaction).isEqualTo(updated)
        assertThat(updated.type).isEqualTo(TransactionType.EXCLUDED)
        assertThat(updated.status).isEqualTo(TransactionStatus.EXCLUDED)
    }

    @Test
    fun resolveSettlementPersistsRequestedPartyCountAndMyShare() = runTest {
        val updated = ResolveReviewUseCase(AtomicReviewRepository()).resolve(
            reviewItemId = 9,
            transaction = reviewTransaction(),
            resolution = ReviewResolution.SETTLEMENT,
            userMemo = "dinner",
            settlementPartyCount = 3,
            settlementMyShareWon = 3_000
        )

        assertThat(updated.type).isEqualTo(TransactionType.SETTLEMENT)
        assertThat(updated.settlementPartyCount).isEqualTo(3)
        assertThat(updated.settlementMyShareWon).isEqualTo(3_000)
        assertThat(updated.settlementParentId).isNull()
    }

    @Test
    fun resolveSettlementDefaultsMyShareFromPartyCount() = runTest {
        val updated = ResolveReviewUseCase(AtomicReviewRepository()).resolve(
            reviewItemId = 9,
            transaction = reviewTransaction(),
            resolution = ReviewResolution.SETTLEMENT,
            userMemo = null,
            settlementPartyCount = 3
        )

        assertThat(updated.settlementMyShareWon).isEqualTo(3_333)
    }
}

private class AtomicReviewRepository : MoneyRepository {
    var atomicReviewItemId: Long? = null
    var atomicTransaction: MoneyTransaction? = null

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> = emptyList()
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()
    override suspend fun saveTransaction(transaction: MoneyTransaction): Long = 1
    override suspend fun updateTransaction(transaction: MoneyTransaction): Nothing =
        error("resolve must use atomic repository call")
    override suspend fun deleteTransaction(transactionId: Long) = Unit
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long): Nothing =
        error("resolve must use atomic repository call")
    override suspend fun saveRule(rule: Rule): Long = 1

    override suspend fun resolveReviewItemWithTransaction(
        reviewItemId: Long,
        transaction: MoneyTransaction
    ) {
        atomicReviewItemId = reviewItemId
        atomicTransaction = transaction
    }
}

private fun reviewTransaction() = MoneyTransaction(
    id = 3,
    occurredAt = Instant.parse("2026-07-08T01:00:00Z"),
    amount = MoneyAmount(10_000),
    direction = TransactionDirection.NEUTRAL,
    type = TransactionType.TRANSFER,
    category = Category.OTHER,
    paymentMethod = "KB",
    merchant = null,
    counterparty = "friend",
    memo = "transfer review",
    sourceApp = "com.kbstar.kbbank",
    sourceType = SourceType.NOTIFICATION,
    sourceNotificationHash = "hash",
    status = TransactionStatus.NEEDS_REVIEW,
    confidence = 0.7,
    monthKey = YearMonth.of(2026, 7)
)
