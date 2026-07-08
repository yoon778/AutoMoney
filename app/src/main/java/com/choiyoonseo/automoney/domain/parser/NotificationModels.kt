package com.choiyoonseo.automoney.domain.parser

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
    val postedAt: Instant
) {
    val combinedText: String
        get() = listOfNotNull(title, text, bigText).joinToString("\n")
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
    val reviewReason: ReviewReason?
)

sealed interface ParseResult {
    data class Parsed(val draft: TransactionDraft) : ParseResult
    data class Ignored(val reason: String) : ParseResult
}

fun Instant.toKoreanMonthKey(): YearMonth {
    return YearMonth.from(atZone(ZoneId.of("Asia/Seoul")))
}

