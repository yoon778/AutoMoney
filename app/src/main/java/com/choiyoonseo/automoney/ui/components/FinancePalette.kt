package com.choiyoonseo.automoney.ui.components

import androidx.compose.ui.graphics.Color

val MoneyYellow = Color(0xFFFFC542)

private val fallbackAccents = listOf(MoneyBlue, MoneyMint, MoneyCoral, MoneyYellow, MoneyGreen)

fun accountAccentForName(name: String): Color {
    val normalized = name.normalizedForPalette()
    return when {
        normalized.contains("\uad6d\ubbfc") || normalized.contains("kb") -> MoneyYellow
        normalized.contains("\ud1a0\uc2a4") || normalized.contains("toss") -> MoneyBlue
        normalized.contains("\uce74\uce74\uc624") || normalized.contains("kakao") -> MoneyYellow
        normalized.contains("\ub124\uc774\ubc84") || normalized.contains("naver") -> MoneyMint
        normalized.contains("\ud604\uae08") || normalized.contains("cash") -> MoneyGreen
        else -> stableFallbackAccent(normalized)
    }
}

fun categoryAccentForName(name: String): Color {
    val normalized = name.normalizedForPalette()
    return when {
        normalized.contains("\uce74\ud398") || normalized.contains("\uac04\uc2dd") || normalized.contains("coffee") -> MoneyYellow
        normalized.contains("\uad50\ud1b5") || normalized.contains("transport") -> MoneyBlue
        normalized.contains("\uae09\uc5ec") || normalized.contains("\uc218\uc785") || normalized.contains("income") -> MoneyGreen
        normalized.contains("\uc1fc\ud551") || normalized.contains("\uc9c0\ucd9c") || normalized.contains("shopping") -> MoneyCoral
        normalized.contains("\uc0dd\ud65c") || normalized.contains("living") -> MoneyMint
        else -> stableFallbackAccent(normalized)
    }
}

fun reviewAccentForLabel(label: String): Color {
    val normalized = label.normalizedForPalette()
    return when {
        normalized.contains("\ucda9\uc804") || normalized.contains("topup") -> MoneyYellow
        normalized.contains("\uc1a1\uae08") || normalized.contains("\uc815\uc0b0") || normalized.contains("transfer") -> MoneyBlue
        normalized.contains("\ud658\ubd88") || normalized.contains("refund") -> MoneyGreen
        normalized.contains("\uc911\ubcf5") || normalized.contains("\ud655\uc778") || normalized.contains("review") -> MoneyCoral
        else -> MoneyCoral
    }
}

fun softAccentColor(accent: Color): Color = accent.copy(alpha = 0.12f)

private fun stableFallbackAccent(text: String): Color {
    val index = Math.floorMod(text.hashCode(), fallbackAccents.size)
    return fallbackAccents[index]
}

private fun String.normalizedForPalette(): String = trim().lowercase()
