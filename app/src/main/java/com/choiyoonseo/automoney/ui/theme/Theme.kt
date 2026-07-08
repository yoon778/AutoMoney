package com.choiyoonseo.automoney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AutoMoneyLightColors = lightColorScheme(
    primary = Color(0xFF2F80ED),
    secondary = Color(0xFF24A148),
    tertiary = Color(0xFFFF8A65),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF2FF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF172033),
    onSurface = Color(0xFF172033),
    onSurfaceVariant = Color(0xFF697386)
)

@Composable
fun AutoMoneyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoMoneyLightColors,
        content = content
    )
}
