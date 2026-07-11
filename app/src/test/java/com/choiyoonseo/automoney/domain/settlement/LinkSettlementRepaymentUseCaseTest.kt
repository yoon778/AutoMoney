package com.choiyoonseo.automoney.domain.settlement

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
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

class LinkSettlementRepaymentUseCaseTest {
    @Test
    fun linkMarksIncomingAsSettlementRepaymentAndResolvesReviewAtomically() = runTest {
        val repository = RecordingRepository()
        val result = LinkSettlementRepaymentUseCase(repository).link(
            reviewItemId = 9,
            repaymentTransaction = repayment(),
            settlementTransactionId = 41
        )

        assertThat(result.settlementParentId).isEqualTo(41)
        assertThat(result.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(repository.reviewItemId).isEqualTo(9)
        assertThat(repository.transaction).isEqualTo(result)
    }
}

private class RecordingRepository : MoneyRepository {
    var reviewItemId: Long? = null
    var transaction: MoneyTransaction? = null

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> = emptyList()
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()
    override suspend fun saveTransaction(transaction: MoneyTransaction): Long = 1
    override suspend fun saveNotificationTransaction(
        transaction: MoneyTransaction,
        accountHint: BankAccountHint?,
        reviewReason: ReviewReason?
    ) = error("not used")
    override suspend fun updateTransaction(transaction: MoneyTransaction): Nothing = error("atomic call required")
    override suspend fun deleteTransaction(transactionId: Long) = Unit
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long): Nothing = error("atomic call required")
    override suspend fun saveRule(rule: Rule): Long = 1
    override suspend fun resolveReviewItemWithTransaction(reviewItemId: Long, transaction: MoneyTransaction) {
        this.reviewItemId = reviewItemId
        this.transaction = transaction
    }
}

private fun repayment() = MoneyTransaction(
    id = 55,
    occurredAt = Instant.parse("2026-07-03T01:00:00Z"),
    amount = MoneyAmount(10_000),
    direction = TransactionDirection.INCOME,
    type = TransactionType.INCOME,
    category = Category.OTHER,
    paymentMethod = null,
    merchant = "friend",
    counterparty = null,
    memo = null,
    sourceApp = null,
    sourceType = SourceType.NOTIFICATION,
    sourceNotificationHash = "repayment",
    status = TransactionStatus.NEEDS_REVIEW,
    confidence = 0.9,
    monthKey = YearMonth.of(2026, 7)
)
