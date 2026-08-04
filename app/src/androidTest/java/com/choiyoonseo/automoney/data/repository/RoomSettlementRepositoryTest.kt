package com.choiyoonseo.automoney.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.report.countsAsReportIncome
import com.choiyoonseo.automoney.domain.settlement.LinkSettlementRepaymentUseCase
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomSettlementRepositoryTest {
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
    fun linkRecoveryResolvesReviewAndExcludesPrincipalFromIncome() = runBlocking {
        val settlementId = repository.saveTransaction(settlement())
        repository.saveTransactionWithReview(
            recovery(),
            ReviewReason.INCOME_UNKNOWN
        )
        val recoveryReview = repository.observeOpenReviewItems().first().single()

        val linked = LinkSettlementRepaymentUseCase(repository).link(
            reviewItemId = recoveryReview.id,
            repaymentTransaction = recoveryReview.transaction,
            settlementTransactionId = settlementId
        )

        assertEquals(settlementId, linked.settlementParentId)
        assertEquals(TransactionStatus.USER_EDITED, linked.status)
        assertEquals(TransactionType.INCOME, linked.type)
        assertFalse(linked.countsAsReportIncome())
        assertTrue(repository.observeOpenReviewItems().first().isEmpty())
    }

    @Test
    fun settlementTrackingQueryLoadsOnlyRelevantCandidateAndItsRecoveries() = runBlocking {
        val settlementId = repository.saveTransaction(settlement())
        val linkedRecoveryId = repository.saveTransaction(
            recovery().copy(
                sourceNotificationHash = "linked-recovery",
                status = TransactionStatus.USER_EDITED,
                settlementParentId = settlementId
            )
        )
        repository.saveTransaction(
            settlement().copy(
                occurredAt = Instant.parse("2026-06-15T01:00:00Z"),
                monthKey = YearMonth.of(2026, 6)
            )
        )
        repository.saveTransaction(
            recovery().copy(
                direction = TransactionDirection.EXPENSE,
                type = TransactionType.EXPENSE,
                sourceNotificationHash = "unrelated-expense"
            )
        )
        repository.saveTransactionWithReview(recovery(), ReviewReason.INCOME_UNKNOWN)

        val trackedIds = repository.observeSettlementTrackingTransactions()
            .first()
            .mapTo(mutableSetOf(), MoneyTransaction::id)

        assertEquals(setOf(settlementId, linkedRecoveryId), trackedIds)
    }

    private fun settlement() = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
        amount = MoneyAmount(12_000),
        direction = TransactionDirection.NEUTRAL,
        type = TransactionType.SETTLEMENT,
        category = Category.FOOD,
        paymentMethod = null,
        merchant = "Meal",
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = SourceType.MANUAL,
        sourceNotificationHash = null,
        status = TransactionStatus.USER_EDITED,
        confidence = 1.0,
        monthKey = YearMonth.of(2026, 7),
        balanceImpact = BalanceImpact.NONE,
        settlementPartyCount = 3,
        settlementMyShareWon = 4_000
    )

    private fun recovery() = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-03T01:00:00Z"),
        amount = MoneyAmount(8_000),
        direction = TransactionDirection.INCOME,
        type = TransactionType.INCOME,
        category = null,
        paymentMethod = "Checking",
        merchant = null,
        counterparty = "Friend",
        memo = null,
        sourceApp = "test.finance",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "recovery",
        status = TransactionStatus.NEEDS_REVIEW,
        confidence = 0.9,
        monthKey = YearMonth.of(2026, 7),
        balanceImpact = BalanceImpact.NONE
    )
}
