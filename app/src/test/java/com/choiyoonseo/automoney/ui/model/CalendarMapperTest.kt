package com.choiyoonseo.automoney.ui.model

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
import java.time.LocalDate
import java.time.YearMonth

class CalendarMapperTest {

    @Test
    fun transactionsToSpendCalendarGroupsMonthlyExpensesByDay() {
        val month = YearMonth.of(2026, 6)
        val calendar = transactionsToSpendCalendar(
            month = month,
            transactions = listOf(
                transaction(
                    occurredAt = "2026-06-15T01:00:00Z",
                    amountWon = 6100,
                    type = TransactionType.EXPENSE,
                    category = Category.CAFE_SNACK,
                    month = month
                ),
                transaction(
                    occurredAt = "2026-06-15T02:00:00Z",
                    amountWon = 4800,
                    type = TransactionType.WALLET_SPEND,
                    category = Category.FOOD,
                    month = month
                ),
                transaction(
                    occurredAt = "2026-06-16T01:00:00Z",
                    amountWon = 10000,
                    type = TransactionType.WALLET_TOPUP,
                    category = null,
                    month = month
                ),
                transaction(
                    occurredAt = "2026-05-31T01:00:00Z",
                    amountWon = 99000,
                    type = TransactionType.EXPENSE,
                    category = Category.SHOPPING,
                    month = YearMonth.of(2026, 5)
                )
            )
        )

        assertThat(calendar.monthTitle).isEqualTo("2026년 6월")
        assertThat(calendar.daysInMonth).isEqualTo(30)
        assertThat(calendar.firstWeekdayOffset).isEqualTo(1)
        assertThat(calendar.dailySpends).containsExactly(
            DailySpendUi(day = 15, amountWon = 10900, label = "2건")
        )
    }

    @Test
    fun transactionsToSpendCalendarUsesCategoryLabelForSingleDailySpend() {
        val month = YearMonth.of(2026, 6)
        val calendar = transactionsToSpendCalendar(
            month = month,
            transactions = listOf(
                transaction(
                    occurredAt = "2026-06-21T01:00:00Z",
                    amountWon = 22000,
                    type = TransactionType.FIXED_EXPENSE,
                    category = Category.TRANSPORT,
                    month = month
                )
            )
        )

        assertThat(calendar.spendForDay(21)).isEqualTo(
            DailySpendUi(day = 21, amountWon = 22000, label = "교통비")
        )
    }

    @Test
    fun transactionsToSpendCalendarIgnoresNeedsReviewTransactions() {
        val month = YearMonth.of(2026, 7)
        val calendar = transactionsToSpendCalendar(
            month = month,
            transactions = listOf(
                transaction(
                    occurredAt = "2026-07-06T01:00:00Z",
                    amountWon = 30000,
                    type = TransactionType.EXPENSE,
                    category = Category.FOOD,
                    month = month,
                    status = TransactionStatus.NEEDS_REVIEW
                ),
                transaction(
                    occurredAt = "2026-07-06T02:00:00Z",
                    amountWon = 6100,
                    type = TransactionType.EXPENSE,
                    category = Category.CAFE_SNACK,
                    month = month,
                    status = TransactionStatus.USER_EDITED
                )
            )
        )

        assertThat(calendar.spendForDay(6)).isEqualTo(
            DailySpendUi(day = 6, amountWon = 6100, label = "카페/간식")
        )
    }

    @Test
    fun currentMonthCalendarSelectsTodayByDefault() {
        val month = YearMonth.of(2026, 7)
        val calendar = transactionsToSpendCalendar(month = month, transactions = emptyList())

        assertThat(calendar.defaultSelectedDay(LocalDate.of(2026, 7, 6))).isEqualTo(6)
    }

    @Test
    fun pastMonthCalendarSelectsLastSpendOrFirstDay() {
        val month = YearMonth.of(2026, 6)
        val calendar = transactionsToSpendCalendar(
            month = month,
            transactions = listOf(
                transaction(
                    occurredAt = "2026-06-15T01:00:00Z",
                    amountWon = 6100,
                    type = TransactionType.EXPENSE,
                    category = Category.CAFE_SNACK,
                    month = month
                )
            )
        )

        assertThat(calendar.defaultSelectedDay(LocalDate.of(2026, 7, 6))).isEqualTo(15)
        assertThat(
            transactionsToSpendCalendar(month = month, transactions = emptyList())
                .defaultSelectedDay(LocalDate.of(2026, 7, 6))
        ).isEqualTo(1)
    }

    @Test
    fun transactionsToSpendCalendarUsesOnlyReportableActualExpenses() {
        val month = YearMonth.of(2026, 7)
        val calendar = transactionsToSpendCalendar(
            month = month,
            transactions = listOf(
                transaction(
                    occurredAt = "2026-07-08T01:00:00Z",
                    amountWon = 6_100,
                    type = TransactionType.EXPENSE,
                    category = Category.CAFE_SNACK,
                    month = month
                ),
                transaction(
                    occurredAt = "2026-07-08T02:00:00Z",
                    amountWon = 10_000,
                    type = TransactionType.WALLET_TOPUP,
                    category = null,
                    month = month
                ),
                transaction(
                    occurredAt = "2026-07-08T03:00:00Z",
                    amountWon = 20_000,
                    type = TransactionType.SAVING,
                    category = Category.SAVING,
                    month = month
                ),
                transaction(
                    occurredAt = "2026-07-08T04:00:00Z",
                    amountWon = 30_000,
                    type = TransactionType.TRANSFER,
                    category = null,
                    month = month
                ),
                transaction(
                    occurredAt = "2026-07-08T05:00:00Z",
                    amountWon = 888_000,
                    type = TransactionType.EXPENSE,
                    category = Category.SHOPPING,
                    month = month,
                    status = TransactionStatus.EXCLUDED
                ),
                transaction(
                    occurredAt = "2026-07-08T06:00:00Z",
                    amountWon = 777_000,
                    type = TransactionType.EXPENSE,
                    category = Category.FOOD,
                    month = month,
                    status = TransactionStatus.NEEDS_REVIEW
                )
            )
        )

        assertThat(calendar.spendForDay(8)?.amountWon).isEqualTo(6_100)
    }

    private fun transaction(
        occurredAt: String,
        amountWon: Long,
        type: TransactionType,
        category: Category?,
        month: YearMonth,
        status: TransactionStatus = TransactionStatus.AUTO_CONFIRMED
    ) = MoneyTransaction(
        occurredAt = Instant.parse(occurredAt),
        amount = MoneyAmount(amountWon),
        direction = type.defaultDirection.takeIf { it != TransactionDirection.NEUTRAL }
            ?: TransactionDirection.NEUTRAL,
        type = type,
        category = category,
        paymentMethod = "테스트",
        merchant = "테스트 상점",
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = SourceType.MANUAL,
        sourceNotificationHash = null,
        status = status,
        confidence = 1.0,
        monthKey = month
    )
}
