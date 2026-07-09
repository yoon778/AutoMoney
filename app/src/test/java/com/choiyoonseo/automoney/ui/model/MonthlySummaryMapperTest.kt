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
import java.time.YearMonth
import java.time.ZoneId

class MonthlySummaryMapperTest {
    @Test
    fun monthlySummarySeparatesRemainingMoneyFromActualSaving() {
        val month = YearMonth.of(2026, 7)
        val expenseOnly = transactionsToMonthlySummary(
            month = month,
            transactions = listOf(
                tx("2026-07-01T01:00:00Z", 30_000, TransactionType.EXPENSE, Category.FOOD, month)
            ),
            reviewCount = 0
        )

        assertThat(expenseOnly.expenseWon).isEqualTo(30_000)
        assertThat(expenseOnly.savingWon).isEqualTo(0)
        assertThat(expenseOnly.netWon).isEqualTo(-30_000)

        val withSaving = transactionsToMonthlySummary(
            month = month,
            transactions = listOf(
                tx("2026-07-01T01:00:00Z", 100_000, TransactionType.INCOME, Category.SALARY, month),
                tx("2026-07-02T01:00:00Z", 30_000, TransactionType.EXPENSE, Category.FOOD, month),
                tx("2026-07-03T01:00:00Z", 20_000, TransactionType.SAVING, Category.SAVING, month)
            ),
            reviewCount = 0
        )

        assertThat(withSaving.incomeWon).isEqualTo(100_000)
        assertThat(withSaving.expenseWon).isEqualTo(30_000)
        assertThat(withSaving.savingWon).isEqualTo(20_000)
        assertThat(withSaving.netWon).isEqualTo(50_000)
        assertThat(withSaving.savingsRatePercent).isEqualTo(20)
    }

    @Test
    fun transactionsToMonthlySummaryCalculatesMoneyMetricsFromExpenseTypesOnly() {
        val month = YearMonth.of(2026, 6)
        val summary = transactionsToMonthlySummary(
            month = month,
            transactions = listOf(
                tx("2026-06-01T01:00:00Z", 100000, TransactionType.INCOME, Category.SALARY, month),
                tx("2026-06-02T01:00:00Z", 6000, TransactionType.WALLET_SPEND, Category.CAFE_SNACK, month),
                tx("2026-06-03T01:00:00Z", 4000, TransactionType.EXPENSE, Category.FOOD, month),
                tx("2026-06-04T01:00:00Z", 10000, TransactionType.WALLET_TOPUP, null, month),
                tx("2026-06-05T01:00:00Z", 20000, TransactionType.SAVING, Category.SAVING, month),
                tx("2026-05-31T01:00:00Z", 50000, TransactionType.EXPENSE, Category.SHOPPING, YearMonth.of(2026, 5))
            ),
            reviewCount = 2
        )

        assertThat(summary.monthTitle).isEqualTo("6월 돈 흐름")
        assertThat(summary.incomeWon).isEqualTo(100000)
        assertThat(summary.expenseWon).isEqualTo(10000)
        assertThat(summary.savingWon).isEqualTo(20000)
        assertThat(summary.netWon).isEqualTo(70000)
        assertThat(summary.savingsRatePercent).isEqualTo(20)
        assertThat(summary.homeSnapshot.metrics.map { it.value }).containsExactly("10,000원", "20%", "2건")
    }

    @Test
    fun transactionsToMonthlySummaryBuildsCategoryBarsAndRecentRows() {
        val month = YearMonth.of(2026, 6)
        val summary = transactionsToMonthlySummary(
            month = month,
            transactions = listOf(
                tx("2026-06-01T01:00:00Z", 100000, TransactionType.INCOME, Category.SALARY, month),
                tx("2026-06-02T01:00:00Z", 6000, TransactionType.WALLET_SPEND, Category.CAFE_SNACK, month, "스타벅스"),
                tx("2026-06-03T01:00:00Z", 4000, TransactionType.EXPENSE, Category.FOOD, month, "김밥천국"),
                tx("2026-06-04T01:00:00Z", 10000, TransactionType.WALLET_TOPUP, null, month, "네이버페이")
            ),
            reviewCount = 0
        )

        assertThat(summary.categorySpends).containsExactly(
            CategorySpendUi("카페/간식", 6000, 0.6f),
            CategorySpendUi("식비", 4000, 0.4f)
        ).inOrder()
        assertThat(summary.homeSnapshot.recentTransactions.map { it.merchant })
            .containsExactly("김밥천국", "스타벅스", "월급")
    }

    @Test
    fun transactionsToMonthlySummaryIgnoresNeedsReviewTransactions() {
        val month = YearMonth.of(2026, 7)
        val summary = transactionsToMonthlySummary(
            month = month,
            transactions = listOf(
                tx("2026-07-01T01:00:00Z", 100000, TransactionType.INCOME, Category.SALARY, month),
                tx(
                    occurredAt = "2026-07-02T01:00:00Z",
                    amountWon = 30000,
                    type = TransactionType.EXPENSE,
                    category = Category.FOOD,
                    month = month,
                    merchant = "검토 전 식당",
                    status = TransactionStatus.NEEDS_REVIEW
                ),
                tx("2026-07-03T01:00:00Z", 7000, TransactionType.EXPENSE, Category.CAFE_SNACK, month, "확정 카페")
            ),
            reviewCount = 1
        )

        assertThat(summary.expenseWon).isEqualTo(7000)
        assertThat(summary.categorySpends).containsExactly(CategorySpendUi("카페/간식", 7000, 1.0f))
        assertThat(summary.homeSnapshot.recentTransactions.map { it.merchant })
            .doesNotContain("검토 전 식당")
    }

    @Test
    fun transactionsToMonthlySummaryUsesOnlyReportableIncomeExpenseAndSavingBuckets() {
        val month = YearMonth.of(2026, 7)
        val summary = transactionsToMonthlySummary(
            month = month,
            transactions = listOf(
                tx("2026-07-01T01:00:00Z", 100_000, TransactionType.INCOME, Category.SALARY, month, "Salary"),
                tx("2026-07-02T01:00:00Z", 20_000, TransactionType.EXPENSE, Category.FOOD, month, "Lunch"),
                tx("2026-07-03T01:00:00Z", 6_000, TransactionType.WALLET_SPEND, Category.CAFE_SNACK, month, "Cafe"),
                tx("2026-07-04T01:00:00Z", 10_000, TransactionType.SAVING, Category.SAVING, month, "Savings"),
                tx("2026-07-05T01:00:00Z", 5_000, TransactionType.INVESTMENT, Category.STOCK, month, "Stock"),
                tx("2026-07-06T01:00:00Z", 30_000, TransactionType.WALLET_TOPUP, null, month, "NaverPay"),
                tx("2026-07-07T01:00:00Z", 40_000, TransactionType.TRANSFER, null, month, "AccountMove"),
                tx("2026-07-08T01:00:00Z", 7_000, TransactionType.SETTLEMENT, null, month, "DutchPay"),
                tx(
                    occurredAt = "2026-07-09T01:00:00Z",
                    amountWon = 999_000,
                    type = TransactionType.INCOME,
                    category = Category.SALARY,
                    month = month,
                    merchant = "ExcludedIncome",
                    status = TransactionStatus.EXCLUDED
                ),
                tx(
                    occurredAt = "2026-07-10T01:00:00Z",
                    amountWon = 888_000,
                    type = TransactionType.EXPENSE,
                    category = Category.SHOPPING,
                    month = month,
                    merchant = "ExcludedExpense",
                    status = TransactionStatus.EXCLUDED
                ),
                tx(
                    occurredAt = "2026-07-11T01:00:00Z",
                    amountWon = 777_000,
                    type = TransactionType.EXPENSE,
                    category = Category.FOOD,
                    month = month,
                    merchant = "ReviewExpense",
                    status = TransactionStatus.NEEDS_REVIEW
                )
            ),
            reviewCount = 1
        )

        assertThat(summary.incomeWon).isEqualTo(100_000)
        assertThat(summary.expenseWon).isEqualTo(26_000)
        assertThat(summary.savingWon).isEqualTo(15_000)
        assertThat(summary.netWon).isEqualTo(59_000)
        assertThat(summary.categorySpends.map { it.amountWon }).containsExactly(20_000L, 6_000L)
        assertThat(summary.homeSnapshot.recentTransactions.map { it.merchant })
            .containsNoneOf("NaverPay", "AccountMove", "DutchPay", "ExcludedIncome", "ExcludedExpense", "ReviewExpense")
    }

    @Test
    fun transactionsToRowsBuildsFullTransactionRowsWithoutTopups() {
        val month = YearMonth.of(2026, 7)
        val rows = transactionsToRows(
            transactions = listOf(
                tx("2026-07-01T01:00:00Z", 6100, TransactionType.EXPENSE, Category.CAFE_SNACK, month, "스타벅스", id = 1),
                tx("2026-07-01T02:00:00Z", 10000, TransactionType.WALLET_TOPUP, null, month, "네이버페이", id = 2),
                tx("2026-07-01T03:00:00Z", 4800, TransactionType.EXPENSE, Category.FOOD, month, "GS25 합정역점", id = 3)
            ),
            limit = 20
        )

        assertThat(rows.map { it.merchant }).containsExactly(
            "GS25 합정역점",
            "스타벅스"
        ).inOrder()
        assertThat(rows.map { it.amountWon }).containsExactly(-4800L, -6100L).inOrder()
        assertThat(rows.map { it.id }).containsExactly(3L, 1L).inOrder()
    }

    @Test
    fun transactionsToRowsMarksExcludedRowsForSubduedStyle() {
        val month = YearMonth.of(2026, 7)
        val rows = transactionsToRows(
            transactions = listOf(
                tx(
                    occurredAt = "2026-07-01T01:00:00Z",
                    amountWon = 10000,
                    type = TransactionType.EXCLUDED,
                    category = null,
                    month = month,
                    merchant = "내 계좌 이동",
                    id = 4,
                    status = TransactionStatus.EXCLUDED
                )
            )
        )

        assertThat(rows.single().isExcluded).isTrue()
    }

    @Test
    fun transactionsToRowsAttachSourceAppInfoFromNotificationPackage() {
        val month = YearMonth.of(2026, 7)
        val rows = transactionsToRows(
            transactions = listOf(
                tx(
                    occurredAt = "2026-07-01T01:00:00Z",
                    amountWon = 6100,
                    type = TransactionType.EXPENSE,
                    category = Category.CAFE_SNACK,
                    month = month,
                    merchant = "\uc2a4\ud0c0\ubc85\uc2a4",
                    id = 6,
                    sourceApp = "com.kbstar.kbbank",
                    sourceType = SourceType.NOTIFICATION
                )
            )
        )

        assertThat(rows.single().sourceApp).isEqualTo(
            SourceAppUi(
                packageName = "com.kbstar.kbbank",
                displayName = "\uad6d\ubbfc\uc740\ud589",
                badgeText = "KB"
            )
        )
        assertThat(rows.single().sourceLabel).isEqualTo("\uc790\ub3d9")
    }

    @Test
    fun transactionsToRowsDoesNotMarkManualRowsAsAutomatic() {
        val month = YearMonth.of(2026, 7)
        val rows = transactionsToRows(
            transactions = listOf(
                tx(
                    occurredAt = "2026-07-01T01:00:00Z",
                    amountWon = 6100,
                    type = TransactionType.EXPENSE,
                    category = Category.CAFE_SNACK,
                    month = month,
                    merchant = "\uc2a4\ud0c0\ubc85\uc2a4",
                    id = 6,
                    sourceType = SourceType.MANUAL
                )
            )
        )

        assertThat(rows.single().sourceLabel).isNull()
    }

    @Test
    fun transactionsToRowsHidesNeedsReviewUntilResolved() {
        val month = YearMonth.of(2026, 7)
        val rows = transactionsToRows(
            transactions = listOf(
                tx(
                    occurredAt = "2026-07-01T01:00:00Z",
                    amountWon = 10000,
                    type = TransactionType.TRANSFER,
                    category = null,
                    month = month,
                    merchant = "검토 전 송금",
                    id = 1,
                    status = TransactionStatus.NEEDS_REVIEW
                ),
                tx(
                    occurredAt = "2026-07-01T02:00:00Z",
                    amountWon = 6100,
                    type = TransactionType.EXPENSE,
                    category = Category.CAFE_SNACK,
                    month = month,
                    merchant = "확정 카페",
                    id = 2,
                    status = TransactionStatus.USER_EDITED
                )
            )
        )

        assertThat(rows.map { it.merchant }).containsExactly("확정 카페")
    }

    @Test
    fun transactionsToDateSectionsGroupsByLocalDateDescendingThenTimeDescending() {
        val month = YearMonth.of(2026, 7)
        val sections = transactionsToDateSections(
            transactions = listOf(
                tx("2026-07-01T01:00:00Z", 6100, TransactionType.EXPENSE, Category.CAFE_SNACK, month, "첫째 카페", id = 1),
                tx("2026-07-03T03:00:00Z", 9000, TransactionType.EXPENSE, Category.FOOD, month, "셋째 늦은 식당", id = 2),
                tx("2026-07-03T01:00:00Z", 4800, TransactionType.EXPENSE, Category.FOOD, month, "셋째 이른 편의점", id = 3),
                tx(
                    occurredAt = "2026-07-04T01:00:00Z",
                    amountWon = 10_000,
                    type = TransactionType.TRANSFER,
                    category = null,
                    month = month,
                    merchant = "검토 전 송금",
                    id = 4,
                    status = TransactionStatus.NEEDS_REVIEW
                )
            ),
            zoneId = ZoneId.of("UTC")
        )

        assertThat(sections.map { it.date.toString() }).containsExactly("2026-07-03", "2026-07-01").inOrder()
        assertThat(sections[0].dateLabel).isEqualTo("7월 3일")
        assertThat(sections[0].rows.map { it.merchant })
            .containsExactly("셋째 늦은 식당", "셋째 이른 편의점")
            .inOrder()
        assertThat(sections.flatMap { it.rows }.map { it.merchant }).doesNotContain("검토 전 송금")
    }

    @Test
    fun transactionsToRowsUsesMemoAndTypeWhenTitleFieldsAreBlank() {
        val month = YearMonth.of(2026, 7)
        val rows = transactionsToRows(
            transactions = listOf(
                tx(
                    occurredAt = "2026-07-01T01:00:00Z",
                    amountWon = 30000,
                    type = TransactionType.TRANSFER,
                    category = null,
                    month = month,
                    merchant = "",
                    id = 7,
                    status = TransactionStatus.USER_EDITED,
                    memo = "\uce5c\uad6c \uc815\uc0b0"
                ),
                tx("2026-07-01T02:00:00Z", 10000, TransactionType.INCOME, Category.SALARY, month, merchant = "", id = 8)
            )
        )

        assertThat(rows.map { it.merchant }).containsExactly(
            "\uc6d4\uae09",
            "\uce5c\uad6c \uc815\uc0b0"
        ).inOrder()
    }

    private fun tx(
        occurredAt: String,
        amountWon: Long,
        type: TransactionType,
        category: Category?,
        month: YearMonth,
        merchant: String? = null,
        id: Long = 0,
        status: TransactionStatus = TransactionStatus.AUTO_CONFIRMED,
        memo: String? = null,
        sourceApp: String? = null,
        sourceType: SourceType = SourceType.MANUAL
    ) = MoneyTransaction(
        id = id,
        occurredAt = Instant.parse(occurredAt),
        amount = MoneyAmount(amountWon),
        direction = type.defaultDirection,
        type = type,
        category = category,
        paymentMethod = "테스트",
        merchant = merchant,
        counterparty = null,
        memo = memo,
        sourceApp = sourceApp,
        sourceType = sourceType,
        sourceNotificationHash = null,
        status = status,
        confidence = 1.0,
        monthKey = month
    )
}
