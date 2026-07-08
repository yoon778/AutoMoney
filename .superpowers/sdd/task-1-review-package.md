# Task 1 Review Package

Repository note: no HEAD commit exists; package contains current contents of task-owned files.

## app\src\main\java\com\choiyoonseo\automoney\domain\parser\NotificationParser.kt
```kotlin
package com.choiyoonseo.automoney.domain.parser

interface NotificationParser {
    fun canParse(snapshot: NotificationSnapshot): Boolean

    fun parse(snapshot: NotificationSnapshot): ParseResult
}
```

## app\src\main\java\com\choiyoonseo\automoney\domain\parser\NotificationParserRouter.kt
```kotlin
package com.choiyoonseo.automoney.domain.parser

class NotificationParserRouter(
    private val parsers: List<NotificationParser>
) : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        parsers.any { it.canParse(snapshot) }

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        val parser = parsers.firstOrNull { it.canParse(snapshot) }
            ?: return ParseResult.Ignored("unsupported package")
        return parser.parse(snapshot)
    }
}
```

## app\src\main\java\com\choiyoonseo\automoney\domain\parser\TossNotificationParser.kt
```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.security.MessageDigest

class TossNotificationParser : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        snapshot.packageName == TOSS_PACKAGE

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        if (!canParse(snapshot)) {
            return ParseResult.Ignored("unsupported package")
        }

        val text = snapshot.combinedText.trim()
        val amount = extractAmount(text) ?: return ParseResult.Ignored("amount not found")
        val hash = hash(text)

        if (text.contains("異⑹쟾")) {
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
                    memo = "${walletName ?: "?섏씠"} 異⑹쟾",
                    sourceApp = TOSS_PACKAGE,
                    sourceNotificationHash = hash,
                    status = TransactionStatus.NEEDS_REVIEW,
                    confidence = 0.85,
                    monthKey = snapshot.postedAt.toKoreanMonthKey(),
                    reviewReason = ReviewReason.WALLET_TOPUP
                )
            )
        }

        if (text.contains("?↔툑")) {
            return ParseResult.Parsed(
                TransactionDraft(
                    occurredAt = snapshot.postedAt,
                    amount = amount,
                    direction = TransactionDirection.NEUTRAL,
                    type = TransactionType.TRANSFER,
                    category = null,
                    paymentMethod = "怨꾩쥖?댁껜",
                    merchant = null,
                    counterparty = extractCounterparty(text),
                    memo = "?↔툑 紐⑹쟻 ?뺤씤 ?꾩슂",
                    sourceApp = TOSS_PACKAGE,
                    sourceNotificationHash = hash,
                    status = TransactionStatus.NEEDS_REVIEW,
                    confidence = 0.75,
                    monthKey = snapshot.postedAt.toKoreanMonthKey(),
                    reviewReason = ReviewReason.TRANSFER_UNKNOWN
                )
            )
        }

        if (text.contains("痍⑥냼") || text.contains("?섎텋")) {
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
                    memo = "?섎텋/痍⑥냼 ?뺤씤 ?꾩슂",
                    sourceApp = TOSS_PACKAGE,
                    sourceNotificationHash = hash,
                    status = TransactionStatus.NEEDS_REVIEW,
                    confidence = 0.75,
                    monthKey = snapshot.postedAt.toKoreanMonthKey(),
                    reviewReason = ReviewReason.REFUND_OR_CANCEL
                )
            )
        }

        if (text.contains("寃곗젣")) {
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

    private fun extractAmount(text: String): MoneyAmount? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        return MoneyAmount(match.groupValues[1].replace(",", "").toLong())
    }

    private fun extractMerchant(text: String, amount: MoneyAmount): String {
        val compactText = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.contains("??) && (it.contains("寃곗젣") || it.contains("痍⑥냼") || it.contains("?섎텋")) }
            ?: text
        val amountText = "%,d??.format(amount.won)
        return compactText
            .substringBefore(amountText)
            .trim()
            .removeSuffix("?먯꽌")
            .trim()
            .ifBlank { "?????녿뒗 媛留뱀젏" }
    }

    private fun extractCounterparty(text: String): String? {
        return Regex("""(.+?)?섏뿉寃?"").find(text)?.groupValues?.get(1)?.trim()
            ?: Regex("""(.+?)?먭쾶""").find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractWalletName(text: String): String? {
        val rawName = text.substringBefore("異⑹쟾")
            .replace(AMOUNT_REGEX, "")
            .replace("?꾨즺", "")
            .replace("?덉뼱??, "")
            .trim()

        return when {
            rawName.contains("?ㅼ씠踰꾪럹??) -> "?ㅼ씠踰꾪럹??
            rawName.contains("移댁뭅?ㅽ럹??) -> "移댁뭅?ㅽ럹??
            rawName.contains("?섏씠肄?, ignoreCase = true) || rawName.contains("PAYCO", ignoreCase = true) -> "?섏씠肄?
            rawName.contains("?좎뒪?섏씠") -> "?좎뒪?섏씠"
            rawName.contains("?ъ씤??) -> "?ъ씤??
            else -> rawName.ifBlank { null }
        }
    }

    private fun guessCategory(merchant: String): Category {
        val lower = merchant.lowercase()
        return when {
            merchant.contains("?ㅽ?踰낆뒪") || merchant.contains("移댄럹") || lower.contains("coffee") -> Category.CAFE_SNACK
            merchant.contains("踰꾩뒪") || merchant.contains("吏?섏쿋") || merchant.contains("?앹떆") -> Category.TRANSPORT
            merchant.contains("GS25") || merchant.contains("CU") || merchant.contains("?앸떦") -> Category.FOOD
            merchant.contains("?щ━釉뚯쁺") -> Category.BEAUTY
            merchant.contains("荑좏뙜") || merchant.contains("?ㅼ씠踰꾪럹??) -> Category.SHOPPING
            else -> Category.OTHER
        }
    }

    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val TOSS_PACKAGE = "viva.republica.toss"
        private val AMOUNT_REGEX = Regex("""([0-9,]+)??"")
        private val PAYMENT_GATEWAYS = listOf("KCP", "NICE", "KG?대땲?쒖뒪", "?좎뒪?섏씠癒쇱툩")
    }
}
```

## app\src\test\java\com\choiyoonseo\automoney\domain\parser\NotificationParserRouterTest.kt
```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class NotificationParserRouterTest {
    @Test
    fun routesToFirstParserThatCanParse() {
        val parser = RecordingParser(canParse = true, result = ParseResult.Ignored("handled"))
        val fallback = RecordingParser(canParse = true, result = ParseResult.Ignored("fallback"))
        val router = NotificationParserRouter(listOf(parser, fallback))

        val result = router.parse(snapshot("viva.republica.toss"))

        assertThat(result).isEqualTo(ParseResult.Ignored("handled"))
        assertThat(parser.parseCalls).isEqualTo(1)
        assertThat(fallback.parseCalls).isEqualTo(0)
    }

    @Test
    fun ignoresWhenNoParserCanParse() {
        val router = NotificationParserRouter(listOf(RecordingParser(canParse = false)))

        val result = router.parse(snapshot("unknown.package"))

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    private fun snapshot(packageName: String) = NotificationSnapshot(
        packageName = packageName,
        title = "title",
        text = "text",
        bigText = null,
        postedAt = Instant.parse("2026-07-03T01:00:00Z")
    )

    private class RecordingParser(
        private val canParse: Boolean,
        private val result: ParseResult = ParseResult.Ignored("unused")
    ) : NotificationParser {
        var parseCalls = 0

        override fun canParse(snapshot: NotificationSnapshot): Boolean = canParse

        override fun parse(snapshot: NotificationSnapshot): ParseResult {
            parseCalls += 1
            return result
        }
    }
}
```

