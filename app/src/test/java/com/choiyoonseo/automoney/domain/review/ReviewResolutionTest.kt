package com.choiyoonseo.automoney.domain.review

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

class ReviewResolutionTest {

    @Test
    fun settlementResolutionAppendsUserMemo() {
        val updated = transaction(
            type = TransactionType.TRANSFER,
            direction = TransactionDirection.NEUTRAL,
            memo = "송금 목적 확인 필요"
        ).resolveReview(
            resolution = ReviewResolution.SETTLEMENT,
            userMemo = "친구가 결제한 저녁값"
        )

        assertThat(updated.type).isEqualTo(TransactionType.SETTLEMENT)
        assertThat(updated.direction).isEqualTo(TransactionDirection.NEUTRAL)
        assertThat(updated.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(updated.memo).isEqualTo("송금 목적 확인 필요 · N분의1 정산으로 확인 · 친구가 결제한 저녁값")
    }

    @Test
    fun incomeConfirmationKeepsIncomeAndAppendsReason() {
        val updated = transaction(
            type = TransactionType.INCOME,
            direction = TransactionDirection.INCOME,
            memo = "입금 확인 필요"
        ).resolveReview(
            resolution = ReviewResolution.CONFIRM,
            userMemo = "친구 정산금"
        )

        assertThat(updated.type).isEqualTo(TransactionType.INCOME)
        assertThat(updated.direction).isEqualTo(TransactionDirection.INCOME)
        assertThat(updated.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(updated.memo).isEqualTo("입금 확인 필요 · 사용자 확인 · 친구 정산금")
    }

    @Test
    fun excludeResolutionAppendsReason() {
        val updated = transaction(
            type = TransactionType.TRANSFER,
            direction = TransactionDirection.NEUTRAL,
            memo = null
        ).resolveReview(
            resolution = ReviewResolution.EXCLUDE,
            userMemo = "내 계좌 간 이동"
        )

        assertThat(updated.type).isEqualTo(TransactionType.EXCLUDED)
        assertThat(updated.direction).isEqualTo(TransactionDirection.NEUTRAL)
        assertThat(updated.status).isEqualTo(TransactionStatus.EXCLUDED)
        assertThat(updated.category).isNull()
        assertThat(updated.memo).isEqualTo("지출 제외 · 내 계좌 간 이동")
    }

    @Test
    fun accountTransferResolutionKeepsTransferAndAppendsAccounts() {
        val updated = transaction(
            type = TransactionType.TRANSFER,
            direction = TransactionDirection.NEUTRAL,
            memo = "needs account transfer review"
        ).resolveReview(
            resolution = ReviewResolution.ACCOUNT_TRANSFER,
            userMemo = "KB -> Kakao"
        )

        assertThat(updated.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(updated.direction).isEqualTo(TransactionDirection.NEUTRAL)
        assertThat(updated.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(updated.memo).contains("KB -> Kakao")
    }

    private fun transaction(
        type: TransactionType,
        direction: TransactionDirection,
        memo: String?
    ) = MoneyTransaction(
        id = 12,
        occurredAt = Instant.parse("2026-07-06T01:00:00Z"),
        amount = MoneyAmount(10000),
        direction = direction,
        type = type,
        category = Category.OTHER,
        paymentMethod = "국민은행",
        merchant = null,
        counterparty = "김민수",
        memo = memo,
        sourceApp = "com.kbstar.kbbank",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash",
        status = TransactionStatus.NEEDS_REVIEW,
        confidence = 0.8,
        monthKey = YearMonth.of(2026, 7)
    )
}
