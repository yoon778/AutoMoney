package com.choiyoonseo.automoney.domain.rules

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.RuleAction
import com.choiyoonseo.automoney.domain.model.RuleMatchType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.parser.TransactionDraft
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import org.junit.Test

class CategorizationEngineTest {
    private val engine = CategorizationEngine()

    @Test
    fun invalidCategoryRuleIsIgnored() {
        val draft = draft()

        val result = engine.applyRules(
            draft,
            listOf(
                rule(
                    action = RuleAction.SET_CATEGORY,
                    targetValue = "OLD_CATEGORY"
                )
            )
        )

        assertThat(result).isEqualTo(draft)
    }

    @Test
    fun invalidTransactionTypeRuleIsIgnored() {
        val draft = draft()

        val result = engine.applyRules(
            draft,
            listOf(
                rule(
                    action = RuleAction.SET_TRANSACTION_TYPE,
                    targetValue = "OLD_TYPE"
                )
            )
        )

        assertThat(result).isEqualTo(draft)
    }

    private fun rule(
        action: RuleAction,
        targetValue: String
    ) = Rule(
        matchType = RuleMatchType.MERCHANT,
        matchValue = "store",
        action = action,
        targetValue = targetValue,
        enabled = true
    )

    private fun draft() = TransactionDraft(
        occurredAt = Instant.parse("2026-07-08T01:00:00Z"),
        amount = MoneyAmount(10_000),
        direction = TransactionDirection.EXPENSE,
        type = TransactionType.EXPENSE,
        category = Category.OTHER,
        paymentMethod = "KB",
        merchant = "store",
        counterparty = null,
        memo = "store",
        sourceApp = "com.kbstar.kbbank",
        sourceNotificationHash = "hash",
        status = TransactionStatus.AUTO_CONFIRMED,
        confidence = 0.9,
        monthKey = YearMonth.of(2026, 7),
        reviewReason = null
    )
}
