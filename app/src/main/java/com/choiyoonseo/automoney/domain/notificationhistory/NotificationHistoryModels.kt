package com.choiyoonseo.automoney.domain.notificationhistory

import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant

enum class NotificationHistoryStatus {
    SAVED,
    REVIEW,
    IGNORED,
    DUPLICATE,
    BLOCKED,
    ERROR,
    RESOLVED_MANUALLY
}

enum class NotificationHistoryReason {
    SAVED_AUTOMATICALLY,
    REVIEW_REQUIRED,
    PARSER_IGNORED,
    DUPLICATE_EVENT,
    BLOCKED_SOURCE,
    PROCESSING_ERROR,
    MANUAL_RECORD_CREATED
}

data class NotificationHistoryRecord(
    val id: Long = 0,
    val packageName: String,
    val sourceLabel: String?,
    val receivedAt: Instant,
    val status: NotificationHistoryStatus,
    val transactionType: TransactionType?,
    val amountWon: Long?,
    val reason: NotificationHistoryReason,
    val linkedTransactionId: Long?
)
