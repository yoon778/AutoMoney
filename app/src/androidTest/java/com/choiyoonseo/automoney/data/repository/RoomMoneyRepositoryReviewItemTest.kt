package com.choiyoonseo.automoney.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choiyoonseo.automoney.data.local.AppDatabase
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

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMoneyRepository(database)
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
