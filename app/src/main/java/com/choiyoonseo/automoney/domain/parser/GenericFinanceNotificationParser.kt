package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType

class GenericFinanceNotificationParser : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean = snapshot.combinedText.isNotBlank()

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        if (!canParse(snapshot)) return ParseResult.Ignored("empty notification")
        // 잔액/잔고 금액을 제거한 뒤, 최신 본문부터 금액과 행동어가 함께 있는 줄을 선택한다.
        // 복수 금액은 캐시백/환급/적립 라벨이 있을 때만 행동어에 가장 가까운 금액을 선택한다.
        val primaryCandidates = candidatesIn(snapshot.text)
        val candidate = when {
            primaryCandidates.size == 1 -> primaryCandidates.single()
            primaryCandidates.size > 1 -> null
            else -> candidatesIn(listOfNotNull(snapshot.title, snapshot.bigText).joinToString("\n"))
                .singleOrNull()
        } ?: return ParseResult.Ignored("unambiguous amount and action not found")
        val amountMatch = candidate.amountMatch
        val semantics = candidate.semantics
        val amountWon = amountMatch.groupValues[1].replace(",", "").toLongOrNull()
            ?.takeIf { it > 0 } ?: return ParseResult.Ignored("invalid amount")

        return ParseResult.Parsed(
            TransactionDraft(
                occurredAt = snapshot.postedAt,
                amount = MoneyAmount(amountWon),
                direction = semantics.direction,
                type = semantics.type,
                category = null,
                paymentMethod = null,
                merchant = null,
                counterparty = null,
                memo = null,
                sourceApp = snapshot.packageName,
                sourceNotificationHash = snapshot.sourceNotificationHash,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = GENERIC_CONFIDENCE,
                monthKey = snapshot.postedAt.toKoreanMonthKey(),
                reviewReason = semantics.reviewReason,
                bankAccountHint = null
            )
        )
    }

    private fun candidatesIn(content: String?): List<Candidate> =
        BALANCE_TOKEN_REGEX.replace(content.orEmpty(), " ")
            .lineSequence()
            .map(String::trim)
            .mapNotNull(::candidateFor)
            .toList()

    private fun candidateFor(line: String): Candidate? {
        val semantics = semanticsFor(line) ?: return null
        val amountMatches = AMOUNT_REGEX.findAll(line).toList()
        val hasLabeledSecondaryAmount = amountMatches.size > 1 &&
            SECONDARY_AMOUNT_KEYWORDS.any { line.contains(it, ignoreCase = true) }
        val blockingKeywords = BLOCK_KEYWORDS.filter { keyword ->
            keyword != "적립" || !hasLabeledSecondaryAmount
        }
        if (blockingKeywords.any { line.contains(it, ignoreCase = true) }) return null
        val amountMatch = when (amountMatches.size) {
            1 -> amountMatches.single()
            in 2..Int.MAX_VALUE -> closestLabeledAmount(line, amountMatches, semantics)
            else -> null
        } ?: return null
        return Candidate(amountMatch, semantics)
    }

    private fun closestLabeledAmount(
        line: String,
        amountMatches: List<MatchResult>,
        semantics: Semantics
    ): MatchResult? {
        if (SECONDARY_AMOUNT_KEYWORDS.none { line.contains(it, ignoreCase = true) }) return null
        val actionIndexes = semantics.actionKeywords.mapNotNull { keyword ->
            line.indexOf(keyword, ignoreCase = true).takeIf { it >= 0 }
        }
        if (actionIndexes.isEmpty()) return null
        val scored = amountMatches.map { match ->
            match to actionIndexes.minOf { actionIndex ->
                kotlin.math.abs(match.range.last - actionIndex)
            }
        }
        val minimumDistance = scored.minOf { it.second }
        return scored.singleOrNull { it.second == minimumDistance }?.first
    }

    private fun semanticsFor(line: String): Semantics? = when {
        REFUND_KEYWORDS.any(line::contains) -> Semantics(
            TransactionType.REFUND,
            TransactionDirection.NEUTRAL,
            ReviewReason.REFUND_OR_CANCEL,
            REFUND_KEYWORDS
        )
        TOPUP_KEYWORDS.any(line::contains) -> Semantics(
            TransactionType.WALLET_TOPUP,
            TransactionDirection.NEUTRAL,
            ReviewReason.WALLET_TOPUP,
            TOPUP_KEYWORDS
        )
        DEPOSIT_KEYWORDS.any(line::contains) -> Semantics(
            TransactionType.INCOME,
            TransactionDirection.INCOME,
            ReviewReason.INCOME_UNKNOWN,
            DEPOSIT_KEYWORDS
        )
        TRANSFER_KEYWORDS.any(line::contains) -> Semantics(
            TransactionType.TRANSFER,
            TransactionDirection.NEUTRAL,
            ReviewReason.TRANSFER_UNKNOWN,
            TRANSFER_KEYWORDS
        )
        PAYMENT_KEYWORDS.any(line::contains) -> Semantics(
            TransactionType.EXPENSE,
            TransactionDirection.EXPENSE,
            ReviewReason.LOW_CONFIDENCE_CATEGORY,
            PAYMENT_KEYWORDS
        )
        else -> null
    }

    private data class Semantics(
        val type: TransactionType,
        val direction: TransactionDirection,
        val reviewReason: ReviewReason,
        val actionKeywords: List<String>
    )

    private data class Candidate(
        val amountMatch: MatchResult,
        val semantics: Semantics
    )

    private companion object {
        val AMOUNT_REGEX = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
        val PAYMENT_KEYWORDS = listOf("결제", "승인", "사용")
        val DEPOSIT_KEYWORDS = listOf("입금", "받음", "받았")
        val TRANSFER_KEYWORDS = listOf("이체", "송금", "출금", "ATM")
        val REFUND_KEYWORDS = listOf("취소", "환불", "환급")
        val TOPUP_KEYWORDS = listOf("충전")
        val SECONDARY_AMOUNT_KEYWORDS = listOf("캐시백", "환급", "적립")
        val BLOCK_KEYWORDS = listOf(
            "혜택", "할인", "이벤트", "쿠폰", "광고", "최대", "적립", "예정",
            "실패", "거절", "한도", "이용가능"
        )
        val BALANCE_TOKEN_REGEX = Regex(
            """(?:${FINANCE_BALANCE_KEYWORDS.joinToString("|", transform = Regex::escape)})""" +
                """\s*(?:금액|액)?\s*[:：]?\s*""" +
                """(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원?"""
        )
        const val GENERIC_CONFIDENCE = 0.5
    }
}
