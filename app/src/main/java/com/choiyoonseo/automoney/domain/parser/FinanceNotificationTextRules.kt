package com.choiyoonseo.automoney.domain.parser

internal val FINANCE_PROMOTION_KEYWORDS =
    listOf("혜택", "할인", "이벤트", "쿠폰", "광고", "최대")

internal val FINANCE_NON_FINAL_KEYWORDS =
    listOf("실패", "거절", "예정", "미승인")

internal val FINANCE_BALANCE_KEYWORDS =
    listOf("잔액", "잔고", "출금가능", "사용가능", "이용가능", "한도")

internal fun isBlockedFinanceEventLine(line: String): Boolean =
    (FINANCE_PROMOTION_KEYWORDS + FINANCE_NON_FINAL_KEYWORDS)
        .any { line.contains(it, ignoreCase = true) }

internal fun isBalanceDetailLine(line: String): Boolean =
    FINANCE_BALANCE_KEYWORDS.any { line.contains(it, ignoreCase = true) }

internal fun stripFinanceBalanceKeywords(text: String): String =
    FINANCE_BALANCE_KEYWORDS.fold(text) { current, keyword ->
        current.replace(keyword, "", ignoreCase = true)
    }
