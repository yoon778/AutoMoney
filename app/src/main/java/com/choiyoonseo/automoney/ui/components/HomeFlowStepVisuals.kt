package com.choiyoonseo.automoney.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.choiyoonseo.automoney.R

data class HomeFlowStepVisual(
    val label: String,
    val value: String,
    val accent: Color,
    @param:DrawableRes val imageRes: Int
)

fun homeFlowStepVisuals(
    incomeValue: String,
    expenseValue: String,
    savingsValue: String
): List<HomeFlowStepVisual> = listOf(
    HomeFlowStepVisual(
        label = "\uc218\uc785",
        value = incomeValue,
        accent = MoneyBlue,
        imageRes = R.drawable.illustration_flow_income
    ),
    HomeFlowStepVisual(
        label = "\uc9c0\ucd9c",
        value = expenseValue,
        accent = MoneyCoral,
        imageRes = R.drawable.illustration_flow_expense
    ),
    HomeFlowStepVisual(
        label = "\uc800\ucd95/\uc774\uccb4",
        value = savingsValue,
        accent = MoneyMint,
        imageRes = R.drawable.illustration_flow_saving
    )
)
