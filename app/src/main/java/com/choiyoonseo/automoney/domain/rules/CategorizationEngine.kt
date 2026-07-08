package com.choiyoonseo.automoney.domain.rules

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.RuleAction
import com.choiyoonseo.automoney.domain.model.RuleMatchType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.parser.TransactionDraft

class CategorizationEngine {
    fun applyRules(draft: TransactionDraft, rules: List<Rule>): TransactionDraft {
        return rules.filter { it.enabled }.fold(draft) { current, rule ->
            if (!matches(current, rule)) return@fold current
            when (rule.action) {
                RuleAction.SET_CATEGORY -> current.copy(category = Category.valueOf(rule.targetValue))
                RuleAction.SET_TRANSACTION_TYPE -> {
                    val type = TransactionType.valueOf(rule.targetValue)
                    current.copy(type = type, direction = type.defaultDirection)
                }
                RuleAction.EXCLUDE -> current.copy(
                    type = TransactionType.EXCLUDED,
                    direction = TransactionDirection.NEUTRAL,
                    status = TransactionStatus.EXCLUDED
                )
                RuleAction.MARK_AS_WALLET_TOPUP -> current.copy(
                    type = TransactionType.WALLET_TOPUP,
                    direction = TransactionDirection.NEUTRAL,
                    status = TransactionStatus.NEEDS_REVIEW
                )
                RuleAction.MARK_AS_SETTLEMENT -> current.copy(
                    type = TransactionType.SETTLEMENT,
                    status = TransactionStatus.NEEDS_REVIEW
                )
            }
        }
    }

    private fun matches(draft: TransactionDraft, rule: Rule): Boolean {
        val value = when (rule.matchType) {
            RuleMatchType.MERCHANT -> draft.merchant
            RuleMatchType.COUNTERPARTY -> draft.counterparty
            RuleMatchType.NOTIFICATION_KEYWORD -> draft.memo
            RuleMatchType.PAYMENT_METHOD -> draft.paymentMethod
        } ?: return false
        return value.contains(rule.matchValue, ignoreCase = true)
    }
}

