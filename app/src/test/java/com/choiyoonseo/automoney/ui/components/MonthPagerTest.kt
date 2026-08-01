package com.choiyoonseo.automoney.ui.components

import com.google.common.truth.Truth.assertThat
import java.time.YearMonth
import org.junit.Test

class MonthPagerTest {
    @Test
    fun adjacentPagesMoveOneMonthAndRoundTripToPage() {
        val anchor = YearMonth.of(2026, 8)

        assertThat(monthForPagerPage(MonthPagerInitialPage - 1, anchor))
            .isEqualTo(YearMonth.of(2026, 7))
        assertThat(monthForPagerPage(MonthPagerInitialPage + 1, anchor))
            .isEqualTo(YearMonth.of(2026, 9))
        assertThat(pagerPageForMonth(YearMonth.of(2027, 1), anchor))
            .isEqualTo(MonthPagerInitialPage + 5)
    }

    @Test
    fun labelShowsYearAndMonth() {
        assertThat(monthPagerLabel(YearMonth.of(2026, 8))).isEqualTo("2026년 8월")
    }
}
