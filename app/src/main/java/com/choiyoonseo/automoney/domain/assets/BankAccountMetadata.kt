package com.choiyoonseo.automoney.domain.assets

enum class BankProvider(val displayName: String, val badgeText: String) {
    KB("KB국민은행", "KB"),
    SHINHAN("신한은행", "신한"),
    HANA("하나은행", "하나"),
    WOORI("우리은행", "우리"),
    NH("NH농협은행", "NH"),
    IBK("IBK기업은행", "IBK"),
    KAKAO_BANK("카카오뱅크", "카카오"),
    TOSS_BANK("토스뱅크", "토스")
}

enum class BalanceImpact {
    CREDIT,
    DEBIT,
    NONE
}

fun normalizeAccountLast4(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    require(digits.length >= 4) { "계좌번호는 숫자 4자리 이상 입력해 주세요." }
    return digits.takeLast(4)
}

fun maskedAccountLast4(last4: String?): String? =
    last4?.takeIf { it.length == 4 && it.all(Char::isDigit) }?.let { "****$it" }
