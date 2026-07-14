package com.choiyoonseo.automoney.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.category.UserCategory
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.ui.settings.SharedPreferencesCategoryPreferenceStore
import com.choiyoonseo.automoney.ui.settings.defaultEnabledExpenseCategories
import com.choiyoonseo.automoney.ui.settings.defaultEnabledIncomeCategories
import kotlinx.coroutines.flow.flowOf

// 활성 기본 분류와 사용자 정의 분류를 하나의 라벨 목록으로 병합한다.
// 저장은 TransactionCategoryResolver가 라벨 문자열로 처리하므로 라벨만 넘기면 된다.
fun mergedCategoryLabels(
    enabled: List<Category>,
    custom: List<UserCategory>,
    kind: UserCategoryKind
): List<String> {
    val base = enabled.map { it.displayName }
    val customNames = custom.filter { it.kind == kind }.map { it.name }
    return (base + customNames).distinct()
}

// 활성 기본 분류(SharedPreferences)와 사용자 정의 분류(Room)를 실시간 구독해
// 지출·수입 라벨 목록으로 병합한다. (지출, 수입) 순서로 반환.
@Composable
fun rememberMergedCategoryLabels(
    userCategoryRepository: UserCategoryRepository?
): Pair<List<String>, List<String>> {
    val context = LocalContext.current
    val store = remember { SharedPreferencesCategoryPreferenceStore(context) }
    val enabledExpense by remember { store.observeEnabledExpenseCategories() }
        .collectAsState(initial = defaultEnabledExpenseCategories)
    val enabledIncome by remember { store.observeEnabledIncomeCategories() }
        .collectAsState(initial = defaultEnabledIncomeCategories)
    val custom by remember(userCategoryRepository) {
        userCategoryRepository?.observeActiveCategories() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val expense = remember(enabledExpense, custom) {
        mergedCategoryLabels(enabledExpense, custom, UserCategoryKind.EXPENSE)
    }
    val income = remember(enabledIncome, custom) {
        mergedCategoryLabels(enabledIncome, custom, UserCategoryKind.INCOME)
    }
    return expense to income
}
