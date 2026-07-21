package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.notification.FinancialAppKind
import com.choiyoonseo.automoney.notification.FinancialAppRegistry

class SecuritiesNotificationParser : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        FinancialAppRegistry.infoForPackage(snapshot.packageName)?.kind == FinancialAppKind.SECURITIES

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        val appInfo = FinancialAppRegistry.infoForPackage(snapshot.packageName)
            ?.takeIf { it.kind == FinancialAppKind.SECURITIES }
            ?: return ParseResult.Ignored("unsupported package")
        val primaryText = listOfNotNull(snapshot.title, snapshot.text).joinToString("\n").trim()
        val expandedText = listOfNotNull(snapshot.title, snapshot.bigText).joinToString("\n").trim()
        val text = when {
            hasCashUsageAmount(primaryText) -> primaryText
            hasCashUsageAmount(expandedText) -> expandedText
            else -> snapshot.combinedText.trim()
        }
        if (text.isBlank()) return ParseResult.Ignored("empty notification")
        if (BLOCK_KEYWORDS.any { text.contains(it, ignoreCase = true) }) {
            return ParseResult.Ignored("non-final or non-usage securities notification")
        }
        if (!isConfirmedUsage(text)) {
            return ParseResult.Ignored("unconfirmed securities cash usage")
        }

        val amountCandidates = buildList {
            EXPLICIT_USAGE_REGEX.findAll(text).forEach { add(it.amountWon()) }
            TRAILING_USAGE_REGEX.findAll(text).forEach { add(it.amountWon()) }
            if (
                text.contains("매수") &&
                text.contains("체결") &&
                !text.contains("매도")
            ) {
                SETTLEMENT_AMOUNT_REGEX.findAll(text).forEach { add(it.amountWon()) }
            }
        }.filter { it > 0 }.distinct()

        val amountWon = when (amountCandidates.size) {
            0 -> return ParseResult.Ignored("confirmed cash usage amount not found")
            1 -> amountCandidates.single()
            else -> return ParseResult.Ignored("ambiguous cash usage amount")
        }

        return ParseResult.Parsed(
            TransactionDraft(
                occurredAt = snapshot.postedAt,
                amount = MoneyAmount(amountWon),
                direction = TransactionDirection.EXPENSE,
                type = TransactionType.INVESTMENT,
                category = Category.STOCK,
                paymentMethod = appInfo.displayName,
                merchant = null,
                counterparty = null,
                memo = "예수금 투자 사용",
                sourceApp = snapshot.packageName,
                sourceNotificationHash = snapshot.sourceNotificationHash,
                status = TransactionStatus.AUTO_CONFIRMED,
                confidence = 0.94,
                monthKey = snapshot.postedAt.toKoreanMonthKey(),
                reviewReason = null,
                bankAccountHint = null
            )
        )
    }

    private fun MatchResult.amountWon(): Long =
        groupValues[1].replace(",", "").toLongOrNull() ?: 0

    private fun hasCashUsageAmount(text: String): Boolean =
        EXPLICIT_USAGE_REGEX.containsMatchIn(text) ||
            TRAILING_USAGE_REGEX.containsMatchIn(text) ||
            SETTLEMENT_AMOUNT_REGEX.containsMatchIn(text)

    private fun isConfirmedUsage(text: String): Boolean =
        text.contains("차감") || CONFIRMATION_KEYWORDS.any { text.contains(it) }

    private companion object {
        const val AMOUNT = "([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)"
        val EXPLICIT_USAGE_REGEX = Regex(
            "(?:예수금\\s*(?:사용(?:액|금액)?|차감(?:액|금액)?)|사용(?:된)?\\s*예수금)" +
                "\\s*[:：]?\\s*$AMOUNT\\s*원"
        )
        val TRAILING_USAGE_REGEX = Regex(
            "예수금\\s*[:：]?\\s*$AMOUNT\\s*원\\s*(?:사용|차감)"
        )
        val SETTLEMENT_AMOUNT_REGEX = Regex("정산금액\\s*[:：]?\\s*$AMOUNT\\s*원")
        val CONFIRMATION_KEYWORDS = listOf("체결", "완료", "사용됨", "사용되었습니다")
        val BLOCK_KEYWORDS = listOf(
            "매도", "주문접수", "미체결", "정정", "취소", "예약", "예정",
            "이벤트", "광고", "혜택", "실패", "거절"
        )
    }
}
