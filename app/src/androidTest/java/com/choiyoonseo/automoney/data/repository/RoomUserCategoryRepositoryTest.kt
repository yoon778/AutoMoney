package com.choiyoonseo.automoney.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomUserCategoryRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var categoryRepository: RoomUserCategoryRepository
    private lateinit var moneyRepository: RoomMoneyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryRepository = RoomUserCategoryRepository(database)
        moneyRepository = RoomMoneyRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun renameUpdatesTransactionSnapshotAndDeleteOnlyHidesOption() = runBlocking {
        val categoryId = categoryRepository.add(UserCategoryKind.EXPENSE, "Pet")
        moneyRepository.saveTransaction(customExpense(categoryId, "Pet"))

        categoryRepository.rename(categoryId, "Pet care")

        assertEquals("Pet care", categoryRepository.observeActiveCategories().first().single().name)
        assertEquals(
            "Pet care",
            moneyRepository.observeTransactionsForMonth(YearMonth.of(2026, 7))
                .first()
                .single()
                .customCategoryName
        )

        categoryRepository.delete(categoryId)

        assertTrue(categoryRepository.observeActiveCategories().first().isEmpty())
        assertEquals(
            "Pet care",
            moneyRepository.observeTransactionsForMonth(YearMonth.of(2026, 7))
                .first()
                .single()
                .customCategoryName
        )
    }

    @Test
    fun resolveOrCreateReactivatesAnExistingHiddenCategory() = runBlocking {
        val categoryId = categoryRepository.add(UserCategoryKind.EXPENSE, "Pet")
        categoryRepository.delete(categoryId)

        val restored = categoryRepository.resolveOrCreate(UserCategoryKind.EXPENSE, "  Pet  ")

        assertEquals(categoryId, restored.id)
        assertEquals("Pet", restored.name)
        assertTrue(restored.active)
    }

    private fun customExpense(categoryId: Long, categoryName: String) = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
        amount = MoneyAmount(30_000),
        direction = TransactionDirection.EXPENSE,
        type = TransactionType.EXPENSE,
        category = Category.OTHER,
        paymentMethod = null,
        merchant = "Vet",
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = SourceType.MANUAL,
        sourceNotificationHash = null,
        status = TransactionStatus.USER_EDITED,
        confidence = 1.0,
        monthKey = YearMonth.of(2026, 7),
        customCategoryId = categoryId,
        customCategoryName = categoryName
    )
}
