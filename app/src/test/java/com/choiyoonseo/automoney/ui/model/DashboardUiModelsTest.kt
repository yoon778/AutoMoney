package com.choiyoonseo.automoney.ui.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DashboardUiModelsTest {

    @Test
    fun dismissReviewCardRemovesOnlyMatchingCard() {
        val remainingCards = dismissReviewCard(sampleReviewCards, "wallet-naverpay")

        assertThat(remainingCards.map { it.id }).containsExactly("transfer-friend-split")
    }

    @Test
    fun spendForDayReturnsMatchingDailySpend() {
        val spend = sampleSpendCalendar.spendForDay(15)

        assertThat(spend?.amountWon).isEqualTo(42300)
        assertThat(spend?.label).isEqualTo("카페/간식")
    }

    @Test
    fun spendForDayReturnsNullWhenNoSpendExists() {
        assertThat(sampleSpendCalendar.spendForDay(2)).isNull()
    }

    @Test
    fun totalSpendWonAddsAllDailySpends() {
        assertThat(sampleSpendCalendar.totalSpendWon()).isEqualTo(286600)
    }
}
