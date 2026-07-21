package com.choiyoonseo.automoney.domain.assets

enum class BankProvider(val displayName: String, val badgeText: String) {
    KB("KB국민은행", "KB"),
    SHINHAN("신한은행", "신한"),
    HANA("하나은행", "하나"),
    WOORI("우리은행", "우리"),
    NH("NH농협은행", "NH"),
    IBK("IBK기업은행", "IBK"),
    KAKAO_BANK("카카오뱅크", "카카오"),
    TOSS_BANK("토스뱅크", "토스"),
    K_BANK("케이뱅크", "K"),
    SC("SC제일은행", "SC"),
    IM_BANK("iM뱅크", "iM"),
    BNK_BUSAN("BNK부산은행", "BNK")
}

enum class BalanceImpact {
    CREDIT,
    DEBIT,
    NONE
}
