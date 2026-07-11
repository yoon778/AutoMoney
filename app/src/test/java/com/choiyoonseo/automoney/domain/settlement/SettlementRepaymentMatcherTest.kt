package com.choiyoonseo.automoney.domain.settlement

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.YearMonth
import org.junit.Test

class SettlementRepaymentMatcherTest {
    @Test
    fun findsUniqueMatchForSplitShareWithinFourteenDays() {
        val settlement = transaction(id = 41, amountWon = 30_000).copy(
            type = TransactionType.SETTLEMENT,
            direction = TransactionDirection.NEUTRAL,
            settlementPartyCount = 3,
            settlementMyShareWon = 10_000
        )
        val incoming = transaction(
            id = 55,
            amountWon = 10_000,
            occurredAt = settlement.occurredAt.plus(14, ChronoUnit.DAYS)
        ).copy(direction = TransactionDirection.INCOME, type = TransactionType.INCOME)

        val match = findSettlementMatch(
            incoming = incoming,
            settlements = listOf(settlement),
            linkedRecoveries = emptyList()
        )

        assertThat(match?.settlementTransactionId).isEqualTo(41)
    }

    @Test
    fun rejectsIncomingAfterFourteenDayWindow() {
        val settlement = transaction(id = 41, amountWon = 30_000).copy(
            type = TransactionType.SETTLEMENT,
            direction = TransactionDirection.NEUTRAL,
            settlementPartyCount = 3,
            settlementMyShareWon = 10_000
        )
        val incoming = transaction(
            id = 55,
            amountWon = 10_000,
            occurredAt = settlement.occurredAt.plus(14, ChronoUnit.DAYS).plusMillis(1)
        ).copy(direction = TransactionDirection.INCOME, type = TransactionType.INCOME)

        assertThat(
            findSettlementMatch(incoming, listOf(settlement), emptyList())
        ).isNull()
    }

    @Test
    fun findsMatchForRemainingReceivableAfterEarlierRecovery() {
        val settlement = transaction(id = 41, amountWon = 40_000).copy(
            type = TransactionType.SETTLEMENT,
            direction = TransactionDirection.NEUTRAL,
            settlementPartyCount = 3,
            settlementMyShareWon = 14_000
        )
        val earlierRecovery = transaction(id = 51, amountWon = 10_000).copy(
            direction = TransactionDirection.INCOME,
            type = TransactionType.INCOME,
            settlementParentId = 41
        )
        val incoming = transaction(id = 55, amountWon = 16_000).copy(
            direction = TransactionDirection.INCOME,
            type = TransactionType.INCOME
        )

        assertThat(
            findSettlementMatch(incoming, listOf(settlement), listOf(earlierRecovery))
                ?.settlementTransactionId
        ).isEqualTo(41)
    }

    private fun transaction(
        id: Long,
        amountWon: Long,
        occurredAt: Instant = Instant.parse("2026-07-01T01:00:00Z")
    ) = MoneyTransaction(
        id = id,
        occurredAt = occurredAt,
        amount = MoneyAmount(amountWon),
        direction = TransactionDirection.EXPENSE,
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        paymentMethod = null,
        merchant = "meal",
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = null,
        status = TransactionStatus.NEEDS_REVIEW,
        confidence = 0.8,
        monthKey = YearMonth.of(2026, 7)
    )
}
