package com.choiyoonseo.automoney.domain.transactions

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.RuleAction
import com.choiyoonseo.automoney.domain.model.RuleMatchType
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

class EditTransactionUseCaseTest {
    @Test
    fun updateChangesAmountCategoryAndMemoAndMarksUserEdited() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.update(
            transaction = transaction(),
            amountWon = 4800,
            categoryText = "식비",
            memo = "김밥천국 점심",
            occurredAt = Instant.parse("2026-07-05T03:30:00Z")
        )

        val updated = repository.updatedTransactions.single()
        assertThat(updated.id).isEqualTo(12)
        assertThat(updated.amount.won).isEqualTo(4800)
        assertThat(updated.category).isEqualTo(Category.FOOD)
        assertThat(updated.memo).isEqualTo("김밥천국 점심")
        assertThat(updated.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(updated.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(updated.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(updated.confidence).isEqualTo(1.0)
        assertThat(updated.occurredAt).isEqualTo(Instant.parse("2026-07-05T03:30:00Z"))
        assertThat(updated.monthKey).isEqualTo(YearMonth.of(2026, 7))
    }

    @Test
    fun updateRejectsZeroAmount() = runTest {
        val useCase = EditTransactionUseCase(FakeMoneyRepository())

        var error: IllegalArgumentException? = null
        try {
            useCase.update(
                transaction = transaction(),
                amountWon = 0,
                categoryText = "기타",
                memo = "",
                occurredAt = transaction().occurredAt
            )
        } catch (e: IllegalArgumentException) {
            error = e
        }

        assertThat(error).isNotNull()
        assertThat(error).hasMessageThat().contains("0원보다 커야")
    }

    @Test
    fun updateChangesPaymentMethodWhenAccountIsCorrected() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.update(
            transaction = transaction(),
            amountWon = 6100,
            categoryText = "카페/간식",
            memo = "커피",
            occurredAt = transaction().occurredAt,
            paymentMethod = "국민은행 통장"
        )

        assertThat(repository.updatedTransactions.single().paymentMethod)
            .isEqualTo("국민은행 통장")
    }

    @Test
    fun updateCanConvertNeutralReviewToExpense() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.update(
            transaction = transaction().copy(
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.TRANSFER,
                category = null,
                status = TransactionStatus.NEEDS_REVIEW
            ),
            amountWon = 6100,
            categoryText = "카페/간식",
            memo = "친구가 먼저 결제한 커피",
            occurredAt = transaction().occurredAt,
            paymentMethod = "국민은행 통장",
            transactionType = TransactionType.EXPENSE
        )

        val updated = repository.updatedTransactions.single()
        assertThat(updated.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(updated.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(updated.category).isEqualTo(Category.CAFE_SNACK)
        assertThat(updated.status).isEqualTo(TransactionStatus.USER_EDITED)
    }

    @Test
    fun updateIncomeCanUseInvestmentReturnCategory() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.update(
            transaction = transaction().copy(
                direction = TransactionDirection.INCOME,
                type = TransactionType.INCOME,
                category = Category.SALARY
            ),
            amountWon = 12000,
            categoryText = "투자성과",
            memo = "배당금",
            occurredAt = transaction().occurredAt,
            paymentMethod = "국민은행",
            transactionType = TransactionType.INCOME
        )

        val updated = repository.updatedTransactions.single()
        assertThat(updated.type).isEqualTo(TransactionType.INCOME)
        assertThat(updated.direction).isEqualTo(TransactionDirection.INCOME)
        assertThat(updated.category).isEqualTo(Category.INVESTMENT_RETURN)
    }

    @Test
    fun updateLearnsCategoryRuleForSameMerchant() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.update(
            transaction = transaction().copy(merchant = "Starbucks", category = Category.OTHER),
            amountWon = 6100,
            categoryText = Category.CAFE_SNACK.name,
            memo = "coffee",
            occurredAt = transaction().occurredAt,
            transactionType = TransactionType.EXPENSE
        )

        assertThat(repository.savedRules).contains(
            Rule(
                matchType = RuleMatchType.MERCHANT,
                matchValue = "Starbucks",
                action = RuleAction.SET_CATEGORY,
                targetValue = Category.CAFE_SNACK.name
            )
        )
    }

    @Test
    fun updateLearnsWalletTopupRuleWhenReviewTypeIsCorrected() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.update(
            transaction = transaction().copy(
                merchant = "NaverPay",
                type = TransactionType.TRANSFER,
                direction = TransactionDirection.NEUTRAL,
                category = null,
                status = TransactionStatus.NEEDS_REVIEW
            ),
            amountWon = 10000,
            categoryText = Category.OTHER.name,
            memo = "topup",
            occurredAt = transaction().occurredAt,
            transactionType = TransactionType.WALLET_TOPUP
        )

        assertThat(repository.savedRules).contains(
            Rule(
                matchType = RuleMatchType.MERCHANT,
                matchValue = "NaverPay",
                action = RuleAction.MARK_AS_WALLET_TOPUP,
                targetValue = TransactionType.WALLET_TOPUP.name
            )
        )
    }

    @Test
    fun updateSkipsDuplicateLearnedRule() = runTest {
        val existingRule = Rule(
            matchType = RuleMatchType.MERCHANT,
            matchValue = "Starbucks",
            action = RuleAction.SET_CATEGORY,
            targetValue = Category.CAFE_SNACK.name
        )
        val repository = FakeMoneyRepository(enabledRules = listOf(existingRule))
        val useCase = EditTransactionUseCase(repository)

        useCase.update(
            transaction = transaction().copy(merchant = "Starbucks", category = Category.OTHER),
            amountWon = 6100,
            categoryText = Category.CAFE_SNACK.name,
            memo = "coffee",
            occurredAt = transaction().occurredAt,
            transactionType = TransactionType.EXPENSE
        )

        assertThat(repository.savedRules).isEmpty()
    }

    @Test
    fun excludeMarksTransactionExcludedWithoutDeletingIt() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.exclude(
            transaction().copy(
                linkedAssetAccountId = 7,
                balanceImpact = BalanceImpact.DEBIT
            )
        )

        val updated = repository.updatedTransactions.single()
        assertThat(updated.id).isEqualTo(12)
        assertThat(updated.direction).isEqualTo(TransactionDirection.NEUTRAL)
        assertThat(updated.type).isEqualTo(TransactionType.EXCLUDED)
        assertThat(updated.category).isNull()
        assertThat(updated.status).isEqualTo(TransactionStatus.EXCLUDED)
        assertThat(updated.confidence).isEqualTo(1.0)
        assertThat(updated.linkedAssetAccountId).isEqualTo(7)
        assertThat(updated.balanceImpact).isEqualTo(BalanceImpact.DEBIT)
        assertThat(updated.memo).contains("사용자 삭제")
    }

    @Test
    fun deleteRemovesTransaction() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = EditTransactionUseCase(repository)

        useCase.delete(transaction())

        assertThat(repository.deletedTransactions).containsExactly(12L)
    }

    private fun transaction() = MoneyTransaction(
        id = 12,
        occurredAt = Instant.parse("2026-07-02T01:00:00Z"),
        amount = MoneyAmount(6100),
        direction = TransactionDirection.EXPENSE,
        type = TransactionType.EXPENSE,
        category = Category.CAFE_SNACK,
        paymentMethod = "수동 입력",
        merchant = "스타벅스 홍대입구",
        counterparty = null,
        memo = "커피",
        sourceApp = null,
        sourceType = SourceType.MANUAL,
        sourceNotificationHash = null,
        status = TransactionStatus.AUTO_CONFIRMED,
        confidence = 0.9,
        monthKey = YearMonth.of(2026, 7)
    )
}

private class FakeMoneyRepository : MoneyRepository {
    constructor(enabledRules: List<Rule> = emptyList()) {
        this.enabledRules = enabledRules
    }

    val updatedTransactions = mutableListOf<MoneyTransaction>()
    val deletedTransactions = mutableListOf<Long>()
    val savedRules = mutableListOf<Rule>()
    private val enabledRules: List<Rule>

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> = emptyList()
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = enabledRules
    override suspend fun saveTransaction(transaction: MoneyTransaction): Long = 0
    override suspend fun updateTransaction(transaction: MoneyTransaction) {
        updatedTransactions += transaction
    }
    override suspend fun deleteTransaction(transactionId: Long) {
        deletedTransactions += transactionId
    }
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long) = Unit
    override suspend fun saveRule(rule: Rule): Long {
        savedRules += rule
        return savedRules.size.toLong()
    }
}
