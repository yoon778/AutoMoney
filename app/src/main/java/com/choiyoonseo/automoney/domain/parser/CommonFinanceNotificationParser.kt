package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.ParsedBankMovement
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.notification.FinancialAppRegistry

class CommonFinanceNotificationParser(
    private val bankAccountHintExtractor: BankAccountHintExtractor = BankAccountHintExtractor()
) : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        FinancialAppRegistry.infoForPackage(snapshot.packageName)?.bankProvider != null

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

        val provider = FinancialAppRegistry.providerCandidateForPackage(snapshot.packageName)
            ?: return ParseResult.Ignored("unsupported package")
        val nonMovementEventText = selectNonMovementEventLine(snapshot)
        val hasNonMovementCandidate = text.lineSequence().any(::isNonMovementEventLine)
        if (nonMovementEventText == null && hasNonMovementCandidate) {
            return ParseResult.Ignored("ambiguous finance notification")
        }
        val eventText = nonMovementEventText ?: text
        val hasNonMovementKeyword = nonMovementEventText != null
        val movement = if (hasNonMovementKeyword) {
            null
        } else {
            bankAccountHintExtractor.extract(provider, text)
        }
        if (
            movement == null &&
            !hasNonMovementKeyword &&
            bankAccountHintExtractor.hasMovementCandidate(text) &&
            bankAccountHintExtractor.hasAccountReference(text) &&
            AMOUNT_REGEX.findAll(text).count() != 1
        ) {
            // 금액이 여러 개(잔액 등)면 임의 선택 금지 규칙에 따라 버리고,
            // 금액이 1개뿐이면 아래 keyword 분기로 넘겨 검토함 후보로 만든다
            return ParseResult.Ignored("ambiguous bank movement")
        }

        val amountMatch = extractAmountMatch(eventText) ?: return ParseResult.Ignored("amount not found")
        val amount = amountMatch.amount
        val hash = snapshot.sourceNotificationHash
        val merchant = extractMerchant(eventText, amountMatch).ifBlank {
            if (containsAny(eventText, PAYMENT_KEYWORDS)) {
                extractFollowingMerchant(snapshot.combinedText, eventText)
            } else {
                ""
            }
        }
        val memoSource = eventText.lineSequence()
            .firstOrNull { it.contains(amountMatch.matchedText) } ?: eventText
        val maskedMemo = SensitiveTextMasker.mask(memoSource.trim())

        return when {
            nonMovementEventText != null && containsAny(eventText, REFUND_KEYWORDS) -> parsed(
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

            nonMovementEventText != null && containsAny(eventText, TOPUP_KEYWORDS) -> parsed(
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

            movement != null -> parsedMovement(snapshot, movement, hash)

            containsAny(eventText, DEPOSIT_KEYWORDS) -> parsed(
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

            containsAny(eventText, TRANSFER_KEYWORDS) || hasAccountHint(eventText) -> parsed(
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

            nonMovementEventText != null &&
                containsAny(eventText, PAYMENT_KEYWORDS) && merchant.isNotBlank() -> parsed(
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
        hash: String,
        bankAccountHint: BankAccountHint? = null
    ): ParseResult.Parsed = ParseResult.Parsed(
        TransactionDraft(
            occurredAt = snapshot.postedAt,
            amount = amount,
            direction = direction,
            type = type,
            category = if (type == TransactionType.EXPENSE) guessCategory(merchant.orEmpty()) else null,
            paymentMethod = FinancialAppRegistry.providerCandidateForPackage(snapshot.packageName)?.badgeText,
            merchant = merchant,
            counterparty = counterparty,
            memo = memo,
            sourceApp = snapshot.packageName,
            sourceNotificationHash = hash,
            status = status,
            confidence = confidence,
            monthKey = snapshot.postedAt.toKoreanMonthKey(),
            reviewReason = reviewReason,
            bankAccountHint = bankAccountHint
        )
    )

    private fun parsedMovement(
        snapshot: NotificationSnapshot,
        movement: ParsedBankMovement,
        hash: String
    ): ParseResult.Parsed {
        val (type, direction, reviewReason) = when {
            movement.hint.eventKind == BankEventKind.DEPOSIT &&
                movement.hint.direction == AccountMovementDirection.CREDIT ->
                MovementSemantics(
                    TransactionType.INCOME,
                    TransactionDirection.INCOME,
                    ReviewReason.INCOME_UNKNOWN
                )

            movement.hint.eventKind == BankEventKind.TRANSFER &&
                movement.hint.direction != AccountMovementDirection.UNKNOWN ->
                MovementSemantics(
                    TransactionType.TRANSFER,
                    TransactionDirection.NEUTRAL,
                    ReviewReason.TRANSFER_UNKNOWN
                )

            movement.hint.eventKind == BankEventKind.WITHDRAWAL &&
                movement.hint.direction == AccountMovementDirection.DEBIT ->
                MovementSemantics(
                    TransactionType.EXPENSE,
                    TransactionDirection.EXPENSE,
                    ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN
                )

            else -> MovementSemantics(
                TransactionType.TRANSFER,
                TransactionDirection.NEUTRAL,
                ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN
            )
        }
        return parsed(
            snapshot = snapshot,
            amount = MoneyAmount(movement.amountWon),
            direction = direction,
            type = type,
            status = TransactionStatus.NEEDS_REVIEW,
            confidence = 0.7,
            reviewReason = reviewReason,
            memo = null,
            hash = hash,
            bankAccountHint = movement.hint
        )
    }

    private fun extractAmountMatch(text: String): AmountMatch? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        return AmountMatch(
            amount = MoneyAmount(match.groupValues[1].replace(",", "").toLong()),
            matchedText = match.value
        )
    }

    private fun selectNonMovementEventLine(snapshot: NotificationSnapshot): String? {
        listOf(snapshot.text, snapshot.title, snapshot.bigText).forEach { content ->
            val candidates = content.orEmpty()
                .lineSequence()
                .map(String::trim)
                .filter(::isNonMovementEventLine)
                .toList()
            if (candidates.isEmpty()) return@forEach
            if (candidates.size == 1) {
                val candidate = candidates.single()
                val hasMultipleSemantics = listOf(
                    REFUND_KEYWORDS,
                    TOPUP_KEYWORDS,
                    PAYMENT_KEYWORDS
                ).count { containsAny(candidate, it) } > 1
                if (hasMultipleSemantics && AMOUNT_REGEX.findAll(candidate).count() > 1) {
                    val paymentClause = candidate.split(CLAUSE_SEPARATOR_REGEX)
                        .map(String::trim)
                        .singleOrNull { clause ->
                            containsAny(clause, PAYMENT_KEYWORDS) &&
                                !containsAny(clause, REFUND_KEYWORDS) &&
                                AMOUNT_REGEX.findAll(clause).count() == 1
                        }
                    if (paymentClause != null) return paymentClause
                    val withoutSecondaryDetail = SECONDARY_EVENT_DETAIL_REGEX
                        .replace(candidate, "")
                        .trim()
                    return withoutSecondaryDetail.takeIf { line ->
                        containsAny(line, PAYMENT_KEYWORDS) &&
                            AMOUNT_REGEX.findAll(line).count() == 1
                    }
                }
                return candidate
            }
            return candidates.singleOrNull { candidate ->
                containsAny(candidate, PAYMENT_KEYWORDS) &&
                    !containsAny(candidate, REFUND_KEYWORDS) &&
                    !containsAny(candidate, TOPUP_KEYWORDS)
            }
        }
        return null
    }

    private fun isNonMovementEventLine(line: String): Boolean =
        containsAny(line, NON_MOVEMENT_HINT_KEYWORDS) &&
            AMOUNT_REGEX.containsMatchIn(line) &&
            !containsAny(line, NON_EVENT_AMOUNT_KEYWORDS)

    private fun extractMerchant(text: String, amountMatch: AmountMatch): String {
        val line = text.lineSequence().firstOrNull { it.contains(amountMatch.matchedText) } ?: text
        val beforeAmount = cleanNameCandidate(line.substringBefore(amountMatch.matchedText))
        val afterAmount = cleanNameCandidate(line.substringAfter(amountMatch.matchedText, ""))
        return beforeAmount.ifBlank { afterAmount }
    }

    private fun extractFollowingMerchant(text: String, eventText: String): String {
        val lines = text.lineSequence().map(String::trim).toList()
        val eventIndex = lines.indexOfFirst { it == eventText.trim() }
        if (eventIndex < 0) return ""
        val candidate = lines.getOrNull(eventIndex + 1).orEmpty()
        if (
            candidate.isBlank() ||
            candidate.none(Char::isLetter) ||
            AMOUNT_REGEX.containsMatchIn(candidate) ||
            containsAny(candidate, NON_MERCHANT_LINE_KEYWORDS)
        ) {
            return ""
        }
        return cleanNameCandidate(candidate)
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
        private val REFUND_KEYWORDS = listOf("\ucde8\uc18c", "\ud658\ubd88", "\ud658\uae09")
        private val ACCOUNT_KEYWORDS = listOf("\uacc4\uc88c", "\uacc4\uc88c\ubc88\ud638")
        private val PROMOTION_KEYWORDS = listOf("\ucfe0\ud3f0", "\ud61c\ud0dd", "\uc774\ubca4\ud2b8", "\uad11\uace0")
        private val NAME_NOISE_WORDS = PAYMENT_KEYWORDS + TRANSFER_KEYWORDS + DEPOSIT_KEYWORDS +
            TOPUP_KEYWORDS + REFUND_KEYWORDS + ACCOUNT_KEYWORDS
        private val NON_MOVEMENT_HINT_KEYWORDS = PAYMENT_KEYWORDS + TOPUP_KEYWORDS + REFUND_KEYWORDS
        private val NON_EVENT_AMOUNT_KEYWORDS = listOf("사용가능", "이용가능", "한도")
        private val NON_MERCHANT_LINE_KEYWORDS =
            listOf("카드", "출금가능", "사용가능", "이용가능", "잔액", "잔고", "한도")
        private val CLAUSE_SEPARATOR_REGEX = Regex("[/·]")
        private val SECONDARY_EVENT_DETAIL_REGEX =
            Regex("""(?:[/·,]\s*)?(?:캐시백|환급|적립).*$""")
    }

    private data class AmountMatch(
        val amount: MoneyAmount,
        val matchedText: String
    )

    private data class MovementSemantics(
        val type: TransactionType,
        val direction: TransactionDirection,
        val reviewReason: ReviewReason
    )
}
