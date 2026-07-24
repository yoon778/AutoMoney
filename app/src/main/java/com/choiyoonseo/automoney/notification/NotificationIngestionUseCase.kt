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
import com.choiyoonseo.automoney.domain.parser.notificationEventIdentityHash
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDecision
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector
import com.choiyoonseo.automoney.domain.refund.RefundMatchDecision
import kotlinx.coroutines.CancellationException

class NotificationIngestionUseCase(
    private val parser: NotificationParser,
    private val genericParser: NotificationParser,
    private val categorizationEngine: CategorizationEngine,
    private val duplicateDetector: DuplicateDetector,
    private val repository: MoneyRepository,
    private val refundAutoLink: suspend (Long) -> RefundMatchDecision = {
        RefundMatchDecision.NoMatch
    }
) {
    suspend fun ingest(
        snapshot: NotificationSnapshot,
        sourceAccess: NotificationSourceAccess
    ): IngestionResult {
        if (sourceAccess == NotificationSourceAccess.BLOCKED) {
            return IngestionResult.Ignored("blocked source")
        }
        val selectedParser = when (sourceAccess) {
            NotificationSourceAccess.TRUSTED -> parser
            NotificationSourceAccess.SELECTED_UNVERIFIED -> genericParser
            NotificationSourceAccess.BLOCKED -> error("handled above")
        }
        val parsed = selectedParser.parse(snapshot)
        if (parsed !is ParseResult.Parsed) {
            val reason = (parsed as? ParseResult.Ignored)?.reason ?: "not parsed"
            return IngestionResult.Ignored(reason)
        }

        val eventIdentified = parsed.draft.copy(
            sourceNotificationHash = notificationEventIdentityHash(snapshot, parsed.draft)
        )
        val sourceGuarded = if (sourceAccess == NotificationSourceAccess.SELECTED_UNVERIFIED) {
            eventIdentified.forceUnverifiedReview()
        } else {
            eventIdentified
        }
        val withRules = categorizationEngine
            .applyRules(sourceGuarded, repository.enabledRules())
            .routeLowConfidenceToReview()
        val existingTransactions = repository.recentNotificationTransactions(limit = 50)
        val isLegacyDuplicate = existingTransactions.any { transaction ->
            transaction.sourceNotificationHash == parsed.draft.sourceNotificationHash &&
                transaction.amount == eventIdentified.amount &&
                transaction.type == eventIdentified.type &&
                transaction.direction == eventIdentified.direction
        }
        if (isLegacyDuplicate) {
            return IngestionResult.Duplicate(
                transactionType = eventIdentified.type,
                amountWon = eventIdentified.amount.won
            )
        }
        val duplicateDecision = duplicateDetector.detect(
            candidate = withRules,
            existing = existingTransactions
        )

        if (duplicateDecision == DuplicateDecision.DUPLICATE) {
            return IngestionResult.Duplicate(
                transactionType = withRules.type,
                amountWon = withRules.amount.won
            )
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
            val effectiveReviewReason = if (finalDraft.type == TransactionType.REFUND) {
                try {
                    when (refundAutoLink(saved.transactionId)) {
                        is RefundMatchDecision.Match -> null
                        RefundMatchDecision.NoMatch,
                        RefundMatchDecision.Ambiguous -> saved.reviewReason
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: RuntimeException) {
                    saved.reviewReason
                }
            } else {
                saved.reviewReason
            }
            return IngestionResult.Saved(
                transactionType = finalDraft.type,
                reviewReason = effectiveReviewReason,
                transactionId = saved.transactionId,
                amountWon = finalDraft.amount.won
            )
        } catch (e: DuplicateNotificationException) {
            return IngestionResult.Duplicate(
                transactionType = finalDraft.type,
                amountWon = finalDraft.amount.won
            )
        }
    }
}

private fun TransactionDraft.forceUnverifiedReview(): TransactionDraft = copy(
    category = null,
    paymentMethod = null,
    merchant = null,
    counterparty = null,
    memo = null,
    bankAccountHint = null,
    status = TransactionStatus.NEEDS_REVIEW,
    reviewReason = when (type) {
        TransactionType.EXPENSE,
        TransactionType.FIXED_EXPENSE,
        TransactionType.WALLET_SPEND -> ReviewReason.LOW_CONFIDENCE_CATEGORY
        TransactionType.INCOME -> ReviewReason.INCOME_UNKNOWN
        TransactionType.TRANSFER -> ReviewReason.TRANSFER_UNKNOWN
        TransactionType.REFUND -> ReviewReason.REFUND_OR_CANCEL
        TransactionType.WALLET_TOPUP -> ReviewReason.WALLET_TOPUP
        else -> ReviewReason.LOW_CONFIDENCE_CATEGORY
    }
)

sealed interface IngestionResult {
    data class Saved(
        val transactionType: TransactionType,
        val reviewReason: ReviewReason?,
        val transactionId: Long = 0,
        val amountWon: Long? = null
    ) : IngestionResult

    data class Duplicate(
        val transactionType: TransactionType?,
        val amountWon: Long? = null
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
