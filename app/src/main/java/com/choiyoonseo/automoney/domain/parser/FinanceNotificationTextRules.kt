package com.choiyoonseo.automoney.domain.parser

internal val FINANCE_PROMOTION_KEYWORDS =
    listOf("혜택", "할인", "이벤트", "쿠폰", "광고", "최대")

internal val FINANCE_NON_FINAL_KEYWORDS =
    listOf("실패", "거절", "예정", "미승인")

internal val FINANCE_BALANCE_KEYWORDS =
    listOf("잔액", "잔고", "출금가능", "사용가능", "이용가능", "한도")

internal fun isBlockedFinanceEventLine(line: String): Boolean =
    hasFinancePromotionKeyword(line) ||
        hasFinanceNonFinalKeyword(line)

internal fun hasFinancePromotionKeyword(line: String): Boolean =
    FINANCE_PROMOTION_TOKEN_REGEX.containsMatchIn(line) ||
        FINANCE_PROMOTION_COMPOUND_REGEX.containsMatchIn(line) ||
        AMOUNT_THEN_PROMOTION_REGEX.containsMatchIn(line) ||
        ACTION_THEN_PROMOTION_REGEX.containsMatchIn(line) ||
        PROMOTION_WITH_PARTICLE_REGEX.containsMatchIn(line)

internal fun hasFinanceNonFinalKeyword(line: String): Boolean =
    FINANCE_NON_FINAL_TOKEN_REGEX.containsMatchIn(line) ||
        NON_FINAL_INFLECTED_REGEX.containsMatchIn(line) ||
        ACTION_THEN_NON_FINAL_REGEX.containsMatchIn(line)

internal fun isBalanceDetailLine(line: String): Boolean =
    FINANCE_BALANCE_KEYWORDS.any { line.contains(it, ignoreCase = true) }

internal fun stripFinanceBalanceKeywords(text: String): String =
    FINANCE_BALANCE_KEYWORDS.fold(text) { current, keyword ->
        current.replace(keyword, "", ignoreCase = true)
    }

internal fun isBalanceOnlyFinanceLine(
    line: String,
    actionKeywords: List<String>
): Boolean {
    val eventText = stripFinanceBalanceKeywords(line)
    return isBalanceDetailLine(line) &&
        actionKeywords.none { eventText.contains(it, ignoreCase = true) }
}

internal fun isDebitCardWithdrawal(text: String): Boolean =
    text.contains("체크카드", ignoreCase = true) &&
        text.contains("출금", ignoreCase = true)

private val FINANCE_PROMOTION_TOKEN_REGEX = Regex(
    """(?<!\p{L})(?:${FINANCE_PROMOTION_KEYWORDS.joinToString("|", transform = Regex::escape)})(?!\p{L})""",
    RegexOption.IGNORE_CASE,
)
private val PROMOTION_KEYWORD_PATTERN =
    FINANCE_PROMOTION_KEYWORDS.joinToString("|", transform = Regex::escape)
private val PROMOTION_EXPRESSION_PATTERN =
    """(?:$PROMOTION_KEYWORD_PATTERN)(?:(?:$PROMOTION_KEYWORD_PATTERN))?"""
private val FINANCE_PROMOTION_COMPOUND_REGEX = Regex(
    """(?<!\p{L})(?:$PROMOTION_KEYWORD_PATTERN)(?:$PROMOTION_KEYWORD_PATTERN)(?!\p{L})""",
    RegexOption.IGNORE_CASE,
)
private val AMOUNT_THEN_PROMOTION_REGEX = Regex(
    """[0-9,]+\s*원\s*$PROMOTION_EXPRESSION_PATTERN(?:은|는|이|가|을|를|도|만)?(?!\p{L})""",
    RegexOption.IGNORE_CASE,
)
private val ACTION_THEN_PROMOTION_REGEX = Regex(
    """(?:결제|승인|송금|이체|카드|사용)(?:에|은|는|이|가)?\s*""" +
        """$PROMOTION_EXPRESSION_PATTERN(?:은|는|이|가|을|를|도|만)?(?!\p{L})""",
    RegexOption.IGNORE_CASE,
)
private val PROMOTION_WITH_PARTICLE_REGEX = Regex(
    """(?<!\p{L})(?:$PROMOTION_KEYWORD_PATTERN)(?:은|는|이|가|을|를|도|만)(?!\p{L})""",
    RegexOption.IGNORE_CASE,
)
private val NON_FINAL_KEYWORD_PATTERN =
    FINANCE_NON_FINAL_KEYWORDS.joinToString("|", transform = Regex::escape)
private val FINANCE_NON_FINAL_TOKEN_REGEX = Regex(
    """(?<!\p{L})(?:$NON_FINAL_KEYWORD_PATTERN)(?!\p{L})""",
    RegexOption.IGNORE_CASE,
)
private val NON_FINAL_INFLECTED_PATTERN =
    """(?:실패(?:했|해|함)|거절(?:됐|되었|됨|당했)|예정(?:이|입니|임)|미승인(?:이|입니|임|됨))"""
private val NON_FINAL_INFLECTED_REGEX = Regex(
    """(?<!\p{L})$NON_FINAL_INFLECTED_PATTERN""",
    RegexOption.IGNORE_CASE,
)
private val ACTION_THEN_NON_FINAL_REGEX = Regex(
    """(?:결제|승인|송금|이체|카드|사용)(?:에|은|는|이|가)?\s*""" +
        """(?:(?:$NON_FINAL_KEYWORD_PATTERN)(?:은|는|이|가)?(?!\p{L})|$NON_FINAL_INFLECTED_PATTERN)""",
    RegexOption.IGNORE_CASE,
)
