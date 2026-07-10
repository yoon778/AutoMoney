package com.choiyoonseo.automoney.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class RoomMoneyRepositoryReviewItemTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomMoneyRepository
    private lateinit var assetRepository: RoomAssetRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMoneyRepository(database)
        assetRepository = RoomAssetRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeOpenReviewItemsReturnsTransactionAndResolveHidesIt() = runBlocking {
        val transactionId = repository.saveTransaction(walletTopup())
        repository.createReviewItem(transactionId, ReviewReason.WALLET_TOPUP)

        val openItems = repository.observeOpenReviewItems().first()

        assertEquals(1, openItems.size)
        assertEquals(transactionId, openItems.single().transaction.id)
        assertEquals(ReviewReason.WALLET_TOPUP, openItems.single().reason)

        repository.resolveReviewItem(openItems.single().id)

        assertTrue(repository.observeOpenReviewItems().first().isEmpty())
    }

    @Test
    fun unmatchedNotificationAccountIsStoredAsReviewTransaction() = runBlocking {
        val transactionId = repository.saveTransaction(autoExpense(paymentMethod = "Unknown Card"))

        val transaction = repository.observeTransactionsForMonth(YearMonth.of(2026, 7))
            .first()
            .single { it.id == transactionId }
        val reviewItem = repository.observeOpenReviewItems().first().single()

        assertEquals(TransactionStatus.NEEDS_REVIEW, transaction.status)
        assertEquals(ReviewReason.ACCOUNT_UNMATCHED, reviewItem.reason)
        assertEquals(transactionId, reviewItem.transaction.id)
    }

    @Test
    fun unmatchedAccountTakesPriorityOverGenericReviewReason() = runBlocking {
        val transactionId = repository.saveTransactionWithReview(
            autoExpense(
                paymentMethod = "Unknown Card",
                status = TransactionStatus.NEEDS_REVIEW
            ),
            ReviewReason.LOW_CONFIDENCE_CATEGORY
        )

        val transaction = repository.observeTransactionsForMonth(YearMonth.of(2026, 7))
            .first()
            .single { it.id == transactionId }
        val reviewItem = repository.observeOpenReviewItems().first().single()

        assertEquals(TransactionStatus.NEEDS_REVIEW, transaction.status)
        assertEquals(ReviewReason.ACCOUNT_UNMATCHED, reviewItem.reason)
        assertEquals(transactionId, reviewItem.transaction.id)
    }

    @Test
    fun matchedReviewTransferAppliesDebitExactlyOnce() = runBlocking {
        val accountId = saveBankAccount(balanceWon = 50_000)
        val result = repository.saveNotificationTransaction(
            transaction = bankTransfer(hash = "matched-review"),
            accountHint = bankHint(AccountMovementDirection.DEBIT),
            reviewReason = ReviewReason.TRANSFER_UNKNOWN
        )

        val stored = transaction(result.transactionId)
        val reviewItems = repository.observeOpenReviewItems().first()

        assertEquals(ReviewReason.TRANSFER_UNKNOWN, result.reviewReason)
        assertEquals(accountId, stored.linkedAssetAccountId)
        assertEquals(BalanceImpact.DEBIT, stored.balanceImpact)
        assertEquals(TransactionStatus.NEEDS_REVIEW, stored.status)
        assertEquals(40_000, accountBalance(accountId))
        assertEquals(1, reviewItems.size)
        assertEquals(ReviewReason.TRANSFER_UNKNOWN, reviewItems.single().reason)
    }

    @Test
    fun missingSuffixStoresNoneAndAccountUnmatchedReview() = runBlocking {
        val accountId = saveBankAccount(balanceWon = 50_000)
        val result = repository.saveNotificationTransaction(
            transaction = bankTransfer(hash = "missing-suffix"),
            accountHint = bankHint(AccountMovementDirection.DEBIT, accountLast4 = "9999"),
            reviewReason = ReviewReason.TRANSFER_UNKNOWN
        )

        val stored = transaction(result.transactionId)
        val reviewItems = repository.observeOpenReviewItems().first()

        assertEquals(ReviewReason.ACCOUNT_UNMATCHED, result.reviewReason)
        assertEquals(null, stored.linkedAssetAccountId)
        assertEquals(BalanceImpact.NONE, stored.balanceImpact)
        assertEquals(TransactionStatus.NEEDS_REVIEW, stored.status)
        assertEquals(50_000, accountBalance(accountId))
        assertEquals(1, reviewItems.size)
        assertEquals(ReviewReason.ACCOUNT_UNMATCHED, reviewItems.single().reason)
    }

    @Test
    fun duplicateSuffixStoresNoneAndAccountAmbiguousReview() = runBlocking {
        saveBankAccount(name = "Primary", balanceWon = 50_000)
        saveBankAccount(name = "Secondary", balanceWon = 30_000)
        val result = repository.saveNotificationTransaction(
            transaction = bankTransfer(hash = "ambiguous-suffix"),
            accountHint = bankHint(AccountMovementDirection.CREDIT),
            reviewReason = null
        )

        val stored = transaction(result.transactionId)
        val reviewItems = repository.observeOpenReviewItems().first()

        assertEquals(ReviewReason.ACCOUNT_AMBIGUOUS, result.reviewReason)
        assertEquals(null, stored.linkedAssetAccountId)
        assertEquals(BalanceImpact.NONE, stored.balanceImpact)
        assertEquals(listOf(50_000L, 30_000L), assetRepository.observeAccounts().first().map { it.balanceWon })
        assertEquals(1, reviewItems.size)
        assertEquals(ReviewReason.ACCOUNT_AMBIGUOUS, reviewItems.single().reason)
    }

    @Test
    fun hintlessCardStoresNoneWithoutAccountReview() = runBlocking {
        val accountId = saveBankAccount(name = "KB", balanceWon = 50_000)
        val result = repository.saveNotificationTransaction(
            transaction = autoExpense(paymentMethod = "KB"),
            accountHint = null,
            reviewReason = null
        )

        val stored = transaction(result.transactionId)

        assertEquals(null, result.reviewReason)
        assertEquals(null, stored.linkedAssetAccountId)
        assertEquals(BalanceImpact.NONE, stored.balanceImpact)
        assertEquals(TransactionStatus.AUTO_CONFIRMED, stored.status)
        assertEquals(50_000, accountBalance(accountId))
        assertTrue(repository.observeOpenReviewItems().first().isEmpty())
    }

    @Test
    fun duplicateNotificationRollsBackBalanceEffect() = runBlocking {
        val accountId = saveBankAccount(balanceWon = 50_000)
        val transaction = bankTransfer(hash = "duplicate")
        repository.saveNotificationTransaction(
            transaction = transaction,
            accountHint = bankHint(AccountMovementDirection.DEBIT),
            reviewReason = null
        )

        var duplicateThrown = false
        try {
            repository.saveNotificationTransaction(
                transaction = transaction,
                accountHint = bankHint(AccountMovementDirection.DEBIT),
                reviewReason = null
            )
        } catch (_: DuplicateNotificationException) {
            duplicateThrown = true
        }

        assertTrue(duplicateThrown)
        assertEquals(40_000, accountBalance(accountId))
        assertEquals(1, repository.observeTransactionsForMonth(YearMonth.of(2026, 7)).first().size)
    }

    @Test
    fun explicitUpdateAndDeleteReplaceThenReverseEffect() = runBlocking {
        val accountId = saveBankAccount(balanceWon = 50_000)
        val result = repository.saveNotificationTransaction(
            transaction = bankTransfer(hash = "update-delete"),
            accountHint = bankHint(AccountMovementDirection.DEBIT),
            reviewReason = null
        )
        val stored = transaction(result.transactionId)

        repository.updateTransaction(stored.copy(amount = MoneyAmount(5_000)))

        assertEquals(45_000, accountBalance(accountId))

        repository.deleteTransaction(result.transactionId)

        assertEquals(50_000, accountBalance(accountId))
        assertTrue(repository.observeTransactionsForMonth(YearMonth.of(2026, 7)).first().isEmpty())
    }

    private suspend fun saveBankAccount(
        name: String = "Checking",
        balanceWon: Long
    ): Long = assetRepository.saveAccount(
        AssetAccount(
            name = name,
            balanceWon = balanceWon,
            bankProvider = BankProvider.KB,
            accountLast4 = "1234"
        )
    )

    private suspend fun accountBalance(accountId: Long): Long =
        assetRepository.observeAccounts().first().single { it.id == accountId }.balanceWon

    private suspend fun transaction(transactionId: Long): MoneyTransaction =
        repository.observeTransactionsForMonth(YearMonth.of(2026, 7))
            .first()
            .single { it.id == transactionId }

    private fun bankHint(
        direction: AccountMovementDirection,
        accountLast4: String = "1234"
    ) = BankAccountHint(
        provider = BankProvider.KB,
        accountLast4 = accountLast4,
        direction = direction,
        eventKind = BankEventKind.TRANSFER
    )

    private fun bankTransfer(hash: String) = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
        amount = MoneyAmount(10_000),
        direction = TransactionDirection.NEUTRAL,
        type = TransactionType.TRANSFER,
        category = null,
        paymentMethod = "KB",
        merchant = null,
        counterparty = "Counterparty",
        memo = "Bank transfer",
        sourceApp = "com.kbstar.kbbank",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = hash,
        status = TransactionStatus.NEEDS_REVIEW,
        confidence = 0.7,
        monthKey = YearMonth.of(2026, 7)
    )

    private fun walletTopup() = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
        amount = MoneyAmount(10000),
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
        confidence = 0.8,
        monthKey = YearMonth.of(2026, 7)
    )

    private fun autoExpense(
        paymentMethod: String,
        status: TransactionStatus = TransactionStatus.AUTO_CONFIRMED
    ) = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
        amount = MoneyAmount(12000),
        direction = TransactionDirection.EXPENSE,
        type = TransactionType.EXPENSE,
        category = null,
        paymentMethod = paymentMethod,
        merchant = "Store",
        counterparty = null,
        memo = "Store payment",
        sourceApp = "test.finance",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash-$paymentMethod-$status",
        status = status,
        confidence = 0.9,
        monthKey = YearMonth.of(2026, 7)
    )
}
