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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class RecordWalletTopupUsageUseCaseTest {
    @Test
    fun updatesExistingTopupAndInsertsWalletSpend() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = RecordWalletTopupUsageUseCase(repository)

        val result = useCase.recordUsage(
            topup = topup(id = 42, amountWon = 10000),
            usedAmount = MoneyAmount(6000),
            category = Category.CAFE_SNACK,
            merchant = "스타벅스 홍대입구",
            memo = "네이버페이 실제 사용"
        )

        assertThat(repository.updatedTransactions).hasSize(1)
        assertThat(repository.updatedTransactions.single().id).isEqualTo(42)
        assertThat(repository.updatedTransactions.single().status).isEqualTo(TransactionStatus.USER_EDITED)

        assertThat(repository.savedTransactions).hasSize(1)
        val savedSpend = repository.savedTransactions.single()
        assertThat(savedSpend.type).isEqualTo(TransactionType.WALLET_SPEND)
        assertThat(savedSpend.amount.won).isEqualTo(6000)
        assertThat(savedSpend.category).isEqualTo(Category.CAFE_SNACK)
        assertThat(savedSpend.merchant).isEqualTo("스타벅스 홍대입구")

        assertThat(result.remainingAmount.won).isEqualTo(4000)
        assertThat(result.walletSpend!!.id).isEqualTo(100)
    }

    @Test
    fun savesNewTopupWhenInputHasNoId() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = RecordWalletTopupUsageUseCase(repository)

        val result = useCase.recordUsage(
            topup = topup(id = 0, amountWon = 10000),
            usedAmount = MoneyAmount(0),
            category = Category.OTHER,
            merchant = "미사용",
            memo = null
        )

        assertThat(repository.updatedTransactions).isEmpty()
        assertThat(repository.savedTransactions).hasSize(1)
        assertThat(repository.savedTransactions.single().type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(result.reviewedTopup.id).isEqualTo(100)
        assertThat(result.walletSpend).isNull()
        assertThat(result.remainingAmount.won).isEqualTo(10000)
    }

    private fun topup(id: Long, amountWon: Long) = MoneyTransaction(
        id = id,
        occurredAt = Instant.parse("2026-06-27T03:47:00Z"),
        amount = MoneyAmount(amountWon),
        direction = TransactionDirection.NEUTRAL,
        type = TransactionType.WALLET_TOPUP,
        category = null,
        paymentMethod = "토스",
        merchant = "네이버페이",
        counterparty = null,
        memo = "네이버페이 충전",
        sourceApp = "viva.republica.toss",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash",
        status = TransactionStatus.NEEDS_REVIEW,
        confidence = 0.85,
        monthKey = YearMonth.of(2026, 6)
    )
}

private class FakeMoneyRepository : MoneyRepository {
    val savedTransactions = mutableListOf<MoneyTransaction>()
    val updatedTransactions = mutableListOf<MoneyTransaction>()
    private var nextId = 100L

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> = emptyList()
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()

    override suspend fun saveTransaction(transaction: MoneyTransaction): Long {
        savedTransactions += transaction
        return nextId++
    }

    override suspend fun updateTransaction(transaction: MoneyTransaction) {
        updatedTransactions += transaction
    }

    override suspend fun deleteTransaction(transactionId: Long) = Unit
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long) = Unit
    override suspend fun saveRule(rule: Rule): Long = 0
}
