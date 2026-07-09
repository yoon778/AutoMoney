package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choiyoonseo.automoney.ui.model.DailySpendUi
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import com.choiyoonseo.automoney.ui.model.MonthlySpendCalendarUi
import com.choiyoonseo.automoney.ui.model.defaultSelectedDay
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.model.spendForDay
import com.choiyoonseo.automoney.ui.model.totalSpendWon
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import java.time.LocalDate

@Composable
fun SpendingCalendarCard(
    title: String,
    calendar: MonthlySpendCalendarUi,
    modifier: Modifier = Modifier,
    currentDate: LocalDate = LocalDate.now(AppDateZoneId)
) {
    var selectedDay by remember(calendar.monthTitle, calendar.dailySpends, currentDate) {
        mutableStateOf(calendar.defaultSelectedDay(currentDate))
    }
    val selectedSpend = calendar.spendForDay(selectedDay)

    val colors = MoneyTheme.colors
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = colors.ink)
                    Text(
                        calendar.monthTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    formatWon(calendar.totalSpendWon()),
                    color = MoneyCoral,
                    fontWeight = FontWeight.Bold
                )
            }

            CalendarWeekHeader()
            CalendarMonthGrid(
                calendar = calendar,
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it }
            )
            CalendarSelectedSummary(selectedDay, selectedSpend)
        }
    }
}

@Composable
private fun CalendarWeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    calendar: MonthlySpendCalendarUi,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val totalCells = calendar.firstWeekdayOffset + calendar.daysInMonth
    val rowCount = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (column in 0 until 7) {
                    val cellIndex = row * 7 + column
                    val day = cellIndex - calendar.firstWeekdayOffset + 1
                    if (day in 1..calendar.daysInMonth) {
                        CalendarDayCell(
                            day = day,
                            spend = calendar.spendForDay(day),
                            selected = day == selectedDay,
                            onClick = { onDaySelected(day) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    spend: DailySpendUi?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoneyTheme.colors
    val spendAccent = spend?.let { categoryAccentForName(it.label) } ?: MoneyBlue
    val background = when {
        selected -> spendAccent
        spend != null -> colors.soft(spendAccent)
        else -> colors.canvas
    }
    val dayColor = if (selected) Color.White else colors.ink
    val amountColor = if (selected) Color.White else spendAccent

    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = day.toString(),
            color = dayColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1
        )
        Text(
            text = spend?.amountWon?.let(::formatCompactWon) ?: "",
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun CalendarSelectedSummary(day: Int, spend: DailySpendUi?) {
    val colors = MoneyTheme.colors
    val accent = spend?.let { categoryAccentForName(it.label) } ?: MoneyBlue
    val summary = if (spend == null) {
        "${day}일 사용 기록 없음"
    } else {
        "${day}일 ${spend.label} ${formatWon(spend.amountWon)}"
    }

    Text(
        text = summary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.soft(accent))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        color = colors.ink,
        fontWeight = FontWeight.Medium
    )
}

private fun formatCompactWon(amount: Long): String {
    return if (amount >= 10_000) {
        val text = "%.1f".format(amount / 10_000.0).removeSuffix(".0")
        "${text}만"
    } else {
        "${amount / 1_000}천"
    }
}
