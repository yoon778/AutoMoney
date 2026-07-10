package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class TossNotificationParserTest {
    private val parser = TossNotificationParser()

    @Test
    fun parsesCardPaymentAsAutoConfirmedExpense() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스뱅크 체크카드",
                text = "스타벅스 홍대입구 6,100원 결제",
                bigText = "스타벅스 홍대입구 6,100원 결제\n06.27 12:47",
                postedAt = Instant.parse("2026-06-27T03:47:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6100)
        assertThat(draft.merchant).isEqualTo("스타벅스 홍대입구")
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.category).isEqualTo(Category.CAFE_SNACK)
        assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
        assertThat(draft.bankAccountHint).isNull()
    }

    @Test
    fun parsesTransferAsNeedsReview() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스",
                text = "김민수님에게 10,000원 송금했어요",
                bigText = null,
                postedAt = Instant.parse("2026-06-27T12:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(10000)
        assertThat(draft.counterparty).isEqualTo("김민수")
        assertThat(draft.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.bankAccountHint).isNull()
    }

    @Test
    fun parsesWalletTopupAsNeutralReviewItem() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "네이버페이",
                text = "네이버페이 10,000원 충전",
                bigText = null,
                postedAt = Instant.parse("2026-06-27T12:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(draft.direction).isEqualTo(TransactionDirection.NEUTRAL)
        assertThat(draft.category).isNull()
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.WALLET_TOPUP)
    }

    @Test
    fun normalizesWalletTopupMerchantNamesForReview() {
        val naverPay = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스",
                text = "네이버페이 포인트 10,000원 충전 완료",
                bigText = null,
                postedAt = Instant.parse("2026-06-27T12:00:00Z")
            )
        ) as ParseResult.Parsed

        val kakaoPay = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스",
                text = "카카오페이머니 20,000원 충전했어요",
                bigText = null,
                postedAt = Instant.parse("2026-06-27T12:05:00Z")
            )
        ) as ParseResult.Parsed

        assertThat(naverPay.draft.merchant).isEqualTo("네이버페이")
        assertThat(kakaoPay.draft.merchant).isEqualTo("카카오페이")
        assertThat(naverPay.draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(kakaoPay.draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
    }

    @Test
    fun parsesTossPayPaymentWithAmountBeforeActionAsAutoConfirmedExpense() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스페이",
                text = "GS25 합정역점에서 4,800원 토스페이로 결제했어요",
                bigText = null,
                postedAt = Instant.parse("2026-07-01T03:47:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(4800)
        assertThat(draft.merchant).isEqualTo("GS25 합정역점")
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.category).isEqualTo(Category.FOOD)
        assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
    }

    @Test
    fun routesPaymentGatewayPaymentToReview() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스뱅크 체크카드",
                text = "KCP 35,000원 결제",
                bigText = "KCP 35,000원 결제\n온라인 결제",
                postedAt = Instant.parse("2026-07-01T04:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.merchant).isEqualTo("KCP")
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.PAYMENT_GATEWAY)
    }

    @Test
    fun parsesCancelAsRefundReviewWithMerchant() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스뱅크 체크카드",
                text = "쿠팡 12,000원 결제 취소",
                bigText = null,
                postedAt = Instant.parse("2026-07-01T05:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.merchant).isEqualTo("쿠팡")
        assertThat(draft.type).isEqualTo(TransactionType.REFUND)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.REFUND_OR_CANCEL)
    }

    @Test
    fun normalizesPayMoneyTopupVariantsForReview() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스",
                text = "네이버페이머니에 10,000원 충전됐어요",
                bigText = null,
                postedAt = Instant.parse("2026-07-01T06:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.merchant).isEqualTo("네이버페이")
        assertThat(draft.type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.WALLET_TOPUP)
    }

    @Test
    fun explicitTossBankMovementProducesHintAndUsesMovementAmount() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "잔액 90,000원",
                text = "토스뱅크 계좌 123-***-4567\n10,000원 출금",
                bigText = null,
                postedAt = Instant.parse("2026-07-01T07:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(10_000)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN)
        assertThat(draft.bankAccountHint?.provider).isEqualTo(BankProvider.TOSS_BANK)
        assertThat(draft.bankAccountHint?.direction).isEqualTo(AccountMovementDirection.DEBIT)
        assertThat(draft.bankAccountHint?.eventKind).isEqualTo(BankEventKind.WITHDRAWAL)
    }

    @Test
    fun tossPackageAndAccountTextAloneDoNotInferTossBank() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = "viva.republica.toss",
                title = "토스",
                text = "계좌 123-***-4567\n10,000원 출금",
                bigText = null,
                postedAt = Instant.parse("2026-07-01T08:00:00Z")
            )
        )

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported toss notification"))
    }
}
