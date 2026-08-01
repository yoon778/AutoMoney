package com.choiyoonseo.automoney.domain.report

import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import org.junit.Test

class TransactionReportRulesTest {
    @Test
    fun specialExpenseAffectsCashFlowButNotRegularBudgetUsage() {
        val special = transaction(1, 500_000, TransactionType.SPECIAL_EXPENSE)

        assertThat(special.countsAsSpecialExpense()).isTrue()
        assertThat(special.countsAsCashExpense()).isTrue()
        assertThat(special.countsAsActualExpense()).isFalse()
        assertThat(plannedUseContributions(listOf(special))).isEmpty()
    }

    @Test
    fun linkedRefundReducesSpecialExpenseCashFlow() {
        val contributions = specialExpenseContributions(
            listOf(
                transaction(1, 500_000, TransactionType.SPECIAL_EXPENSE),
                transaction(2, 100_000, TransactionType.REFUND, refundParentId = 1)
            )
        )

        assertThat(contributions.single().amountWon).isEqualTo(400_000)
    }

    @Test
    fun kbankPaymentMinusCashbackEquals5994() {
        val contributions = plannedUseContributions(
            listOf(
                transaction(1, 6_000, TransactionType.EXPENSE),
                transaction(2, 6, TransactionType.REFUND, refundParentId = 1)
            )
        )

        assertThat(contributions.single().amountWon).isEqualTo(5_994)
    }

    @Test
    fun reviewRefundDoesNotReducePlanUsage() {
        val contributions = plannedUseContributions(
            listOf(
                transaction(1, 6_000, TransactionType.EXPENSE),
                transaction(
                    id = 2,
                    amountWon = 6,
                    type = TransactionType.REFUND,
                    status = TransactionStatus.NEEDS_REVIEW,
                    refundParentId = 1
                )
            )
        )

        assertThat(contributions.single().amountWon).isEqualTo(6_000)
    }

    private fun transaction(
        id: Long,
        amountWon: Long,
        type: TransactionType,
        status: TransactionStatus = TransactionStatus.AUTO_CONFIRMED,
        refundParentId: Long? = null
    ) = MoneyTransaction(
        id = id,
        occurredAt = Instant.parse("2026-07-21T01:00:00Z"),
        amount = MoneyAmount(amountWon),
        direction = type.defaultDirection,
        type = type,
        category = null,
        paymentMethod = null,
        merchant = null,
        counterparty = null,
        memo = null,
        sourceApp = "com.kbankwith.smartbank",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash-$id",
        status = status,
        confidence = 1.0,
        monthKey = YearMonth.of(2026, 7),
        refundParentTransactionId = refundParentId
    )
}
