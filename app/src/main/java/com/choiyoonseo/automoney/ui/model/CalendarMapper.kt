package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.categoryDisplayName
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.plannedUseContributions
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import java.time.YearMonth

fun transactionsToSpendCalendar(
    month: YearMonth,
    transactions: List<MoneyTransaction>
): MonthlySpendCalendarUi {
    val dailySpends = plannedUseContributions(transactions)
        .filter { it.transaction.monthKey == month && it.transaction.countsAsActualExpense() }
        .groupBy { it.transaction.occurredAt.atZone(AppDateZoneId).dayOfMonth }
        .map { (day, dayContributions) ->
            val label = if (dayContributions.size == 1) {
                dayContributions.first().transaction.categoryDisplayName() ?: "기타"
            } else {
                "${dayContributions.size}건"
            }
            DailySpendUi(
                day = day,
                amountWon = dayContributions.sumOf { it.amountWon },
                label = label
            )
        }
        .sortedBy { it.day }

    return MonthlySpendCalendarUi(
        monthTitle = "${month.year}년 ${month.monthValue}월",
        daysInMonth = month.lengthOfMonth(),
        firstWeekdayOffset = month.atDay(1).dayOfWeek.value % 7,
        dailySpends = dailySpends,
        yearMonth = month
    )
}
