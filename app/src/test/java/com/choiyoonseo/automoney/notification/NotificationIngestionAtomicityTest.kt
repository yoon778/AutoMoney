package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.parser.NotificationParser
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.ParseResult
import com.choiyoonseo.automoney.domain.parser.TransactionDraft
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationIngestionAtomicityTest {
    @Test
    fun reviewTransactionsUseAtomicSaveWithReview() = runTest {
        val repository = RecordingMoneyRepository()
        val useCase = NotificationIngestionUseCase(
            parser = StaticParser(reviewDraft()),
            categorizationEngine = CategorizationEngine(),
            duplicateDetector = DuplicateDetector(),
            repository = repository
        )

        val result = useCase.ingest(snapshot())

        assertThat(result).isInstanceOf(IngestionResult.Saved::class.java)
        assertThat(repository.saveWithReviewCalls).isEqualTo(1)
        assertThat(repository.saveTransactionCalls).isEqualTo(0)
        assertThat(repository.createReviewItemCalls).isEqualTo(0)
    }
}

private class StaticParser(
    private val draft: TransactionDraft
) : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean = true
    override fun parse(snapshot: NotificationSnapshot): ParseResult = ParseResult.Parsed(draft)
}

private class RecordingMoneyRepository : MoneyRepository {
    var saveWithReviewCalls = 0
    var saveTransactionCalls = 0
    var createReviewItemCalls = 0

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> = emptyList()
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()

    override suspend fun saveTransaction(transaction: MoneyTransaction): Long {
        saveTransactionCalls += 1
        return 1
    }

    override suspend fun saveTransactionWithReview(
        transaction: MoneyTransaction,
        reason: ReviewReason
    ): Long {
        saveWithReviewCalls += 1
        return 1
    }

    override suspend fun updateTransaction(transaction: MoneyTransaction) = Unit
    override suspend fun deleteTransaction(transactionId: Long) = Unit

    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) {
        createReviewItemCalls += 1
    }

    override suspend fun resolveReviewItem(reviewItemId: Long) = Unit
    override suspend fun saveRule(rule: Rule): Long = 1
}

private fun reviewDraft() = TransactionDraft(
    occurredAt = Instant.parse("2026-07-08T01:00:00Z"),
    amount = MoneyAmount(10_000),
    direction = TransactionDirection.NEUTRAL,
    type = TransactionType.TRANSFER,
    category = null,
    paymentMethod = "토스",
    merchant = null,
    counterparty = "김민수",
    memo = "송금 목적 확인 필요",
    sourceApp = "viva.republica.toss",
    sourceNotificationHash = "atomic-review-hash",
    status = TransactionStatus.NEEDS_REVIEW,
    confidence = 0.75,
    monthKey = YearMonth.of(2026, 7),
    reviewReason = ReviewReason.TRANSFER_UNKNOWN
)

private fun snapshot() = NotificationSnapshot(
    packageName = "viva.republica.toss",
    title = "토스",
    text = "김민수님에게 10,000원 송금했어요",
    bigText = null,
    postedAt = Instant.parse("2026-07-08T01:00:00Z")
)
