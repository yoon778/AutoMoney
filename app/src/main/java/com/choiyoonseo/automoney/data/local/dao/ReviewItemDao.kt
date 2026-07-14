package com.choiyoonseo.automoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.choiyoonseo.automoney.data.local.entity.ReviewItemEntity
import com.choiyoonseo.automoney.data.local.entity.ReviewItemWithTransaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ReviewItemDao {
    @Insert
    suspend fun insert(entity: ReviewItemEntity): Long

    @Update
    suspend fun update(entity: ReviewItemEntity)

    @Query("SELECT COUNT(*) FROM review_items WHERE resolvedAt IS NULL")
    fun observeOpenItemCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM review_items WHERE resolvedAt IS NULL ORDER BY createdAt DESC")
    fun observeOpenItemsWithTransactions(): Flow<List<ReviewItemWithTransaction>>

    @Query("UPDATE review_items SET resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun resolve(id: Long, resolvedAt: Instant)
}
