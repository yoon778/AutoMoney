package com.choiyoonseo.automoney.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeFlowStepVisualsTest {
    @Test
    fun homeFlowStepsExposeIncomeExpenseAndSavingsIcons() {
        val steps = homeFlowStepVisuals(
            incomeValue = "1,000원",
            expenseValue = "600원",
            savingsValue = "400원"
        )

        assertThat(steps.map { it.label })
            .containsExactly("수입", "지출", "저축/이체")
            .inOrder()
        assertThat(steps.map { it.value })
            .containsExactly("1,000원", "600원", "400원")
            .inOrder()
        assertThat(steps.map { it.accent })
            .containsExactly(MoneyBlue, MoneyCoral, MoneyMint)
            .inOrder()
        assertThat(steps.map { it.icon })
            .containsExactly(
                Icons.AutoMirrored.Filled.TrendingUp,
                Icons.Filled.ShoppingCart,
                Icons.Filled.Savings
            )
            .inOrder()
    }
}
