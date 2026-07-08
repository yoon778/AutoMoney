package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class AccountTransferMatcherTest {

    @Test
    fun findAccountTransferCandidatesMatchesSameAmountOutgoingAndIncomingNearEachOther() {
        val outgoing = transaction(
            id = 1,
            occurredAt = Instant.parse("2026-07-06T01:00:00Z"),
            amountWon = 45_000,
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.TRANSFER
        )
        val incoming = transaction(
            id = 2,
            occurredAt = Instant.parse("2026-07-06T01:04:00Z"),
            amountWon = 45_000,
            direction = TransactionDirection.INCOME,
            type = TransactionType.INCOME
        )

        val candidates = findAccountTransferCandidates(listOf(outgoing, incoming))

        assertThat(candidates).containsExactly(
            AccountTransferCandidate(
                outgoingTransactionId = 1,
                incomingTransactionId = 2,
                amountWon = 45_000
            )
        )
    }

    @Test
    fun findAccountTransferCandidatesIgnoresAlreadyConfirmedTransactions() {
        val outgoing = transaction(
            id = 1,
            occurredAt = Instant.parse("2026-07-06T01:00:00Z"),
            amountWon = 45_000,
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.TRANSFER,
            status = TransactionStatus.USER_EDITED
        )
        val incoming = transaction(
            id = 2,
            occurredAt = Instant.parse("2026-07-06T01:02:00Z"),
            amountWon = 45_000,
            direction = TransactionDirection.INCOME,
            type = TransactionType.INCOME
        )

        val candidates = findAccountTransferCandidates(listOf(outgoing, incoming))

        assertThat(candidates).isEmpty()
    }

    private fun transaction(
        id: Long,
        occurredAt: Instant,
        amountWon: Long,
        direction: TransactionDirection,
        type: TransactionType,
        status: TransactionStatus = TransactionStatus.NEEDS_REVIEW
    ) = MoneyTransaction(
        id = id,
        occurredAt = occurredAt,
        amount = MoneyAmount(amountWon),
        direction = direction,
        type = type,
        category = Category.OTHER,
        paymentMethod = null,
        merchant = null,
        counterparty = null,
        memo = null,
        sourceApp = "test",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash-$id",
        status = status,
        confidence = 0.8,
        monthKey = YearMonth.of(2026, 7)
    )
}
