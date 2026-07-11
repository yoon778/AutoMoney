package com.choiyoonseo.automoney.data.repository

import androidx.room.withTransaction
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.local.entity.UserCategoryEntity
import com.choiyoonseo.automoney.domain.category.UserCategory
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserCategoryRepository(
    private val db: AppDatabase
) : UserCategoryRepository {
    override fun observeActiveCategories(): Flow<List<UserCategory>> =
        db.userCategoryDao().observeActiveCategories().map { categories ->
            categories.map { it.toDomain() }
        }

    override suspend fun resolveOrCreate(kind: UserCategoryKind, name: String): UserCategory {
        val requested = UserCategory.create(kind = kind, name = name)
        return db.withTransaction {
            val current = db.userCategoryDao().categoryByKindAndNormalizedName(
                kind = requested.kind,
                normalizedName = requested.normalizedName
            )
            when {
                current == null -> UserCategory.create(
                    id = db.userCategoryDao().insert(requested.toEntity()),
                    kind = requested.kind,
                    name = requested.name
                )
                current.active -> current.toDomain()
                else -> {
                    db.userCategoryDao().reactivate(id = current.id, name = requested.name)
                    UserCategory.create(
                        id = current.id,
                        kind = requested.kind,
                        name = requested.name,
                        active = true
                    )
                }
            }
        }
    }

    override suspend fun add(kind: UserCategoryKind, name: String): Long =
        db.userCategoryDao().insert(UserCategory.create(kind = kind, name = name).toEntity())

    override suspend fun rename(id: Long, name: String) {
        require(id > 0) { "Category id must be positive" }
        db.withTransaction {
            val current = db.userCategoryDao().categoryById(id)
                ?: throw IllegalArgumentException("Category not found: $id")
            val renamed = UserCategory.create(
                id = current.id,
                kind = current.kind,
                name = name,
                active = current.active
            )
            db.userCategoryDao().update(renamed.toEntity())
            db.transactionDao().updateCustomCategoryName(id, renamed.name)
        }
    }

    override suspend fun delete(id: Long) {
        require(id > 0) { "Category id must be positive" }
        db.userCategoryDao().deactivate(id)
    }
}

private fun UserCategoryEntity.toDomain(): UserCategory =
    UserCategory.create(
        id = id,
        kind = kind,
        name = name,
        active = active
    )

private fun UserCategory.toEntity(): UserCategoryEntity =
    UserCategoryEntity(
        id = id,
        kind = kind,
        name = name,
        normalizedName = normalizedName,
        active = active
    )
