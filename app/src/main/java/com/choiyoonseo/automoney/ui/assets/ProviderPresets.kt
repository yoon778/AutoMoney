package com.choiyoonseo.automoney.ui.assets

val securitiesProviderPresets: List<String> = listOf(
    "키움증권",
    "미래에셋증권",
    "삼성증권",
    "KB증권",
    "NH투자증권",
    "토스증권"
)

val payProviderPresets: List<String> = listOf(
    "네이버페이",
    "카카오페이",
    "삼성페이",
    "페이코",
    "토스페이"
)

fun providerPresetsFor(kindLabel: String): List<String> = when (kindLabel) {
    "증권" -> securitiesProviderPresets
    "페이" -> payProviderPresets
    else -> emptyList()
}
