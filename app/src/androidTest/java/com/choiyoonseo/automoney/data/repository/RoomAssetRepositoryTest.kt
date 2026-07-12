package com.choiyoonseo.automoney.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItemType
import com.choiyoonseo.automoney.domain.model.Category
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RoomAssetRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomAssetRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomAssetRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fixedExpensePreservesSelectedAccountIdAndNameSnapshot() = runBlocking {
        val accountId = repository.saveAccount(AssetAccount(name = "생활비", balanceWon = 100_000))
        repository.saveFixedExpense(
            FixedExpensePlan(
                name = "통신비",
                amountWon = 70_000,
                withdrawalDay = 15,
                accountName = "생활비",
                accountId = accountId
            )
        )

        val stored = repository.observeFixedExpenses().first().single()

        assertEquals(accountId, stored.accountId)
        assertEquals("생활비", stored.accountName)
    }

    @Test
    fun savingMonthlyPlanItemReemitsObservedItems() = runBlocking {
        val initialEmission = CompletableDeferred<Unit>()
        val emissions = async {
            repository.observeMonthlyPlanItems()
                .onEach { initialEmission.complete(Unit) }
                .take(2)
                .toList()
        }
        initialEmission.await()

        repository.saveMonthlyPlanItem(
            MonthlyPlanItem(label = "식비", amountWon = 300_000, type = MonthlyPlanItemType.BUDGET)
        )

        assertEquals(0, emissions.await().first().size)
        assertEquals("식비", emissions.await().last().single().label)
    }

    @Test
    fun monthlyPlanPreservesBudgetCategoryLink() = runBlocking {
        repository.saveMonthlyPlanItem(
            MonthlyPlanItem(
                label = "데이트비용",
                amountWon = 100_000,
                type = MonthlyPlanItemType.BUDGET,
                category = Category.OTHER,
                customCategoryId = 7,
                customCategoryName = "데이트비용"
            )
        )

        val stored = repository.observeMonthlyPlanItems().first().single()

        assertEquals(Category.OTHER, stored.category)
        assertEquals(7, stored.customCategoryId)
        assertEquals("데이트비용", stored.customCategoryName)
    }
}
