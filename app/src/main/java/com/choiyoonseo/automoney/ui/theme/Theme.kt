package com.choiyoonseo.automoney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun AutoMoneyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val money = if (darkTheme) DarkMoneyColors else LightMoneyColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = money.primary,
            secondary = money.positive,
            tertiary = money.negative,
            background = money.canvas,
            surface = money.surface,
            surfaceVariant = money.primary.copy(alpha = 0.16f),
            onPrimary = Color.White,
            onBackground = money.ink,
            onSurface = money.ink,
            onSurfaceVariant = money.muted
        )
    } else {
        lightColorScheme(
            primary = money.primary,
            secondary = money.positive,
            tertiary = money.negative,
            background = money.canvas,
            surface = money.surface,
            surfaceVariant = money.primary.copy(alpha = 0.10f),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = money.ink,
            onSurface = money.ink,
            onSurfaceVariant = money.muted
        )
    }
    CompositionLocalProvider(LocalMoneyColors provides money) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AutoMoneyTypography,
            shapes = AutoMoneyShapes,
            content = content
        )
    }
}
