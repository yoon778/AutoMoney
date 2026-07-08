# Task 4 Review Package - Updated After Fix

Repository note: no HEAD commit exists; package contains current contents of task-owned files.
Review finding under re-check: comma-free won amounts should parse amount and merchant correctly.

## app\src\main\java\com\choiyoonseo\automoney\domain\model\MoneyModels.kt
```kotlin
package com.choiyoonseo.automoney.domain.model

import java.time.Instant
import java.time.YearMonth

@JvmInline
value class MoneyAmount(val won: Long) {
    init {
        require(won >= 0) { "MoneyAmount must be zero or positive" }
    }
}

enum class TransactionDirection {
    INCOME,
    EXPENSE,
    NEUTRAL
}

enum class TransactionType(
    val defaultDirection: TransactionDirection,
    val countsAsMonthlyExpense: Boolean
) {
    INCOME(TransactionDirection.INCOME, false),
    EXPENSE(TransactionDirection.EXPENSE, true),
    FIXED_EXPENSE(TransactionDirection.EXPENSE, true),
    SAVING(TransactionDirection.EXPENSE, false),
    INVESTMENT(TransactionDirection.EXPENSE, false),
    TRANSFER(TransactionDirection.NEUTRAL, false),
    WALLET_TOPUP(TransactionDirection.NEUTRAL, false),
    WALLET_SPEND(TransactionDirection.EXPENSE, true),
    SETTLEMENT(TransactionDirection.NEUTRAL, false),
    REFUND(TransactionDirection.NEUTRAL, false),
    EXCLUDED(TransactionDirection.NEUTRAL, false)
}

enum class TransactionStatus {
    AUTO_CONFIRMED,
    NEEDS_REVIEW,
    USER_EDITED,
    EXCLUDED
}

enum class SourceType {
    NOTIFICATION,
    MANUAL,
    IMPORT
}

enum class Category(val displayName: String) {
    FOOD("?앸퉬"),
    CAFE_SNACK("移댄럹/媛꾩떇"),
    TRANSPORT("援먰넻鍮?),
    SHOPPING("?쇳븨"),
    LIVING("?앺솢"),
    HOBBY("痍⑤?/?ш?"),
    BEAUTY("誘몄슜"),
    HEALTH("?섎즺/嫄닿컯"),
    STUDY("?숈뾽"),
    EVENT("?대깽??),
    TELECOM("?듭떊"),
    SUBSCRIPTION("援щ룆"),
    SAVING("?異?),
    STOCK("二쇱떇"),
    COIN("肄붿씤"),
    SALARY("?붽툒"),
    ALLOWANCE("?⑸룉"),
    DISCOUNT("?좎씤"),
    REIMBURSEMENT("?섍툒"),
    OTHER("湲고?")
}

data class MoneyTransaction(
    val id: Long = 0,
    val occurredAt: Instant,
    val amount: MoneyAmount,
    val direction: TransactionDirection,
    val type: TransactionType,
    val category: Category?,
    val paymentMethod: String?,
    val merchant: String?,
    val counterparty: String?,
    val memo: String?,
    val sourceApp: String?,
    val sourceType: SourceType,
    val sourceNotificationHash: String?,
    val status: TransactionStatus,
    val confidence: Double,
    val monthKey: YearMonth
)

data class OpenReviewItem(
    val id: Long,
    val transaction: MoneyTransaction,
    val reason: ReviewReason,
    val createdAt: Instant
)

enum class ReviewReason {
    TRANSFER_UNKNOWN,
    INCOME_UNKNOWN,
    WALLET_TOPUP,
    REFUND_OR_CANCEL,
    DUPLICATE_SUSPECTED,
    LOW_CONFIDENCE_CATEGORY,
    PAYMENT_GATEWAY
}

data class Rule(
    val id: Long = 0,
    val matchType: RuleMatchType,
    val matchValue: String,
    val action: RuleAction,
    val targetValue: String,
    val enabled: Boolean = true
)

enum class RuleMatchType {
    MERCHANT,
    COUNTERPARTY,
    NOTIFICATION_KEYWORD,
    PAYMENT_METHOD
}

enum class RuleAction {
    SET_CATEGORY,
    SET_TRANSACTION_TYPE,
    EXCLUDE,
    MARK_AS_WALLET_TOPUP,
    MARK_AS_SETTLEMENT
}

data class Wallet(
    val id: Long = 0,
    val name: String,
    val type: WalletType,
    val balance: MoneyAmount
)

enum class WalletType {
    PREPAID,
    POINT,
    CASH_LIKE
}
```

## app\src\main\java\com\choiyoonseo\automoney\domain\parser\CommonFinanceNotificationParser.kt
```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.notification.FinancialAppRegistry
import java.security.MessageDigest

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
        val hash = hash(text)
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
        return line.substringBefore(amountMatch.matchedText)
            .replace("KB", "")
            .trim()
            .ifBlank { "" }
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

    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
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
    }

    private data class AmountMatch(
        val amount: MoneyAmount,
        val matchedText: String
    )
}
```

## app\src\test\java\com\choiyoonseo\automoney\domain\parser\CommonFinanceNotificationParserTest.kt
```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class CommonFinanceNotificationParserTest {
    private val parser = CommonFinanceNotificationParser()

    @Test
    fun parsesKbCardPaymentAsAutoConfirmedExpense() {
        val result = parser.parse(snapshot(text = "STARBUCKS 6,100${WON} ${APPROVAL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6100)
        assertThat(draft.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
        assertThat(draft.merchant).isEqualTo("STARBUCKS")
        assertThat(draft.sourceApp).isEqualTo("com.kbstar.kbbank")
    }

    @Test
    fun parsesKbCardPaymentWithoutCommaAsAutoConfirmedExpense() {
        val result = parser.parse(snapshot(text = "STARBUCKS 6100${WON} ${APPROVAL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6100)
        assertThat(draft.merchant).isEqualTo("STARBUCKS")
    }

    @Test
    fun parsesTransferAsNeedsReview() {
        val result = parser.parse(snapshot(text = "Kim 10,000${WON} ${TRANSFER}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.TRANSFER_UNKNOWN)
    }

    @Test
    fun parsesDepositAsNeedsReviewIncome() {
        val result = parser.parse(snapshot(text = "Company 500,000${WON} ${DEPOSIT}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.INCOME)
        assertThat(draft.direction).isEqualTo(TransactionDirection.INCOME)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.INCOME_UNKNOWN)
    }

    @Test
    fun parsesTopupAsNeedsReviewWalletTopup() {
        val result = parser.parse(snapshot(text = "Naver Pay 10,000${WON} ${TOPUP}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.WALLET_TOPUP)
    }

    @Test
    fun parsesCancelAsRefundReview() {
        val result = parser.parse(snapshot(text = "Coupang 12,000${WON} ${CANCEL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.REFUND)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.REFUND_OR_CANCEL)
    }

    @Test
    fun ignoresCouponPromotionWithMoneyAmount() {
        val result = parser.parse(snapshot(text = "Event 10,000${WON} ${COUPON}"))

        assertThat(result).isEqualTo(ParseResult.Ignored("promotional notification"))
    }

    @Test
    fun ignoresUnsupportedPackage() {
        val result = parser.parse(
            snapshot(
                packageName = "com.shopping.adapp",
                text = "STARBUCKS 6,100${WON} ${APPROVAL}"
            )
        )

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    private fun snapshot(
        packageName: String = "com.kbstar.kbbank",
        text: String
    ) = NotificationSnapshot(
        packageName = packageName,
        title = "KB",
        text = text,
        bigText = null,
        postedAt = Instant.parse("2026-07-03T01:00:00Z")
    )

    companion object {
        private const val WON = "\uc6d0"
        private const val APPROVAL = "\uc2b9\uc778"
        private const val TRANSFER = "\uc774\uccb4"
        private const val DEPOSIT = "\uc785\uae08"
        private const val TOPUP = "\ucda9\uc804"
        private const val CANCEL = "\ucde8\uc18c"
        private const val COUPON = "\ucfe0\ud3f0"
    }
}
```

## app\src\main\java\com\choiyoonseo\automoney\ui\model\ReviewItemMapper.kt
```kotlin
package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason

fun openReviewItemsToCards(items: List<OpenReviewItem>): List<ReviewCardUi> =
    items.map { item ->
        val transaction = item.transaction
        when (item.reason) {
            ReviewReason.WALLET_TOPUP -> ReviewCardUi(
                id = "review-${item.id}",
                title = "${transaction.merchant ?: "?섏씠"} 異⑹쟾",
                message = "異⑹쟾 ?뚮┝留??덇퀬 ?ㅼ젣 寃곗젣 ?뚮┝? ?놁쓣 ???덉뼱?? ?ъ슜??湲덉븸留?吏異쒕줈 湲곕줉?댁슂.",
                amountWon = transaction.amount.won,
                tag = "異⑹쟾",
                iconText = "異?,
                primaryAction = "?ъ슜???낅젰",
                secondaryAction = "?꾩쭅 ???",
                detailLines = listOf(
                    "異⑹쟾??${formatWon(transaction.amount.won)}",
                    "?ъ슜???낅젰 ???ㅼ젣 吏異쒕쭔 湲곕줉",
                    "?⑥? 異⑹쟾 ?붿븸? 蹂대쪟"
                ),
                kind = ReviewCardKind.WALLET_TOPUP,
                reviewItemId = item.id,
                sourceTransaction = transaction
            )

            ReviewReason.TRANSFER_UNKNOWN -> ReviewCardUi(
                id = "review-${item.id}",
                title = "${transaction.counterparty ?: "?곷?諛?}?먭쾶 ?↔툑",
                message = "移쒓뎄媛 癒쇱? 寃곗젣???덉씤吏 ?뺤씤???꾩슂?댁슂.",
                amountWon = transaction.amount.won,
                tag = "?↔툑",
                iconText = "??,
                primaryAction = "N遺꾩쓽1",
                secondaryAction = "??吏異??꾨떂",
                kind = ReviewCardKind.TRANSFER,
                reviewItemId = item.id,
                sourceTransaction = transaction
            )

            ReviewReason.REFUND_OR_CANCEL -> ReviewCardUi(
                id = "review-${item.id}",
                title = "${transaction.merchant ?: "?섎텋"} ?뺤씤",
                message = "?섎텋/痍⑥냼 湲덉븸?몄? ?뺤씤???꾩슂?댁슂.",
                amountWon = transaction.amount.won,
                tag = "?섎텋",
                iconText = "??,
                primaryAction = "?섎텋 泥섎━",
                secondaryAction = "蹂대쪟",
                kind = ReviewCardKind.REFUND,
                reviewItemId = item.id,
                sourceTransaction = transaction
            )

            ReviewReason.INCOME_UNKNOWN -> ReviewCardUi(
                id = "review-${item.id}",
                title = transaction.counterparty ?: transaction.merchant ?: "?낃툑 ?뺤씤",
                message = "?먮룞 遺꾨쪟 ?뺤떊????븘 ?뺤씤???꾩슂?댁슂.",
                amountWon = transaction.amount.won,
                tag = "?뺤씤",
                iconText = "?",
                primaryAction = "寃??,
                secondaryAction = "?쒖쇅",
                kind = ReviewCardKind.OTHER,
                reviewItemId = item.id,
                sourceTransaction = transaction
            )

            ReviewReason.DUPLICATE_SUSPECTED,
            ReviewReason.LOW_CONFIDENCE_CATEGORY,
            ReviewReason.PAYMENT_GATEWAY -> ReviewCardUi(
                id = "review-${item.id}",
                title = transaction.merchant ?: transaction.counterparty ?: "嫄곕옒 ?뺤씤",
                message = "?먮룞 遺꾨쪟 ?뺤떊????븘???뺤씤???꾩슂?댁슂.",
                amountWon = transaction.amount.won,
                tag = "?뺤씤",
                iconText = "??,
                primaryAction = "??吏異?,
                secondaryAction = "?쒖쇅",
                kind = ReviewCardKind.OTHER,
                reviewItemId = item.id,
                sourceTransaction = transaction
            )
        }
    }
```

