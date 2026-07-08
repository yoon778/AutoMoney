package com.choiyoonseo.automoney.domain.report

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

class MonthlyReportCalculatorTest {
    @Test
    fun excludesWalletTopupAndCountsWalletSpend() {
        val report = MonthlyReportCalculator().calculate(
            listOf(
                transaction(TransactionType.INCOME, TransactionDirection.INCOME, 100000, Category.SALARY),
                transaction(TransactionType.WALLET_TOPUP, TransactionDirection.NEUTRAL, 10000, null),
                transaction(TransactionType.WALLET_SPEND, TransactionDirection.EXPENSE, 6000, Category.SHOPPING)
            )
        )

        assertThat(report.incomeWon).isEqualTo(100000)
        assertThat(report.expenseWon).isEqualTo(6000)
        assertThat(report.netWon).isEqualTo(94000)
    }

    @Test
    fun excludesSettlementFromIncome() {
        val report = MonthlyReportCalculator().calculate(
            listOf(
                transaction(TransactionType.SETTLEMENT, TransactionDirection.NEUTRAL, 5000, null),
                transaction(TransactionType.EXPENSE, TransactionDirection.EXPENSE, 12000, Category.FOOD)
            )
        )

        assertThat(report.incomeWon).isEqualTo(0)
        assertThat(report.expenseWon).isEqualTo(12000)
    }

    @Test
    fun separatesReportableIncomeExpenseAndSavingMovements() {
        val report = MonthlyReportCalculator().calculate(
            listOf(
                transaction(TransactionType.INCOME, TransactionDirection.INCOME, 100000, Category.SALARY),
                transaction(TransactionType.EXPENSE, TransactionDirection.EXPENSE, 20000, Category.FOOD),
                transaction(TransactionType.WALLET_SPEND, TransactionDirection.EXPENSE, 6000, Category.CAFE_SNACK),
                transaction(TransactionType.SAVING, TransactionDirection.EXPENSE, 10000, Category.SAVING),
                transaction(TransactionType.INVESTMENT, TransactionDirection.EXPENSE, 5000, Category.STOCK),
                transaction(TransactionType.WALLET_TOPUP, TransactionDirection.NEUTRAL, 30000, null),
                transaction(TransactionType.TRANSFER, TransactionDirection.NEUTRAL, 40000, null),
                transaction(
                    TransactionType.EXPENSE,
                    TransactionDirection.EXPENSE,
                    888000,
                    Category.SHOPPING,
                    status = TransactionStatus.EXCLUDED
                ),
                transaction(
                    TransactionType.EXPENSE,
                    TransactionDirection.EXPENSE,
                    777000,
                    Category.FOOD,
                    status = TransactionStatus.NEEDS_REVIEW
                )
            )
        )

        assertThat(report.incomeWon).isEqualTo(100000)
        assertThat(report.expenseWon).isEqualTo(26000)
        assertThat(report.savingWon).isEqualTo(15000)
        assertThat(report.netWon).isEqualTo(59000)
        assertThat(report.categoryExpenseWon)
            .containsExactly(Category.FOOD, 20000L, Category.CAFE_SNACK, 6000L)
    }

    private fun transaction(
        type: TransactionType,
        direction: TransactionDirection,
        amount: Long,
        category: Category?,
        status: TransactionStatus = TransactionStatus.AUTO_CONFIRMED
    ) = MoneyTransaction(
        occurredAt = Instant.parse("2026-06-27T03:47:00Z"),
        amount = MoneyAmount(amount),
        direction = direction,
        type = type,
        category = category,
        paymentMethod = null,
        merchant = null,
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = SourceType.MANUAL,
        sourceNotificationHash = null,
        status = status,
        confidence = 1.0,
        monthKey = YearMonth.of(2026, 6)
    )
}
