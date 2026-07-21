package com.choiyoonseo.automoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.choiyoonseo.automoney.data.local.entity.NotificationHistoryEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Query("SELECT * FROM notification_history ORDER BY receivedAt DESC, id DESC")
    fun observeRecent(): Flow<List<NotificationHistoryEntity>>

    @Insert
    suspend fun insert(entity: NotificationHistoryEntity): Long

    @Query("DELETE FROM notification_history WHERE receivedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant)

    @Query(
        "DELETE FROM notification_history WHERE id NOT IN " +
            "(SELECT id FROM notification_history ORDER BY receivedAt DESC, id DESC LIMIT :limit)"
    )
    suspend fun keepNewest(limit: Int)

    @Query("DELETE FROM notification_history")
    suspend fun clear()

    @Query(
        "UPDATE notification_history SET status = 'RESOLVED_MANUALLY', " +
            "reason = 'MANUAL_RECORD_CREATED', linkedTransactionId = :transactionId " +
            "WHERE id = :historyId AND status IN ('IGNORED', 'ERROR')"
    )
    suspend fun markResolvedManually(historyId: Long, transactionId: Long): Int
}
