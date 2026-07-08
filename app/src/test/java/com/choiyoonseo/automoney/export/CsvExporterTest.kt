package com.choiyoonseo.automoney.export

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class CsvExporterTest {
    @Test
    fun exportsKoreanHeadersAndTransactionRow() {
        val csv = CsvExporter().export(listOf(transaction()))

        assertThat(csv).contains("날짜,금액,분류,결제 수단,메모,월")
        assertThat(csv).contains("2026-06-27,6100,카페/간식,토스뱅크 체크카드,스타벅스 홍대입구,2026-06")
    }

    private fun transaction() = MoneyTransaction(
        occurredAt = Instant.parse("2026-06-27T03:47:00Z"),
        amount = MoneyAmount(6100),
        direction = TransactionDirection.EXPENSE,
        type = TransactionType.EXPENSE,
        category = Category.CAFE_SNACK,
        paymentMethod = "토스뱅크 체크카드",
        merchant = "스타벅스 홍대입구",
        counterparty = null,
        memo = "스타벅스 홍대입구",
        sourceApp = "viva.republica.toss",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash",
        status = TransactionStatus.AUTO_CONFIRMED,
        confidence = 0.9,
        monthKey = YearMonth.of(2026, 6)
    )
}

