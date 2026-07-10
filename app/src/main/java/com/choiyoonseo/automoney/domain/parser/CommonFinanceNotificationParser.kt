package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.notification.FinancialAppRegistry

class CommonFinanceNotificationParser : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        snapshot.packageName == FinancialAppRegistry.KB_STAR_BANKING_PACKAGE

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        if (!canParse(snapshot)) {
            return ParseResult.Ignored("unsupported package")
        }

        val text = snapshot.combinedText.trim()
        if (text.isBlank()) {
            return ParseResult.Ignored("empty notification")
        }
        if (isPromotion(text)) {
            return ParseResult.Ignored("promotional notification")
        }

        val amountMatch = extractAmountMatch(text) ?: return ParseResult.Ignored("amount not found")
        val amount = amountMatch.amount
        val hash = snapshot.sourceNotificationHash
        val merchant = extractMerchant(text, amountMatch)
        val memoSource = text.lineSequence().firstOrNull { it.contains(amountMatch.matchedText) } ?: text
        val maskedMemo = SensitiveTextMasker.mask(memoSource.trim())

        return when {
            containsAny(text, REFUND_KEYWORDS) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.REFUND,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.75,
                reviewReason = ReviewReason.REFUND_OR_CANCEL,
                merchant = merchant,
                memo = maskedMemo,
                hash = hash
            )

            containsAny(text, TOPUP_KEYWORDS) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.WALLET_TOPUP,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.8,
                reviewReason = ReviewReason.WALLET_TOPUP,
                merchant = merchant,
                memo = maskedMemo,
                hash = hash
            )

            containsAny(text, DEPOSIT_KEYWORDS) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.INCOME,
                type = TransactionType.INCOME,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.7,
                reviewReason = ReviewReason.INCOME_UNKNOWN,
                counterparty = merchant,
                memo = maskedMemo,
                hash = hash
            )

            containsAny(text, TRANSFER_KEYWORDS) || hasAccountHint(text) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.TRANSFER,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.7,
                reviewReason = ReviewReason.TRANSFER_UNKNOWN,
                counterparty = merchant,
                memo = maskedMemo,
                hash = hash
            )

            containsAny(text, PAYMENT_KEYWORDS) && merchant.isNotBlank() -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.EXPENSE,
                type = TransactionType.EXPENSE,
                status = TransactionStatus.AUTO_CONFIRMED,
                confidence = 0.86,
                reviewReason = null,
                merchant = merchant,
                memo = merchant,
                hash = hash
            )

            else -> ParseResult.Ignored("unsupported finance notification")
        }
    }

    private fun parsed(
        snapshot: NotificationSnapshot,
        amount: MoneyAmount,
        direction: TransactionDirection,
        type: TransactionType,
        status: TransactionStatus,
        confidence: Double,
        reviewReason: ReviewReason?,
        merchant: String? = null,
        counterparty: String? = null,
        memo: String?,
        hash: String
    ): ParseResult.Parsed = ParseResult.Parsed(
        TransactionDraft(
            occurredAt = snapshot.postedAt,
            amount = amount,
            direction = direction,
            type = type,
            category = if (type == TransactionType.EXPENSE) guessCategory(merchant.orEmpty()) else null,
            paymentMethod = "KB",
            merchant = merchant,
            counterparty = counterparty,
            memo = memo,
            sourceApp = snapshot.packageName,
            sourceNotificationHash = hash,
            status = status,
            confidence = confidence,
            monthKey = snapshot.postedAt.toKoreanMonthKey(),
            reviewReason = reviewReason
        )
    )

    private fun extractAmountMatch(text: String): AmountMatch? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        return AmountMatch(
            amount = MoneyAmount(match.groupValues[1].replace(",", "").toLong()),
            matchedText = match.value
        )
    }

    private fun extractMerchant(text: String, amountMatch: AmountMatch): String {
        val line = text.lineSequence().firstOrNull { it.contains(amountMatch.matchedText) } ?: text
        val beforeAmount = cleanNameCandidate(line.substringBefore(amountMatch.matchedText))
        val afterAmount = cleanNameCandidate(line.substringAfter(amountMatch.matchedText, ""))
        return beforeAmount.ifBlank { afterAmount }
    }

    private fun cleanNameCandidate(raw: String): String {
        var candidate = raw.replace("KB", "").trim()
        NAME_NOISE_WORDS.forEach { word ->
            candidate = candidate.replace(word, "", ignoreCase = true)
        }
        candidate = candidate
            .replace(ACCOUNT_LIKE_REGEX, "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':', ',', '.', '[', ']')
        return candidate
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it, ignoreCase = true) }

    private fun isPromotion(text: String): Boolean =
        containsAny(text, PROMOTION_KEYWORDS) && !containsAny(text, PAYMENT_KEYWORDS)

    private fun hasAccountHint(text: String): Boolean =
        containsAny(text, ACCOUNT_KEYWORDS) || ACCOUNT_LIKE_REGEX.containsMatchIn(text)

    private fun guessCategory(merchant: String): Category {
        val lower = merchant.lowercase()
        return when {
            merchant.contains("GS25") || merchant.contains("CU") -> Category.FOOD
            lower.contains("starbucks") || lower.contains("coffee") -> Category.CAFE_SNACK
            else -> Category.OTHER
        }
    }

    companion object {
        private val AMOUNT_REGEX = Regex("""([0-9,]+)\s*\uc6d0""")
        private val ACCOUNT_LIKE_REGEX = Regex("""\d[\d-]{5,}\d""")
        private val PAYMENT_KEYWORDS = listOf("\uacb0\uc81c", "\uc2b9\uc778", "\uc0ac\uc6a9")
        private val TRANSFER_KEYWORDS = listOf("\uc774\uccb4", "\uc1a1\uae08", "\ucd9c\uae08")
        private val DEPOSIT_KEYWORDS = listOf("\uc785\uae08")
        private val TOPUP_KEYWORDS = listOf("\ucda9\uc804", "\ud3ec\uc778\ud2b8", "\ud398\uc774\uba38\ub2c8")
        private val REFUND_KEYWORDS = listOf("\ucde8\uc18c", "\ud658\ubd88")
        private val ACCOUNT_KEYWORDS = listOf("\uacc4\uc88c", "\uacc4\uc88c\ubc88\ud638")
        private val PROMOTION_KEYWORDS = listOf("\ucfe0\ud3f0", "\ud61c\ud0dd", "\uc774\ubca4\ud2b8", "\uad11\uace0")
        private val NAME_NOISE_WORDS = PAYMENT_KEYWORDS + TRANSFER_KEYWORDS + DEPOSIT_KEYWORDS +
            TOPUP_KEYWORDS + REFUND_KEYWORDS + ACCOUNT_KEYWORDS
    }

    private data class AmountMatch(
        val amount: MoneyAmount,
        val matchedText: String
    )
}
