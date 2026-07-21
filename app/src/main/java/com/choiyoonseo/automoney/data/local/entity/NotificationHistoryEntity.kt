package com.choiyoonseo.automoney.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryReason
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryStatus
import java.time.Instant

@Entity(
    tableName = "notification_history",
    indices = [
        Index(value = ["receivedAt"]),
        Index(value = ["linkedTransactionId"])
    ]
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val sourceLabel: String?,
    val receivedAt: Instant,
    val status: NotificationHistoryStatus,
    val transactionType: TransactionType?,
    val amountWon: Long?,
    val reason: NotificationHistoryReason,
    val linkedTransactionId: Long?
)
