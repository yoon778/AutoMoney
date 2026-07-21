package com.choiyoonseo.automoney.data.repository

import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryRecord
import kotlinx.coroutines.flow.Flow

interface NotificationHistoryRepository {
    fun observeRecent(): Flow<List<NotificationHistoryRecord>>
    suspend fun recordAndPrune(record: NotificationHistoryRecord): Long
    suspend fun clear()
    suspend fun markResolvedManually(historyId: Long, transactionId: Long): Boolean
}
