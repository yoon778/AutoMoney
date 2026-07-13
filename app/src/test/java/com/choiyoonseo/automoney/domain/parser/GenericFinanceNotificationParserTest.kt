package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class GenericFinanceNotificationParserTest {
    private val parser = GenericFinanceNotificationParser()

    @Test
    fun parsesConservativePositiveFixtures() {
        assertParsed("스타벅스 6,100원 결제 완료", TransactionType.EXPENSE, TransactionDirection.EXPENSE, ReviewReason.LOW_CONFIDENCE_CATEGORY)
        assertParsed("10,000원 입금", TransactionType.INCOME, TransactionDirection.INCOME, ReviewReason.INCOME_UNKNOWN)
        assertParsed("10,000원 출금", TransactionType.TRANSFER, TransactionDirection.NEUTRAL, ReviewReason.TRANSFER_UNKNOWN)
        assertParsed("12,000원 결제 취소", TransactionType.REFUND, TransactionDirection.NEUTRAL, ReviewReason.REFUND_OR_CANCEL)
        assertParsed("카카오페이 30,000원 충전", TransactionType.WALLET_TOPUP, TransactionDirection.NEUTRAL, ReviewReason.WALLET_TOPUP)
    }

    @Test
    fun labeledBalanceAmountDoesNotBlockRealBankNotifications() {
        // 실제 은행 알림은 거의 항상 잔액을 함께 표시함 — 잔액으로 명시된 금액은
        // 거래금액 후보에서 확정 제거하고 나머지로 판정한다
        assertParsed("내 통장 출금 30,000원 잔액 20,880원", TransactionType.TRANSFER, TransactionDirection.NEUTRAL, ReviewReason.TRANSFER_UNKNOWN)
        assertParsed("입금 30,000원\n잔액 50,880원", TransactionType.INCOME, TransactionDirection.INCOME, ReviewReason.INCOME_UNKNOWN)
        assertParsed("오픈뱅킹출금 30,000원 잔액20,880", TransactionType.TRANSFER, TransactionDirection.NEUTRAL, ReviewReason.TRANSFER_UNKNOWN)
        assertParsed("10,000원 결제\n잔액 50,000원", TransactionType.EXPENSE, TransactionDirection.EXPENSE, ReviewReason.LOW_CONFIDENCE_CATEGORY)
    }

    @Test
    fun ignoresPromotionsFailuresFutureEventsAndAmbiguousAmounts() {
        listOf(
            "지금 결제하면 10,000원 혜택",
            "계좌이체 시 5,000원 할인",
            "송금 이벤트 최대 10,000원",
            "10,000원 결제 실패",
            "오늘 10,000원 송금 예정",
            "10,000원",
            "결제 완료",
            "10,000원 결제 50,000원"
        ).forEach { text ->
            assertThat(parser.parse(snapshot(text = text))).isInstanceOf(ParseResult.Ignored::class.java)
        }
    }

    @Test
    fun requiresAmountAndActionOnSameLine() {
        val result = parser.parse(snapshot(title = "송금 이벤트", text = "최대 10,000원"))

        assertThat(result).isInstanceOf(ParseResult.Ignored::class.java)
    }

    private fun assertParsed(
        text: String,
        type: TransactionType,
        direction: TransactionDirection,
        reason: ReviewReason
    ) {
        val result = parser.parse(snapshot(text = text)) as ParseResult.Parsed
        assertThat(result.draft.type).isEqualTo(type)
        assertThat(result.draft.direction).isEqualTo(direction)
        assertThat(result.draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(result.draft.reviewReason).isEqualTo(reason)
        assertThat(result.draft.merchant).isNull()
        assertThat(result.draft.counterparty).isNull()
        assertThat(result.draft.memo).isNull()
        assertThat(result.draft.bankAccountHint).isNull()
    }

    private fun snapshot(
        title: String? = null,
        text: String
    ) = NotificationSnapshot(
        packageName = "com.example.bank",
        title = title,
        text = text,
        bigText = null,
        postedAt = Instant.parse("2026-07-13T01:00:00Z")
    )
}
