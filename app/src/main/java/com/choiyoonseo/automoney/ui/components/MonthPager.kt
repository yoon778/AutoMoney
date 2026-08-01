package com.choiyoonseo.automoney.ui.components

import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal const val MonthPagerPageCount = Int.MAX_VALUE
internal const val MonthPagerInitialPage = MonthPagerPageCount / 2

internal fun monthForPagerPage(
    page: Int,
    anchorMonth: YearMonth
): YearMonth = anchorMonth.plusMonths((page - MonthPagerInitialPage).toLong())

internal fun pagerPageForMonth(
    month: YearMonth,
    anchorMonth: YearMonth
): Int = (MonthPagerInitialPage.toLong() + ChronoUnit.MONTHS.between(anchorMonth, month))
    .coerceIn(0, MonthPagerPageCount.toLong() - 1)
    .toInt()

internal fun monthPagerLabel(month: YearMonth): String = "${month.year}년 ${month.monthValue}월"
