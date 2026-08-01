package com.choiyoonseo.automoney.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyModelsTest {
    @Test
    fun walletTopupIsNeutral() {
        assertThat(TransactionType.WALLET_TOPUP.defaultDirection).isEqualTo(TransactionDirection.NEUTRAL)
    }

    @Test
    fun expenseAffectsSpendingTotal() {
        assertThat(TransactionType.EXPENSE.countsAsMonthlyExpense).isTrue()
        assertThat(TransactionType.SPECIAL_EXPENSE.defaultDirection)
            .isEqualTo(TransactionDirection.EXPENSE)
        assertThat(TransactionType.SPECIAL_EXPENSE.countsAsMonthlyExpense).isFalse()
        assertThat(TransactionType.TRANSFER.countsAsMonthlyExpense).isFalse()
        assertThat(TransactionType.WALLET_TOPUP.countsAsMonthlyExpense).isFalse()
    }
}
