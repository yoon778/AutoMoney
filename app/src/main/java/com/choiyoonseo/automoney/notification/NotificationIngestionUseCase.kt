package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.data.repository.DuplicateNotificationException
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.NotificationParser
import com.choiyoonseo.automoney.domain.parser.ParseResult
import com.choiyoonseo.automoney.domain.parser.TransactionDraft
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDecision
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector

class NotificationIngestionUseCase(
    private val parser: NotificationParser,
    private val categorizationEngine: CategorizationEngine,
    private val duplicateDetector: DuplicateDetector,
    private val repository: MoneyRepository
) {
    suspend fun ingest(snapshot: NotificationSnapshot): IngestionResult {
        val parsed = parser.parse(snapshot)
        if (parsed !is ParseResult.Parsed) {
            val reason = (parsed as? ParseResult.Ignored)?.reason ?: "not parsed"
            return IngestionResult.Ignored(reason)
        }

        val withRules = categorizationEngine
            .applyRules(parsed.draft, repository.enabledRules())
            .routeLowConfidenceToReview()
        val duplicateDecision = duplicateDetector.detect(
            candidate = withRules,
            existing = repository.recentNotificationTransactions(limit = 50)
        )

        if (duplicateDecision == DuplicateDecision.DUPLICATE) {
            return IngestionResult.Duplicate(withRules.type)
        }

        val finalDraft = when (duplicateDecision) {
            DuplicateDecision.SUSPECTED -> withRules.copy(
                status = TransactionStatus.NEEDS_REVIEW,
                reviewReason = ReviewReason.DUPLICATE_SUSPECTED
            )
            DuplicateDecision.UNIQUE,
            DuplicateDecision.DUPLICATE -> withRules
        }

        try {
            val saved = repository.saveNotificationTransaction(
                transaction = finalDraft.toDomain(),
                accountHint = finalDraft.bankAccountHint,
                reviewReason = finalDraft.reviewReason
            )
            return IngestionResult.Saved(finalDraft.type, saved.reviewReason)
        } catch (e: DuplicateNotificationException) {
            return IngestionResult.Duplicate(finalDraft.type)
        }
    }
}

sealed interface IngestionResult {
    data class Saved(
        val transactionType: TransactionType,
        val reviewReason: ReviewReason?
    ) : IngestionResult

    data class Duplicate(
        val transactionType: TransactionType?
    ) : IngestionResult

    data class Ignored(
        val reason: String
    ) : IngestionResult
}

private fun TransactionDraft.toDomain(): MoneyTransaction {
    return MoneyTransaction(
        occurredAt = occurredAt,
        amount = amount,
        direction = direction,
        type = type,
        category = category,
        paymentMethod = paymentMethod,
        merchant = merchant,
        counterparty = counterparty,
        memo = memo,
        sourceApp = sourceApp,
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = sourceNotificationHash,
        status = status,
        confidence = confidence,
        monthKey = monthKey
    )
}

private fun TransactionDraft.routeLowConfidenceToReview(): TransactionDraft {
    if (status != TransactionStatus.AUTO_CONFIRMED) return this
    if (confidence >= AUTO_REVIEW_CONFIDENCE_THRESHOLD) return this
    return copy(
        status = TransactionStatus.NEEDS_REVIEW,
        reviewReason = ReviewReason.LOW_CONFIDENCE_CATEGORY
    )
}

private const val AUTO_REVIEW_CONFIDENCE_THRESHOLD = 0.7
