package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeFlowStepVisualsTest {
    @Test
    fun homeFlowStepsExposeIncomeExpenseAndSavingsIllustrations() {
        val steps = homeFlowStepVisuals(
            incomeValue = "1,000\uc6d0",
            expenseValue = "600\uc6d0",
            savingsValue = "400\uc6d0"
        )

        assertThat(steps.map { it.label })
            .containsExactly("\uc218\uc785", "\uc9c0\ucd9c", "\uc800\ucd95/\uc774\uccb4")
            .inOrder()
        assertThat(steps.map { it.value })
            .containsExactly("1,000\uc6d0", "600\uc6d0", "400\uc6d0")
            .inOrder()
        assertThat(steps.map { it.accent })
            .containsExactly(MoneyBlue, MoneyCoral, MoneyMint)
            .inOrder()
        assertThat(steps.map { it.imageRes })
            .containsExactly(
                R.drawable.illustration_flow_income,
                R.drawable.illustration_flow_expense,
                R.drawable.illustration_flow_saving
            )
            .inOrder()
    }
}
