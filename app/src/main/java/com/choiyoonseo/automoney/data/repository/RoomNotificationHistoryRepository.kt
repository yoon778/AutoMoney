package com.choiyoonseo.automoney.data.repository

import androidx.room.withTransaction
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.local.entity.NotificationHistoryEntity
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryRecord
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNotificationHistoryRepository(
    private val db: AppDatabase
) : NotificationHistoryRepository {
    override fun observeRecent(): Flow<List<NotificationHistoryRecord>> =
        db.notificationHistoryDao().observeRecent().map { rows -> rows.map { it.toDomain() } }

    override suspend fun recordAndPrune(record: NotificationHistoryRecord): Long =
        db.withTransaction {
            val id = db.notificationHistoryDao().insert(record.toEntity())
            db.notificationHistoryDao().deleteOlderThan(record.receivedAt.minus(30, ChronoUnit.DAYS))
            db.notificationHistoryDao().keepNewest(200)
            id
        }

    override suspend fun clear() = db.notificationHistoryDao().clear()

    override suspend fun markResolvedManually(historyId: Long, transactionId: Long): Boolean =
        db.notificationHistoryDao().markResolvedManually(historyId, transactionId) == 1
}

private fun NotificationHistoryRecord.toEntity() = NotificationHistoryEntity(
    id = id,
    packageName = packageName,
    sourceLabel = sourceLabel,
    receivedAt = receivedAt,
    status = status,
    transactionType = transactionType,
    amountWon = amountWon,
    reason = reason,
    linkedTransactionId = linkedTransactionId
)

private fun NotificationHistoryEntity.toDomain() = NotificationHistoryRecord(
    id = id,
    packageName = packageName,
    sourceLabel = sourceLabel,
    receivedAt = receivedAt,
    status = status,
    transactionType = transactionType,
    amountWon = amountWon,
    reason = reason,
    linkedTransactionId = linkedTransactionId
)
