package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.categoryDisplayName
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.effectiveExpenseWon
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import java.time.YearMonth

fun transactionsToSpendCalendar(
    month: YearMonth,
    transactions: List<MoneyTransaction>
): MonthlySpendCalendarUi {
    val dailySpends = transactions
        .filter { it.monthKey == month && it.countsAsActualExpense() }
        .groupBy { it.occurredAt.atZone(AppDateZoneId).dayOfMonth }
        .map { (day, dayTransactions) ->
            val label = if (dayTransactions.size == 1) {
                dayTransactions.first().categoryDisplayName() ?: "기타"
            } else {
                "${dayTransactions.size}건"
            }
            DailySpendUi(
                day = day,
                amountWon = dayTransactions.sumOf { it.effectiveExpenseWon() },
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
