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
import java.time.YearMonth

class RoomAssetRepositoryTest {
    private val july = YearMonth.of(2026, 7)
    private val august = YearMonth.of(2026, 8)
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
            ),
            july
        )

        val stored = repository.observeFixedExpenses(july).first().single()

        assertEquals(accountId, stored.accountId)
        assertEquals("생활비", stored.accountName)
    }

    @Test
    fun fixedExpenseUpdateReplacesSameDatabaseRow() = runBlocking {
        val id = repository.saveFixedExpense(
            FixedExpensePlan(
                name = "통신비",
                amountWon = 70_000,
                withdrawalDay = 15,
                accountName = "국민은행"
            ),
            july
        )

        repository.saveFixedExpense(
            FixedExpensePlan(
                id = id,
                name = "휴대폰 요금",
                amountWon = 80_000,
                withdrawalDay = 20,
                accountName = "국민은행"
            ),
            july
        )

        val stored = repository.observeFixedExpenses(july).first().single()
        assertEquals(id, stored.id)
        assertEquals("휴대폰 요금", stored.name)
        assertEquals(80_000, stored.amountWon)
        assertEquals(20, stored.withdrawalDay)
    }

    @Test
    fun fixedExpenseEditKeepsPastMonthAndContinuesFromSelectedMonth() = runBlocking {
        val originalId = repository.saveFixedExpense(
            FixedExpensePlan(
                name = "통신비",
                amountWon = 70_000,
                withdrawalDay = 15,
                accountName = "국민은행"
            ),
            july
        )

        repository.saveFixedExpense(
            FixedExpensePlan(
                id = originalId,
                name = "통신비",
                amountWon = 80_000,
                withdrawalDay = 15,
                accountName = "국민은행"
            ),
            august
        )

        val julyPlan = repository.observeFixedExpenses(july).first().single()
        val augustPlan = repository.observeFixedExpenses(august).first().single()
        val septemberPlan = repository.observeFixedExpenses(august.plusMonths(1)).first().single()
        val nextYearPlan = repository.observeFixedExpenses(YearMonth.of(2027, 1)).first().single()

        assertEquals(originalId, julyPlan.id)
        assertEquals(70_000, julyPlan.amountWon)
        assertEquals(80_000, augustPlan.amountWon)
        assertEquals(augustPlan.id, septemberPlan.id)
        assertEquals(80_000, nextYearPlan.amountWon)
    }

    @Test
    fun fixedExpenseDeletePreservesPastAndHidesSelectedAndFutureMonths() = runBlocking {
        val id = repository.saveFixedExpense(
            FixedExpensePlan(
                name = "구독료",
                amountWon = 12_000,
                withdrawalDay = 3,
                accountName = "국민은행"
            ),
            july
        )

        repository.deleteFixedExpense(id, august)

        assertEquals(1, repository.observeFixedExpenses(july).first().size)
        assertEquals(0, repository.observeFixedExpenses(august).first().size)
        assertEquals(0, repository.observeFixedExpenses(august.plusMonths(1)).first().size)
    }

    @Test
    fun savingMonthlyPlanItemReemitsObservedItems() = runBlocking {
        val initialEmission = CompletableDeferred<Unit>()
        val emissions = async {
            repository.observeMonthlyPlanItems(july)
                .onEach { initialEmission.complete(Unit) }
                .take(2)
                .toList()
        }
        initialEmission.await()

        repository.saveMonthlyPlanItem(
            MonthlyPlanItem(label = "식비", amountWon = 300_000, type = MonthlyPlanItemType.BUDGET),
            july
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
            ),
            july
        )

        val stored = repository.observeMonthlyPlanItems(july).first().single()

        assertEquals(Category.OTHER, stored.category)
        assertEquals(7L, stored.customCategoryId)
        assertEquals("데이트비용", stored.customCategoryName)
    }

    @Test
    fun monthlyPlansAreIsolatedByYearMonthAndUpdatesKeepTheirMonth() = runBlocking {
        repository.saveMonthlyPlanItem(
            MonthlyPlanItem(label = "7월 식비", amountWon = 300_000, type = MonthlyPlanItemType.BUDGET),
            july
        )
        val augustId = repository.saveMonthlyPlanItem(
            MonthlyPlanItem(label = "8월 식비", amountWon = 400_000, type = MonthlyPlanItemType.BUDGET),
            august
        )
        repository.saveMonthlyPlanItem(
            MonthlyPlanItem(
                id = augustId,
                label = "8월 수정 식비",
                amountWon = 450_000,
                type = MonthlyPlanItemType.BUDGET
            ),
            august
        )

        assertEquals("7월 식비", repository.observeMonthlyPlanItems(july).first().single().label)
        assertEquals("8월 수정 식비", repository.observeMonthlyPlanItems(august).first().single().label)
    }
}
