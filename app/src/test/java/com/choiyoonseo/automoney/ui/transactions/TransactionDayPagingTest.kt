package com.choiyoonseo.automoney.ui.transactions

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class TransactionDayPagingTest {

    private val today = LocalDate.of(2026, 8, 1)

    @Test
    fun adjacentPagesMapToAdjacentDates() {
        assertThat(transactionDateForPage(99, today, anchorPage = 100))
            .isEqualTo(LocalDate.of(2026, 7, 31))
        assertThat(transactionDateForPage(101, today, anchorPage = 100))
            .isEqualTo(LocalDate.of(2026, 8, 2))
    }

    @Test
    fun dateToPageRoundTripPreservesDate() {
        val date = LocalDate.of(2025, 12, 25)
        val page = transactionPageForDate(date, today, anchorPage = 1000)

        assertThat(transactionDateForPage(page, today, anchorPage = 1000)).isEqualTo(date)
    }

    @Test
    fun dateLabelIncludesTodayAndWeekday() {
        assertThat(transactionDateLabel(today, today)).isEqualTo("오늘 · 8월 1일 토요일")
        assertThat(transactionDateLabel(today.plusDays(1), today)).isEqualTo("8월 2일 일요일")
    }
}
