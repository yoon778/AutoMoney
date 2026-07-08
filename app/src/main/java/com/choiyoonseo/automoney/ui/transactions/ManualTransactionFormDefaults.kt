package com.choiyoonseo.automoney.ui.transactions

import com.choiyoonseo.automoney.domain.manual.ManualEntryType
import java.time.LocalDate

data class ManualTransactionFormDefaults(
    val entryType: ManualEntryType,
    val selectedDate: LocalDate,
    val amount: String,
    val expenseCategory: ManualCategoryOption,
    val incomeCategory: ManualCategoryOption,
    val memo: String,
    val inputError: String?,
    val isCategoryMenuExpanded: Boolean,
    val isDatePickerOpen: Boolean
)

fun defaultManualTransactionFormValues(today: LocalDate): ManualTransactionFormDefaults =
    ManualTransactionFormDefaults(
        entryType = ManualEntryType.EXPENSE,
        selectedDate = today,
        amount = "",
        expenseCategory = defaultManualCategoryOption,
        incomeCategory = defaultManualIncomeCategoryOption,
        memo = "",
        inputError = null,
        isCategoryMenuExpanded = false,
        isDatePickerOpen = false
    )
