package com.choiyoonseo.automoney.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class MoneyColors(
    val canvas: Color,
    val surface: Color,
    val divider: Color,
    val ink: Color,
    val inkSub: Color,
    val muted: Color,
    val primary: Color,
    val positive: Color,
    val negative: Color,
    val isDark: Boolean
) {
    fun soft(accent: Color): Color = accent.copy(alpha = if (isDark) 0.22f else 0.12f)
}

val LightMoneyColors = MoneyColors(
    canvas = Color(0xFFF2F4F6),
    surface = Color(0xFFFFFFFF),
    divider = Color(0xFFF2F4F6),
    ink = Color(0xFF191F28),
    inkSub = Color(0xFF4E5968),
    muted = Color(0xFF8B95A1),
    primary = Color(0xFF3182F6),
    positive = Color(0xFF00C471),
    negative = Color(0xFFF04452),
    isDark = false
)

val DarkMoneyColors = MoneyColors(
    canvas = Color(0xFF17171C),
    surface = Color(0xFF242429),
    divider = Color(0xFF2E2E35),
    ink = Color(0xFFF2F4F6),
    inkSub = Color(0xFF9DA5B4),
    muted = Color(0xFF8B95A1),
    primary = Color(0xFF4593FC),
    positive = Color(0xFF2AC769),
    negative = Color(0xFFFF6A76),
    isDark = true
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoneyColors }

object MoneyTheme {
    val colors: MoneyColors
        @Composable @ReadOnlyComposable
        get() = LocalMoneyColors.current
}
