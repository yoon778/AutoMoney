package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.AssetAccountKind
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
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

class ResolveAccountTransferUseCaseTest {
    @Test
    fun resolveAccountTransferUsesSingleRepositoryCall() = runTest {
        val repository = RecordingAccountTransferRepository()
        val useCase = ResolveAccountTransferUseCase(repository)
        val accounts = listOf(
            AssetAccount(id = 1, name = "KB", balanceWon = 100_000, kind = AssetAccountKind.BANK),
            AssetAccount(id = 2, name = "Kakao", balanceWon = 20_000, kind = AssetAccountKind.BANK)
        )

        val result = useCase.resolve(
            accounts = accounts,
            reviewItemId = 7,
            transaction = transferTransaction(),
            fromAccountName = "KB",
            toAccountName = "Kakao",
            amountWon = 10_000,
            pairedIncomingReviewItem = null
        )

        assertThat(result.fromAccount.balanceWon).isEqualTo(90_000)
        assertThat(result.toAccount.balanceWon).isEqualTo(30_000)
        assertThat(result.transaction.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(repository.reviewItemId).isEqualTo(7)
        assertThat(repository.fromAccount).isEqualTo(result.fromAccount)
        assertThat(repository.toAccount).isEqualTo(result.toAccount)
        assertThat(repository.transaction).isEqualTo(result.transaction)
    }

    @Test
    fun explicitPairedTransferDoesNotApplySecondDirectBalanceChange() = runTest {
        val repository = RecordingAccountTransferRepository()
        val useCase = ResolveAccountTransferUseCase(repository)
        val transaction = transferTransaction().copy(
            linkedAssetAccountId = 1,
            balanceImpact = BalanceImpact.DEBIT
        )
        val paired = pairedReviewItem(
            transferTransaction().copy(
                id = 6,
                linkedAssetAccountId = 2,
                balanceImpact = BalanceImpact.CREDIT
            )
        )

        val result = useCase.resolve(
            accounts = accounts(),
            reviewItemId = 7,
            transaction = transaction,
            fromAccountName = "KB",
            toAccountName = "Kakao",
            amountWon = 10_000,
            pairedIncomingReviewItem = paired
        )

        assertThat(result.fromAccount.balanceWon).isEqualTo(100_000)
        assertThat(result.toAccount.balanceWon).isEqualTo(20_000)
        assertThat(result.transaction.linkedAssetAccountId).isEqualTo(1)
        assertThat(result.transaction.balanceImpact).isEqualTo(BalanceImpact.DEBIT)
        assertThat(result.pairedTransaction?.linkedAssetAccountId).isEqualTo(2)
        assertThat(result.pairedTransaction?.balanceImpact).isEqualTo(BalanceImpact.CREDIT)
        assertThat(repository.fromAccount).isEqualTo(accounts()[0])
        assertThat(repository.toAccount).isEqualTo(accounts()[1])
        assertThat(repository.pairedReviewItemId).isEqualTo(8)
    }

    @Test
    fun explicitTransferRequiresPairedTransaction() = runTest {
        val error = captureFailure {
            ResolveAccountTransferUseCase(RecordingAccountTransferRepository()).resolve(
                accounts = accounts(),
                reviewItemId = 7,
                transaction = transferTransaction().copy(
                    linkedAssetAccountId = 1,
                    balanceImpact = BalanceImpact.DEBIT
                ),
                fromAccountName = "KB",
                toAccountName = "Kakao",
                amountWon = 10_000,
                pairedIncomingReviewItem = null
            )
        }

        assertThat(error).hasMessageThat().contains("입금 알림")
    }

    @Test
    fun explicitTransferRequiresSameAmountAndOppositeEffects() = runTest {
        val useCase = ResolveAccountTransferUseCase(RecordingAccountTransferRepository())
        val debit = transferTransaction().copy(
            linkedAssetAccountId = 1,
            balanceImpact = BalanceImpact.DEBIT
        )
        val amountError = captureFailure {
            useCase.resolve(
                accounts = accounts(),
                reviewItemId = 7,
                transaction = debit,
                fromAccountName = "KB",
                toAccountName = "Kakao",
                amountWon = 10_000,
                pairedIncomingReviewItem = pairedReviewItem(
                    transferTransaction().copy(
                        id = 6,
                        amount = MoneyAmount(9_000),
                        linkedAssetAccountId = 2,
                        balanceImpact = BalanceImpact.CREDIT
                    )
                )
            )
        }
        val effectError = captureFailure {
            useCase.resolve(
                accounts = accounts(),
                reviewItemId = 7,
                transaction = debit,
                fromAccountName = "KB",
                toAccountName = "Kakao",
                amountWon = 10_000,
                pairedIncomingReviewItem = pairedReviewItem(
                    transferTransaction().copy(
                        id = 6,
                        linkedAssetAccountId = 2,
                        balanceImpact = BalanceImpact.DEBIT
                    )
                )
            )
        }

        assertThat(amountError).hasMessageThat().contains("금액")
        assertThat(effectError).hasMessageThat().contains("방향")
    }

    @Test
    fun explicitTransferRequiresSelectedAccountsToMatchLinkedIds() = runTest {
        val error = captureFailure {
            ResolveAccountTransferUseCase(RecordingAccountTransferRepository()).resolve(
                accounts = accounts(),
                reviewItemId = 7,
                transaction = transferTransaction().copy(
                    linkedAssetAccountId = 2,
                    balanceImpact = BalanceImpact.DEBIT
                ),
                fromAccountName = "KB",
                toAccountName = "Kakao",
                amountWon = 10_000,
                pairedIncomingReviewItem = pairedReviewItem(
                    transferTransaction().copy(
                        id = 6,
                        linkedAssetAccountId = 1,
                        balanceImpact = BalanceImpact.CREDIT
                    )
                )
            )
        }

        assertThat(error).hasMessageThat().contains("계좌")
    }
}

private class RecordingAccountTransferRepository : MoneyRepository {
    var reviewItemId: Long? = null
    var transaction: MoneyTransaction? = null
    var fromAccount: AssetAccount? = null
    var toAccount: AssetAccount? = null
    var pairedReviewItemId: Long? = null
    var pairedTransaction: MoneyTransaction? = null

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> = emptyList()
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()
    override suspend fun saveTransaction(transaction: MoneyTransaction): Long = 1
    override suspend fun updateTransaction(transaction: MoneyTransaction): Nothing =
        error("account transfer must use atomic repository call")
    override suspend fun deleteTransaction(transactionId: Long) = Unit
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long): Nothing =
        error("account transfer must use atomic repository call")
    override suspend fun saveRule(rule: Rule): Long = 1

    override suspend fun resolveAccountTransferReview(
        reviewItemId: Long,
        transaction: MoneyTransaction,
        fromAccount: AssetAccount,
        toAccount: AssetAccount,
        pairedReviewItemId: Long?,
        pairedTransaction: MoneyTransaction?
    ) {
        this.reviewItemId = reviewItemId
        this.transaction = transaction
        this.fromAccount = fromAccount
        this.toAccount = toAccount
        this.pairedReviewItemId = pairedReviewItemId
        this.pairedTransaction = pairedTransaction
    }
}

private fun accounts() = listOf(
    AssetAccount(id = 1, name = "KB", balanceWon = 100_000, kind = AssetAccountKind.BANK),
    AssetAccount(id = 2, name = "Kakao", balanceWon = 20_000, kind = AssetAccountKind.BANK)
)

private fun pairedReviewItem(transaction: MoneyTransaction) = OpenReviewItem(
    id = 8,
    transaction = transaction,
    reason = ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN,
    createdAt = Instant.parse("2026-07-08T01:01:00Z")
)

private suspend fun captureFailure(block: suspend () -> Unit): IllegalArgumentException {
    try {
        block()
    } catch (error: IllegalArgumentException) {
        return error
    }
    error("Expected IllegalArgumentException")
}

private fun transferTransaction() = MoneyTransaction(
    id = 5,
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
    sourceNotificationHash = "hash-transfer",
    status = TransactionStatus.NEEDS_REVIEW,
    confidence = 0.7,
    monthKey = YearMonth.of(2026, 7)
)
