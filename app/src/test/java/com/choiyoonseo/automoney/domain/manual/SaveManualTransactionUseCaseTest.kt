package com.choiyoonseo.automoney.domain.manual

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.Category
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

class SaveManualTransactionUseCaseTest {
    @Test
    fun saveExpensePersistsManualExpenseTransaction() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = SaveManualTransactionUseCase(repository)

        val id = useCase.saveExpense(
            amountWon = 6100,
            categoryText = "카페/간식",
            memo = "스타벅스 홍대입구",
            occurredAt = Instant.parse("2026-07-01T01:00:00Z")
        )

        assertThat(id).isEqualTo(100)
        val saved = repository.savedTransactions.single()
        assertThat(saved.amount.won).isEqualTo(6100)
        assertThat(saved.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(saved.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(saved.category).isEqualTo(Category.CAFE_SNACK)
        assertThat(saved.paymentMethod).isEqualTo("수동 입력")
        assertThat(saved.merchant).isEqualTo("스타벅스 홍대입구")
        assertThat(saved.memo).isEqualTo("스타벅스 홍대입구")
        assertThat(saved.sourceType).isEqualTo(SourceType.MANUAL)
        assertThat(saved.sourceNotificationHash).isNull()
        assertThat(saved.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(saved.confidence).isEqualTo(1.0)
        assertThat(saved.monthKey).isEqualTo(YearMonth.of(2026, 7))
    }

    @Test
    fun saveExpenseRejectsZeroAmount() = runTest {
        val useCase = SaveManualTransactionUseCase(FakeMoneyRepository())

        var error: IllegalArgumentException? = null
        try {
            useCase.saveExpense(
                amountWon = 0,
                categoryText = "기타",
                memo = "",
                occurredAt = Instant.parse("2026-07-01T01:00:00Z")
            )
        } catch (e: IllegalArgumentException) {
            error = e
        }

        assertThat(error).isNotNull()
        assertThat(error).hasMessageThat().contains("0원보다 커야")
    }

    @Test
    fun saveExpenseUsesSelectedAccountAsPaymentMethod() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = SaveManualTransactionUseCase(repository)

        useCase.save(
            type = ManualEntryType.EXPENSE,
            amountWon = 12000,
            categoryText = "식비",
            memo = "점심",
            occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
            paymentMethod = "국민은행"
        )

        assertThat(repository.savedTransactions.single().paymentMethod).isEqualTo("국민은행")
    }

    @Test
    fun saveIncomePersistsManualIncomeTransaction() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = SaveManualTransactionUseCase(repository)

        val id = useCase.save(
            type = ManualEntryType.INCOME,
            amountWon = 500000,
            categoryText = "월급",
            memo = "7월 알바비",
            occurredAt = Instant.parse("2026-07-01T01:00:00Z")
        )

        assertThat(id).isEqualTo(100)
        val saved = repository.savedTransactions.single()
        assertThat(saved.amount.won).isEqualTo(500000)
        assertThat(saved.direction).isEqualTo(TransactionDirection.INCOME)
        assertThat(saved.type).isEqualTo(TransactionType.INCOME)
        assertThat(saved.category).isEqualTo(Category.SALARY)
        assertThat(saved.paymentMethod).isEqualTo("수동 입력")
        assertThat(saved.merchant).isEqualTo("7월 알바비")
        assertThat(saved.memo).isEqualTo("7월 알바비")
        assertThat(saved.sourceType).isEqualTo(SourceType.MANUAL)
        assertThat(saved.sourceNotificationHash).isNull()
        assertThat(saved.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(saved.confidence).isEqualTo(1.0)
        assertThat(saved.monthKey).isEqualTo(YearMonth.of(2026, 7))
    }

    @Test
    fun saveTransferPersistsNeutralManualTransfer() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = SaveManualTransactionUseCase(repository)

        val id = useCase.save(
            type = ManualEntryType.TRANSFER,
            amountWon = 100000,
            categoryText = "기타",
            memo = "내 계좌 이동",
            occurredAt = Instant.parse("2026-07-01T01:00:00Z")
        )

        assertThat(id).isEqualTo(100)
        val saved = repository.savedTransactions.single()
        assertThat(saved.amount.won).isEqualTo(100000)
        assertThat(saved.direction).isEqualTo(TransactionDirection.NEUTRAL)
        assertThat(saved.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(saved.category).isNull()
        assertThat(saved.paymentMethod).isEqualTo("수동 입력")
        assertThat(saved.merchant).isNull()
        assertThat(saved.counterparty).isEqualTo("내 계좌 이동")
        assertThat(saved.memo).isEqualTo("내 계좌 이동")
        assertThat(saved.sourceType).isEqualTo(SourceType.MANUAL)
        assertThat(saved.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(saved.monthKey).isEqualTo(YearMonth.of(2026, 7))
    }
}

private class FakeMoneyRepository : MoneyRepository {
    val savedTransactions = mutableListOf<MoneyTransaction>()
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

    override suspend fun updateTransaction(transaction: MoneyTransaction) = Unit
    override suspend fun deleteTransaction(transactionId: Long) = Unit
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long) = Unit
    override suspend fun saveRule(rule: Rule): Long = 0
}
