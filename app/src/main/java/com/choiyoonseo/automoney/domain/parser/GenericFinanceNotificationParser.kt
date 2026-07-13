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
        val amountMatches = AMOUNT_REGEX.findAll(snapshot.combinedText).toList()
        if (amountMatches.size != 1) return ParseResult.Ignored("ambiguous amount")
        val amountMatch = amountMatches.single()
        val line = snapshot.combinedText.lineSequence()
            .map(String::trim)
            .firstOrNull { it.contains(amountMatch.value) }
            ?: return ParseResult.Ignored("amount line not found")
        if (BLOCK_KEYWORDS.any { line.contains(it, ignoreCase = true) }) {
            return ParseResult.Ignored("blocked context")
        }
        val semantics = semanticsFor(line) ?: return ParseResult.Ignored("strong action not found")
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

    private fun semanticsFor(line: String): Semantics? = when {
        REFUND_KEYWORDS.any(line::contains) -> Semantics(TransactionType.REFUND, TransactionDirection.NEUTRAL, ReviewReason.REFUND_OR_CANCEL)
        TOPUP_KEYWORDS.any(line::contains) -> Semantics(TransactionType.WALLET_TOPUP, TransactionDirection.NEUTRAL, ReviewReason.WALLET_TOPUP)
        DEPOSIT_KEYWORDS.any(line::contains) -> Semantics(TransactionType.INCOME, TransactionDirection.INCOME, ReviewReason.INCOME_UNKNOWN)
        TRANSFER_KEYWORDS.any(line::contains) -> Semantics(TransactionType.TRANSFER, TransactionDirection.NEUTRAL, ReviewReason.TRANSFER_UNKNOWN)
        PAYMENT_KEYWORDS.any(line::contains) -> Semantics(TransactionType.EXPENSE, TransactionDirection.EXPENSE, ReviewReason.LOW_CONFIDENCE_CATEGORY)
        else -> null
    }

    private data class Semantics(
        val type: TransactionType,
        val direction: TransactionDirection,
        val reviewReason: ReviewReason
    )

    private companion object {
        val AMOUNT_REGEX = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
        val PAYMENT_KEYWORDS = listOf("결제", "승인", "사용")
        val DEPOSIT_KEYWORDS = listOf("입금", "받음", "받았")
        val TRANSFER_KEYWORDS = listOf("이체", "송금", "출금", "ATM")
        val REFUND_KEYWORDS = listOf("취소", "환불")
        val TOPUP_KEYWORDS = listOf("충전")
        val BLOCK_KEYWORDS = listOf(
            "혜택", "할인", "이벤트", "쿠폰", "광고", "최대", "적립", "예정",
            "실패", "거절", "한도", "잔액", "잔고", "이용가능"
        )
        const val GENERIC_CONFIDENCE = 0.5
    }
}
