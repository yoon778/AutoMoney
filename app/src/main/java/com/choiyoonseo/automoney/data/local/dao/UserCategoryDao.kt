package com.choiyoonseo.automoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.choiyoonseo.automoney.data.local.entity.UserCategoryEntity
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryDao {
    @Query("SELECT * FROM user_categories WHERE active = 1 ORDER BY id ASC")
    fun observeActiveCategories(): Flow<List<UserCategoryEntity>>

    @Query("SELECT * FROM user_categories WHERE id = :id LIMIT 1")
    suspend fun categoryById(id: Long): UserCategoryEntity?

    @Query(
        "SELECT * FROM user_categories WHERE kind = :kind AND normalizedName = :normalizedName LIMIT 1"
    )
    suspend fun categoryByKindAndNormalizedName(
        kind: UserCategoryKind,
        normalizedName: String
    ): UserCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: UserCategoryEntity): Long

    @Update
    suspend fun update(category: UserCategoryEntity)

    @Query("UPDATE user_categories SET active = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("UPDATE user_categories SET active = 1, name = :name WHERE id = :id")
    suspend fun reactivate(id: Long, name: String)
}
