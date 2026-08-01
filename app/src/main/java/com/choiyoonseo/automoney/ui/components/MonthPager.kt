package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
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

@Composable
internal fun MonthPagerHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전 달")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthPagerLabel(month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MoneyTheme.colors.ink
            )
            Text(
                text = "좌우로 넘겨 월 이동",
                style = MaterialTheme.typography.bodySmall,
                color = MoneyTheme.colors.muted
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "다음 달")
        }
    }
}
