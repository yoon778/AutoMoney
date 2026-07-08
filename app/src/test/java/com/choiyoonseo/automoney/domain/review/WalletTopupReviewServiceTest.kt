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

class WalletTopupReviewServiceTest {
    private val service = WalletTopupReviewService()

    @Test
    fun recordUsageCreatesWalletSpendAndKeepsRemainingBalance() {
        val result = service.recordUsage(
            topup = topup(amountWon = 10000),
            usedAmount = MoneyAmount(6000),
            category = Category.CAFE_SNACK,
            merchant = "스타벅스 홍대입구",
            memo = "네이버페이로 커피 결제"
        )

        assertThat(result.remainingAmount.won).isEqualTo(4000)
        assertThat(result.reviewedTopup.type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(result.reviewedTopup.direction).isEqualTo(TransactionDirection.NEUTRAL)
        assertThat(result.reviewedTopup.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(result.reviewedTopup.type.countsAsMonthlyExpense).isFalse()

        val spend = result.walletSpend
        assertThat(spend).isNotNull()
        assertThat(spend!!.amount.won).isEqualTo(6000)
        assertThat(spend.type).isEqualTo(TransactionType.WALLET_SPEND)
        assertThat(spend.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(spend.type.countsAsMonthlyExpense).isTrue()
        assertThat(spend.category).isEqualTo(Category.CAFE_SNACK)
        assertThat(spend.merchant).isEqualTo("스타벅스 홍대입구")
        assertThat(spend.paymentMethod).isEqualTo("네이버페이")
        assertThat(spend.sourceType).isEqualTo(SourceType.MANUAL)
        assertThat(spend.sourceNotificationHash).isNull()
    }

    @Test
    fun rejectsUsageGreaterThanTopupAmount() {
        val error = try {
            service.recordUsage(
                topup = topup(amountWon = 10000),
                usedAmount = MoneyAmount(12000),
                category = Category.FOOD,
                merchant = "식당",
                memo = null
            )
            null
        } catch (e: IllegalArgumentException) {
            e
        }

        assertThat(error).hasMessageThat().contains("used amount cannot exceed topup amount")
    }

    @Test
    fun recordZeroUsageKeepsReasonMemoOnReviewedTopup() {
        val result = service.recordUsage(
            topup = topup(amountWon = 10000),
            usedAmount = MoneyAmount(0),
            category = Category.CAFE_SNACK,
            merchant = "미사용",
            memo = "네이버페이 쇼핑하려고 충전"
        )

        assertThat(result.walletSpend).isNull()
        assertThat(result.remainingAmount.won).isEqualTo(10000)
        assertThat(result.reviewedTopup.memo).contains("네이버페이 쇼핑하려고 충전")
    }

    private fun topup(amountWon: Long) = MoneyTransaction(
        id = 42,
        occurredAt = Instant.parse("2026-06-27T03:47:00Z"),
        amount = MoneyAmount(amountWon),
        direction = TransactionDirection.NEUTRAL,
        type = TransactionType.WALLET_TOPUP,
        category = null,
        paymentMethod = "토스",
        merchant = "네이버페이",
        counterparty = null,
        memo = "네이버페이 충전",
        sourceApp = "viva.republica.toss",
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = "hash",
        status = TransactionStatus.NEEDS_REVIEW,
        confidence = 0.85,
        monthKey = YearMonth.of(2026, 6)
    )
}
