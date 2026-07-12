package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import org.junit.Test

class CategoryBudgetUsageTest {
    @Test
    fun tracksOnlyMatchingCategoryExpenses() {
        val usages = buildCategoryBudgetUsages(
            plans = listOf(
                MonthlyPlanItem(
                    label = "식비",
                    amountWon = 400_000,
                    type = MonthlyPlanItemType.BUDGET,
                    category = Category.FOOD
                )
            ),
            transactions = listOf(
                transaction(120_000, Category.FOOD),
                transaction(30_000, Category.HOBBY),
                transaction(50_000, Category.FOOD, TransactionType.SAVING),
                transaction(10_000, null)
            )
        )

        assertThat(usages.single().spentWon).isEqualTo(120_000)
        assertThat(usages.single().remainingWon).isEqualTo(280_000)
        assertThat(usages.single().usedRatio).isEqualTo(0.3f)
    }

    @Test
    fun tracksCustomCategoryByIdAndClampsOverBudgetRatio() {
        val usages = buildCategoryBudgetUsages(
            plans = listOf(
                MonthlyPlanItem(
                    label = "데이트비용",
                    amountWon = 100_000,
                    type = MonthlyPlanItemType.BUDGET,
                    category = Category.OTHER,
                    customCategoryId = 7,
                    customCategoryName = "데이트비용"
                )
            ),
            transactions = listOf(
                transaction(120_000, Category.OTHER, customCategoryId = 7),
                transaction(30_000, Category.OTHER, customCategoryId = 8)
            )
        )

        assertThat(usages.single().spentWon).isEqualTo(120_000)
        assertThat(usages.single().remainingWon).isEqualTo(-20_000)
        assertThat(usages.single().usedRatio).isEqualTo(1f)
    }

    private fun transaction(
        amountWon: Long,
        category: Category?,
        type: TransactionType = TransactionType.EXPENSE,
        customCategoryId: Long? = null
    ) = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-12T00:00:00Z"),
        amount = MoneyAmount(amountWon),
        direction = type.defaultDirection,
        type = type,
        category = category,
        paymentMethod = null,
        merchant = null,
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = SourceType.MANUAL,
        sourceNotificationHash = null,
        status = TransactionStatus.USER_EDITED,
        confidence = 1.0,
        monthKey = YearMonth.of(2026, 7),
        customCategoryId = customCategoryId
    )
}
