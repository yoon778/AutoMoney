package com.choiyoonseo.automoney.ui.transactions

import com.choiyoonseo.automoney.ui.model.TransactionDateSectionUi
import com.choiyoonseo.automoney.ui.components.monthForPagerPage
import com.choiyoonseo.automoney.ui.components.monthPagerLabel
import com.choiyoonseo.automoney.ui.components.pagerPageForMonth
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

class TransactionMonthPagingTest {

    private val currentMonth = YearMonth.of(2026, 8)

    @Test
    fun adjacentPagesMapToAdjacentMonths() {
        val currentPage = pagerPageForMonth(currentMonth, currentMonth)

        assertThat(monthForPagerPage(currentPage - 1, currentMonth))
            .isEqualTo(YearMonth.of(2026, 7))
        assertThat(monthForPagerPage(currentPage + 1, currentMonth))
            .isEqualTo(YearMonth.of(2026, 9))
    }

    @Test
    fun monthToPageRoundTripPreservesMonth() {
        val month = YearMonth.of(2025, 12)
        val page = pagerPageForMonth(month, currentMonth)

        assertThat(monthForPagerPage(page, currentMonth)).isEqualTo(month)
    }

    @Test
    fun monthLabelIncludesYearAndMonth() {
        assertThat(monthPagerLabel(currentMonth)).isEqualTo("2026년 8월")
    }

    @Test
    fun sectionsForMonthAreSortedByDateAscending() {
        val sections = listOf(
            section(LocalDate.of(2026, 8, 20)),
            section(LocalDate.of(2026, 7, 31)),
            section(LocalDate.of(2026, 8, 2))
        )

        assertThat(transactionSectionsForMonth(sections, currentMonth).map { it.date })
            .containsExactly(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 20))
            .inOrder()
    }

    private fun section(date: LocalDate) = TransactionDateSectionUi(date, "", emptyList())
}
