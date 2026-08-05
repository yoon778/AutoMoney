package com.choiyoonseo.automoney.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeFlowStepVisual(
    val label: String,
    val value: String,
    val accent: Color,
    val icon: ImageVector
)

fun homeFlowStepVisuals(
    incomeValue: String,
    expenseValue: String,
    savingsValue: String
): List<HomeFlowStepVisual> = listOf(
    HomeFlowStepVisual(
        label = "수입",
        value = incomeValue,
        accent = MoneyBlue,
        icon = Icons.AutoMirrored.Filled.TrendingUp
    ),
    HomeFlowStepVisual(
        label = "지출",
        value = expenseValue,
        accent = MoneyCoral,
        icon = Icons.Filled.ShoppingCart
    ),
    HomeFlowStepVisual(
        label = "저축",
        value = savingsValue,
        accent = MoneyMint,
        icon = Icons.Filled.Savings
    )
)
