package com.choiyoonseo.automoney.domain.assets

fun moneyNamesMatch(first: String, second: String): Boolean {
    val firstKey = first.canonicalMoneyNameKey()
    val secondKey = second.canonicalMoneyNameKey()
    if (firstKey.isBlank() || secondKey.isBlank()) return false
    if (firstKey == secondKey) return true

    val firstNormalized = first.normalizedMoneyName()
    val secondNormalized = second.normalizedMoneyName()
    return firstNormalized.contains(secondNormalized) || secondNormalized.contains(firstNormalized)
}

fun String.canonicalMoneyNameKey(): String {
    val normalized = normalizedMoneyName()
    return when {
        normalized.contains("국민") || normalized.contains("kb") -> "kb"
        normalized.contains("토스") || normalized.contains("toss") -> "toss"
        normalized.contains("카카오") || normalized.contains("kakao") -> "kakao"
        normalized.contains("네이버") || normalized.contains("naver") -> "naver"
        else -> normalized
    }
}

fun String.normalizedMoneyName(): String =
    lowercase()
        .replace("은행", "")
        .replace("뱅크", "")
        .replace("통장", "")
        .replace("페이", "")
        .replace("bank", "")
        .replace("pay", "")
        .replace(Regex("""\s+"""), "")
        .trim()
