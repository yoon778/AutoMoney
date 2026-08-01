package com.choiyoonseo.automoney.domain.manual

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.category.UserCategory
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
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
    fun saveSpecialExpensePersistsDebitWithoutBudgetLink() = runTest {
        val repository = FakeMoneyRepository()
        val account = AssetAccount(id = 7, name = "비상금", balanceWon = 1_000_000)

        SaveManualTransactionUseCase(repository).save(
            type = ManualEntryType.SPECIAL_EXPENSE,
            amountWon = 500_000,
            categoryText = "의료/건강",
            memo = "응급 치료",
            account = account,
            budgetPlanId = 3,
            fixedExpensePlanId = 4
        )

        val saved = repository.savedTransactions.single()
        assertThat(saved.type).isEqualTo(TransactionType.SPECIAL_EXPENSE)
        assertThat(saved.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(saved.balanceImpact).isEqualTo(BalanceImpact.DEBIT)
        assertThat(saved.category).isEqualTo(Category.HEALTH)
        assertThat(saved.budgetPlanId).isNull()
        assertThat(saved.fixedExpensePlanId).isNull()
    }

    @Test
    fun saveExpensePersistsManualExpenseTransaction() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = SaveManualTransactionUseCase(repository)

        val id = useCase.save(
            type = ManualEntryType.EXPENSE,
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
            useCase.save(
                type = ManualEntryType.EXPENSE,
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
    fun manualExpenseStoresStableIdAndDebit() = runTest {
        val repository = FakeMoneyRepository()
        val useCase = SaveManualTransactionUseCase(repository)
        val account = AssetAccount(id = 7, name = "Primary", balanceWon = 100_000)

        useCase.save(
            type = ManualEntryType.EXPENSE,
            amountWon = 12_000,
            categoryText = Category.FOOD.name,
            memo = "lunch",
            account = account
        )

        val saved = repository.savedTransactions.single()
        assertThat(saved.paymentMethod).isEqualTo("Primary")
        assertThat(saved.linkedAssetAccountId).isEqualTo(7)
        assertThat(saved.balanceImpact).isEqualTo(BalanceImpact.DEBIT)
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
        assertThat(saved.linkedAssetAccountId).isNull()
        assertThat(saved.balanceImpact).isEqualTo(BalanceImpact.NONE)
        assertThat(saved.category).isNull()
        assertThat(saved.paymentMethod).isEqualTo("수동 입력")
        assertThat(saved.merchant).isNull()
        assertThat(saved.counterparty).isEqualTo("내 계좌 이동")
        assertThat(saved.memo).isEqualTo("내 계좌 이동")
        assertThat(saved.sourceType).isEqualTo(SourceType.MANUAL)
        assertThat(saved.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(saved.monthKey).isEqualTo(YearMonth.of(2026, 7))
    }

    @Test
    fun saveExpenseCreatesCustomCategorySnapshotForFreeText() = runTest {
        val repository = FakeMoneyRepository()
        val categoryRepository = FakeUserCategoryRepository()
        val useCase = SaveManualTransactionUseCase(repository, categoryRepository)

        useCase.save(
            type = ManualEntryType.EXPENSE,
            amountWon = 25_000,
            categoryText = "데이트비용",
            memo = "저녁",
            occurredAt = Instant.parse("2026-07-01T01:00:00Z")
        )

        val saved = repository.savedTransactions.single()
        assertThat(saved.category).isEqualTo(Category.OTHER)
        assertThat(saved.customCategoryId).isEqualTo(101)
        assertThat(saved.customCategoryName).isEqualTo("데이트비용")
        assertThat(categoryRepository.resolvedKind).isEqualTo(UserCategoryKind.EXPENSE)
    }

    @Test
    fun saveExpenseStoresSelectedBudgetPlan() = runTest {
        val repository = FakeMoneyRepository()

        SaveManualTransactionUseCase(repository).save(
            type = ManualEntryType.EXPENSE,
            amountWon = 25_000,
            categoryText = "식비",
            memo = "저녁",
            budgetPlanId = 7
        )

        assertThat(repository.savedTransactions.single().budgetPlanId).isEqualTo(7)
    }

    @Test
    fun saveExpenseCanLinkFixedExpensePlan() = runTest {
        val repository = FakeMoneyRepository()

        SaveManualTransactionUseCase(repository).save(
            type = ManualEntryType.EXPENSE,
            amountWon = 70_000,
            categoryText = "통신",
            memo = "휴대폰 요금",
            fixedExpensePlanId = 9
        )

        val saved = repository.savedTransactions.single()
        assertThat(saved.type).isEqualTo(TransactionType.FIXED_EXPENSE)
        assertThat(saved.fixedExpensePlanId).isEqualTo(9)
        assertThat(saved.budgetPlanId).isNull()
    }

    @Test
    fun saveExpenseIgnoresInvalidFixedExpensePlanId() = runTest {
        val repository = FakeMoneyRepository()

        SaveManualTransactionUseCase(repository).save(
            type = ManualEntryType.EXPENSE,
            amountWon = 70_000,
            categoryText = "통신",
            memo = "휴대폰 요금",
            fixedExpensePlanId = 0
        )

        val saved = repository.savedTransactions.single()
        assertThat(saved.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(saved.fixedExpensePlanId).isNull()
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

private class FakeUserCategoryRepository : UserCategoryRepository {
    var resolvedKind: UserCategoryKind? = null

    override fun observeActiveCategories() = flowOf(emptyList<UserCategory>())
    override suspend fun add(kind: UserCategoryKind, name: String): Long = 101
    override suspend fun rename(id: Long, name: String) = Unit
    override suspend fun delete(id: Long) = Unit

    override suspend fun resolveOrCreate(kind: UserCategoryKind, name: String): UserCategory {
        resolvedKind = kind
        return UserCategory.create(id = 101, kind = kind, name = name)
    }
}
