package com.choiyoonseo.automoney.data.repository

import com.choiyoonseo.automoney.domain.category.UserCategory
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
import kotlinx.coroutines.flow.Flow

interface UserCategoryRepository {
    fun observeActiveCategories(): Flow<List<UserCategory>>
    suspend fun resolveOrCreate(kind: UserCategoryKind, name: String): UserCategory
    suspend fun add(kind: UserCategoryKind, name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
}
