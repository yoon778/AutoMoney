package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.ParsedBankMovement
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType

class TossNotificationParser(
    private val bankAccountHintExtractor: BankAccountHintExtractor = BankAccountHintExtractor()
) : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        snapshot.packageName == TOSS_PACKAGE

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        if (!canParse(snapshot)) {
            return ParseResult.Ignored("unsupported package")
        }

        val text = snapshot.combinedText.trim()
        val amount = extractAmount(text) ?: return ParseResult.Ignored("amount not found")
        val hash = snapshot.sourceNotificationHash
        val movement = if (text.contains("충전") || text.contains("취소") ||
            text.contains("환불") || text.contains("결제")
        ) {
            null
        } else {
            bankAccountHintExtractor.resolveAggregatorProvider(text)?.let { provider ->
                bankAccountHintExtractor.extract(provider, text)
            }
        }

        if (text.contains("충전")) {
            val walletName = extractWalletName(text)
            return ParseResult.Parsed(
                TransactionDraft(
                    occurredAt = snapshot.postedAt,
                    amount = amount,
                    direction = TransactionDirection.NEUTRAL,
                    type = TransactionType.WALLET_TOPUP,
                    category = null,
                    paymentMethod = snapshot.title,
                    merchant = walletName,
                    counterparty = null,
                    memo = "${walletName ?: "페이"} 충전",
                    sourceApp = TOSS_PACKAGE,
                    sourceNotificationHash = hash,
                    status = TransactionStatus.NEEDS_REVIEW,
                    confidence = 0.85,
                    monthKey = snapshot.postedAt.toKoreanMonthKey(),
                    reviewReason = ReviewReason.WALLET_TOPUP
                )
            )
        }

        if (movement != null) {
            return parsedMovement(snapshot, movement, hash)
        }

        if (text.contains("송금")) {
            return ParseResult.Parsed(
                TransactionDraft(
                    occurredAt = snapshot.postedAt,
                    amount = amount,
                    direction = TransactionDirection.NEUTRAL,
                    type = TransactionType.TRANSFER,
                    category = null,
                    paymentMethod = "계좌이체",
                    merchant = null,
                    counterparty = extractCounterparty(text),
                    memo = "송금 목적 확인 필요",
                    sourceApp = TOSS_PACKAGE,
                    sourceNotificationHash = hash,
                    status = TransactionStatus.NEEDS_REVIEW,
                    confidence = 0.75,
                    monthKey = snapshot.postedAt.toKoreanMonthKey(),
                    reviewReason = ReviewReason.TRANSFER_UNKNOWN
                )
            )
        }

        if (text.contains("취소") || text.contains("환불")) {
            return ParseResult.Parsed(
                TransactionDraft(
                    occurredAt = snapshot.postedAt,
                    amount = amount,
                    direction = TransactionDirection.NEUTRAL,
                    type = TransactionType.REFUND,
                    category = null,
                    paymentMethod = snapshot.title,
                    merchant = extractMerchant(text, amount),
                    counterparty = null,
                    memo = "환불/취소 확인 필요",
                    sourceApp = TOSS_PACKAGE,
                    sourceNotificationHash = hash,
                    status = TransactionStatus.NEEDS_REVIEW,
                    confidence = 0.75,
                    monthKey = snapshot.postedAt.toKoreanMonthKey(),
                    reviewReason = ReviewReason.REFUND_OR_CANCEL
                )
            )
        }

        if (text.contains("결제")) {
            val merchant = extractMerchant(text, amount)
            val isGateway = PAYMENT_GATEWAYS.any { merchant.contains(it, ignoreCase = true) }
            return ParseResult.Parsed(
                TransactionDraft(
                    occurredAt = snapshot.postedAt,
                    amount = amount,
                    direction = TransactionDirection.EXPENSE,
                    type = TransactionType.EXPENSE,
                    category = guessCategory(merchant),
                    paymentMethod = snapshot.title,
                    merchant = merchant,
                    counterparty = null,
                    memo = merchant,
                    sourceApp = TOSS_PACKAGE,
                    sourceNotificationHash = hash,
                    status = if (isGateway) TransactionStatus.NEEDS_REVIEW else TransactionStatus.AUTO_CONFIRMED,
                    confidence = if (isGateway) 0.55 else 0.9,
                    monthKey = snapshot.postedAt.toKoreanMonthKey(),
                    reviewReason = if (isGateway) ReviewReason.PAYMENT_GATEWAY else null
                )
            )
        }

        return ParseResult.Ignored("unsupported toss notification")
    }

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
        return ParseResult.Parsed(
            TransactionDraft(
                occurredAt = snapshot.postedAt,
                amount = MoneyAmount(movement.amountWon),
                direction = direction,
                type = type,
                category = null,
                paymentMethod = movement.hint.provider.badgeText,
                merchant = null,
                counterparty = null,
                memo = null,
                sourceApp = TOSS_PACKAGE,
                sourceNotificationHash = hash,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.7,
                monthKey = snapshot.postedAt.toKoreanMonthKey(),
                reviewReason = reviewReason,
                bankAccountHint = movement.hint
            )
        )
    }

    private fun extractAmount(text: String): MoneyAmount? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        return MoneyAmount(match.groupValues[1].replace(",", "").toLong())
    }

    private fun extractMerchant(text: String, amount: MoneyAmount): String {
        val compactText = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.contains("원") && (it.contains("결제") || it.contains("취소") || it.contains("환불")) }
            ?: text
        val amountText = "%,d원".format(amount.won)
        return compactText
            .substringBefore(amountText)
            .trim()
            .removeSuffix("에서")
            .trim()
            .ifBlank { "알 수 없는 가맹점" }
    }

    private fun extractCounterparty(text: String): String? {
        return Regex("""(.+?)님에게""").find(text)?.groupValues?.get(1)?.trim()
            ?: Regex("""(.+?)에게""").find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractWalletName(text: String): String? {
        val rawName = text.substringBefore("충전")
            .replace(AMOUNT_REGEX, "")
            .replace("완료", "")
            .replace("했어요", "")
            .trim()

        return when {
            rawName.contains("네이버페이") -> "네이버페이"
            rawName.contains("카카오페이") -> "카카오페이"
            rawName.contains("페이코", ignoreCase = true) || rawName.contains("PAYCO", ignoreCase = true) -> "페이코"
            rawName.contains("토스페이") -> "토스페이"
            rawName.contains("포인트") -> "포인트"
            else -> rawName.ifBlank { null }
        }
    }

    private fun guessCategory(merchant: String): Category {
        val lower = merchant.lowercase()
        return when {
            merchant.contains("스타벅스") || merchant.contains("카페") || lower.contains("coffee") -> Category.CAFE_SNACK
            merchant.contains("버스") || merchant.contains("지하철") || merchant.contains("택시") -> Category.TRANSPORT
            merchant.contains("GS25") || merchant.contains("CU") || merchant.contains("식당") -> Category.FOOD
            merchant.contains("올리브영") -> Category.BEAUTY
            merchant.contains("쿠팡") || merchant.contains("네이버페이") -> Category.SHOPPING
            else -> Category.OTHER
        }
    }

    companion object {
        const val TOSS_PACKAGE = "viva.republica.toss"
        private val AMOUNT_REGEX = Regex("""([0-9,]+)원""")
        private val PAYMENT_GATEWAYS = listOf("KCP", "NICE", "KG이니시스", "토스페이먼츠")
    }

    private data class MovementSemantics(
        val type: TransactionType,
        val direction: TransactionDirection,
        val reviewReason: ReviewReason
    )
}
