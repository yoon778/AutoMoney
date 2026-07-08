package com.choiyoonseo.automoney.ui.transactions

import com.choiyoonseo.automoney.domain.manual.ManualEntryType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ManualTransactionFormDefaultsTest {
    @Test
    fun defaultValuesResetUserEditableFieldsAfterSuccessfulSave() {
        val today = LocalDate.of(2026, 7, 2)

        val defaults = defaultManualTransactionFormValues(today)

        assertThat(defaults.entryType).isEqualTo(ManualEntryType.EXPENSE)
        assertThat(defaults.selectedDate).isEqualTo(today)
        assertThat(defaults.amount).isEmpty()
        assertThat(defaults.expenseCategory).isEqualTo(defaultManualCategoryOption)
        assertThat(defaults.incomeCategory).isEqualTo(defaultManualIncomeCategoryOption)
        assertThat(defaults.memo).isEmpty()
        assertThat(defaults.inputError).isNull()
        assertThat(defaults.isCategoryMenuExpanded).isFalse()
        assertThat(defaults.isDatePickerOpen).isFalse()
    }
}
