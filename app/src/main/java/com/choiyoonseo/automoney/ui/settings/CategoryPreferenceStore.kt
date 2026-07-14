package com.choiyoonseo.automoney.ui.settings

import android.content.Context
import android.content.SharedPreferences
import com.choiyoonseo.automoney.domain.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

val expenseCategoryPool: List<Category> = listOf(
    Category.FOOD,
    Category.CAFE_SNACK,
    Category.TRANSPORT,
    Category.SHOPPING,
    Category.LIVING,
    Category.HOBBY,
    Category.BEAUTY,
    Category.HEALTH,
    Category.STUDY,
    Category.EVENT,
    Category.TELECOM,
    Category.SUBSCRIPTION,
    Category.OTHER
)

val incomeCategoryPool: List<Category> = listOf(
    Category.SALARY,
    Category.ALLOWANCE,
    Category.INVESTMENT_RETURN,
    Category.DISCOUNT,
    Category.REIMBURSEMENT,
    Category.OTHER
)

val defaultEnabledExpenseCategories: List<Category> = listOf(
    Category.FOOD,
    Category.CAFE_SNACK,
    Category.TRANSPORT,
    Category.SHOPPING,
    Category.LIVING,
    Category.OTHER
)

val defaultEnabledIncomeCategories: List<Category> = listOf(
    Category.SALARY,
    Category.ALLOWANCE,
    Category.INVESTMENT_RETURN,
    Category.REIMBURSEMENT,
    Category.OTHER
)

fun resolveEnabledCategories(
    storedNames: Set<String>?,
    pool: List<Category>,
    defaults: List<Category>
): List<Category> {
    if (storedNames == null) return defaults
    val enabled = pool.filter { it.name in storedNames || it == Category.OTHER }
    return if (enabled.size <= 1) defaults else enabled
}

interface CategoryPreferenceStore {
    fun enabledExpenseCategories(): List<Category>
    fun enabledIncomeCategories(): List<Category>
    fun observeEnabledExpenseCategories(): Flow<List<Category>>
    fun observeEnabledIncomeCategories(): Flow<List<Category>>
    fun setEnabledExpenseCategories(categories: Collection<Category>)
    fun setEnabledIncomeCategories(categories: Collection<Category>)
}

class SharedPreferencesCategoryPreferenceStore(context: Context) : CategoryPreferenceStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun enabledExpenseCategories(): List<Category> =
        resolveEnabledCategories(
            preferences.getStringSet(KEY_EXPENSE, null),
            expenseCategoryPool,
            defaultEnabledExpenseCategories
        )

    override fun enabledIncomeCategories(): List<Category> =
        resolveEnabledCategories(
            preferences.getStringSet(KEY_INCOME, null),
            incomeCategoryPool,
            defaultEnabledIncomeCategories
        )

    override fun observeEnabledExpenseCategories(): Flow<List<Category>> =
        observeCategories(KEY_EXPENSE, ::enabledExpenseCategories)

    override fun observeEnabledIncomeCategories(): Flow<List<Category>> =
        observeCategories(KEY_INCOME, ::enabledIncomeCategories)

    private fun observeCategories(
        key: String,
        read: () -> List<Category>
    ): Flow<List<Category>> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key || changedKey == null) trySend(read())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun setEnabledExpenseCategories(categories: Collection<Category>) {
        preferences.edit()
            .putStringSet(KEY_EXPENSE, categories.map(Category::name).toSet())
            .apply()
    }

    override fun setEnabledIncomeCategories(categories: Collection<Category>) {
        preferences.edit()
            .putStringSet(KEY_INCOME, categories.map(Category::name).toSet())
            .apply()
    }
}

private const val PREFERENCES_NAME = "category_preferences"
private const val KEY_EXPENSE = "enabled_expense_categories"
private const val KEY_INCOME = "enabled_income_categories"
