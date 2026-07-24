package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.data.repository.NotificationHistoryRepository
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryReason
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryRecord
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryStatus
import com.choiyoonseo.automoney.domain.notificationhistory.SafeNotificationAmountExtractor
import java.time.Clock
import kotlinx.coroutines.CancellationException

class NotificationHistoryRecorder(
    private val repository: NotificationHistoryRepository,
    private val amountExtractor: SafeNotificationAmountExtractor = SafeNotificationAmountExtractor(),
    private val clock: Clock = Clock.systemUTC()
) {
    suspend fun recordResult(prepared: PreparedNotification, result: IngestionResult): Boolean =
        bestEffort {
            repository.recordAndPrune(result.toHistoryRecord(prepared))
        }

    @Suppress("UNUSED_PARAMETER")
    suspend fun recordError(
        prepared: PreparedNotification,
        throwable: RuntimeException? = null
    ): Boolean = bestEffort {
        repository.recordAndPrune(
            NotificationHistoryRecord(
                packageName = prepared.snapshot.packageName,
                sourceLabel = prepared.sourceLabel,
                receivedAt = clock.instant(),
                status = NotificationHistoryStatus.ERROR,
                transactionType = null,
                amountWon = amountExtractor.extract(prepared.snapshot),
                reason = NotificationHistoryReason.PROCESSING_ERROR,
                linkedTransactionId = null
            )
        )
    }

    internal suspend fun bestEffort(block: suspend () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: RuntimeException) {
            false
        }

    private fun IngestionResult.toHistoryRecord(
        prepared: PreparedNotification
    ): NotificationHistoryRecord {
        val status = when (this) {
            is IngestionResult.Saved -> if (reviewReason == null) {
                NotificationHistoryStatus.SAVED
            } else {
                NotificationHistoryStatus.REVIEW
            }
            is IngestionResult.Duplicate -> NotificationHistoryStatus.DUPLICATE
            is IngestionResult.Ignored -> NotificationHistoryStatus.IGNORED
        }
        val reason = when (status) {
            NotificationHistoryStatus.SAVED -> NotificationHistoryReason.SAVED_AUTOMATICALLY
            NotificationHistoryStatus.REVIEW -> NotificationHistoryReason.REVIEW_REQUIRED
            NotificationHistoryStatus.DUPLICATE -> NotificationHistoryReason.DUPLICATE_EVENT
            NotificationHistoryStatus.IGNORED -> NotificationHistoryReason.PARSER_IGNORED
            NotificationHistoryStatus.ERROR -> NotificationHistoryReason.PROCESSING_ERROR
            NotificationHistoryStatus.RESOLVED_MANUALLY -> NotificationHistoryReason.MANUAL_RECORD_CREATED
        }
        return NotificationHistoryRecord(
            packageName = prepared.snapshot.packageName,
            sourceLabel = prepared.sourceLabel,
            receivedAt = clock.instant(),
            status = status,
            transactionType = when (this) {
                is IngestionResult.Saved -> transactionType
                is IngestionResult.Duplicate -> transactionType
                is IngestionResult.Ignored -> null
            },
            amountWon = when (this) {
                is IngestionResult.Saved -> amountWon
                is IngestionResult.Duplicate -> amountWon
                is IngestionResult.Ignored -> null
            } ?: amountExtractor.extract(prepared.snapshot),
            reason = reason,
            linkedTransactionId = (this as? IngestionResult.Saved)?.transactionId
        )
    }
}
