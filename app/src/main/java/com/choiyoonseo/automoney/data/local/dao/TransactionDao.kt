package com.choiyoonseo.automoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.choiyoonseo.automoney.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun transactionById(transactionId: Long): TransactionEntity?

    @Query("UPDATE transactions SET customCategoryName = :name WHERE customCategoryId = :categoryId")
    suspend fun updateCustomCategoryName(categoryId: Long, name: String)

    @Query("SELECT * FROM transactions WHERE monthKey = :monthKey ORDER BY occurredAt DESC")
    suspend fun transactionsForMonth(monthKey: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE monthKey = :monthKey ORDER BY occurredAt DESC")
    fun observeTransactionsForMonth(monthKey: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE sourceNotificationHash IS NOT NULL ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun recentNotificationTransactions(limit: Int): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE sourceNotificationHash = :sourceNotificationHash")
    suspend fun countBySourceNotificationHash(sourceNotificationHash: String): Int
}
