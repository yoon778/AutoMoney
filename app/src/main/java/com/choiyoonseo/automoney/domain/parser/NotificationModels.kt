package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class NotificationSnapshot(
    val packageName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val postedAt: Instant,
    val notificationKey: String? = null
) {
    val combinedText: String
        get() {
            // text와 bigText가 같거나 한쪽이 다른 쪽을 포함하면(확장 알림의 일반 패턴)
            // 긴 쪽만 사용 — 같은 금액·문구가 두 번 세어지는 것을 방지
            val body = when {
                text == null -> bigText
                bigText == null -> text
                bigText.contains(text) -> bigText
                text.contains(bigText) -> text
                else -> "$text\n$bigText"
            }
            return listOfNotNull(title, body).joinToString("\n")
        }

    val sourceNotificationHash: String
        get() = notificationIdentityHash(this)
}

data class TransactionDraft(
    val occurredAt: Instant,
    val amount: MoneyAmount,
    val direction: TransactionDirection,
    val type: TransactionType,
    val category: Category?,
    val paymentMethod: String?,
    val merchant: String?,
    val counterparty: String?,
    val memo: String?,
    val sourceApp: String,
    val sourceType: SourceType = SourceType.NOTIFICATION,
    val sourceNotificationHash: String,
    val status: TransactionStatus,
    val confidence: Double,
    val monthKey: YearMonth,
    val reviewReason: ReviewReason?,
    val bankAccountHint: BankAccountHint? = null
)

sealed interface ParseResult {
    data class Parsed(val draft: TransactionDraft) : ParseResult
    data class Ignored(val reason: String) : ParseResult
}

fun Instant.toKoreanMonthKey(): YearMonth {
    return YearMonth.from(atZone(ZoneId.of("Asia/Seoul")))
}

