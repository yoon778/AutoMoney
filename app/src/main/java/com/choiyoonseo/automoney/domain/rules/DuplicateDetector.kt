package com.choiyoonseo.automoney.domain.rules

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.parser.TransactionDraft
import java.time.Duration

enum class DuplicateDecision {
    UNIQUE,
    SUSPECTED,
    DUPLICATE
}

class DuplicateDetector {
    fun detect(candidate: TransactionDraft, existing: List<MoneyTransaction>): DuplicateDecision {
        if (existing.any { it.sourceNotificationHash == candidate.sourceNotificationHash }) {
            return DuplicateDecision.DUPLICATE
        }

        val suspected = existing.any { transaction ->
            transaction.amount == candidate.amount &&
                transaction.merchant == candidate.merchant &&
                Duration.between(transaction.occurredAt, candidate.occurredAt).abs() <= Duration.ofMinutes(5)
        }

        return if (suspected) DuplicateDecision.SUSPECTED else DuplicateDecision.UNIQUE
    }
}

