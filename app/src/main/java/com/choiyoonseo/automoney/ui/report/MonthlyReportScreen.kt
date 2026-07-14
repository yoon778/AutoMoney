package com.choiyoonseo.automoney.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.countsAsReportIncome
import com.choiyoonseo.automoney.domain.report.countsAsSavingMovement
import com.choiyoonseo.automoney.domain.report.effectiveExpenseWon
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import com.choiyoonseo.automoney.ui.components.CategoryBar
import com.choiyoonseo.automoney.ui.components.DetailBottomSheet
import com.choiyoonseo.automoney.ui.components.DetailSheetState
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MetricTile
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.MoneyGreen
import com.choiyoonseo.automoney.ui.components.MoneyFlowHeroCard
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.components.SpendingCalendarCard
import com.choiyoonseo.automoney.ui.components.categoryAccentForName
import com.choiyoonseo.automoney.ui.model.MetricTileUi
import com.choiyoonseo.automoney.ui.model.TransactionRowUi
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.model.sampleHomeSnapshot
import com.choiyoonseo.automoney.ui.model.sampleCategorySpends
import com.choiyoonseo.automoney.ui.model.sampleSpendCalendar
import com.choiyoonseo.automoney.ui.model.transactionsToRows
import com.choiyoonseo.automoney.ui.model.transactionsToSpendCalendar
import com.choiyoonseo.automoney.ui.model.transactionsToMonthlySummary
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import kotlinx.coroutines.flow.flowOf
import java.time.YearMonth

@Composable
fun MonthlyReportScreen(
    padding: PaddingValues,
    moneyRepository: MoneyRepository? = null
) {
    var month by remember { mutableStateOf(YearMonth.now(AppDateZoneId)) }
    val transactions by remember(moneyRepository, month) {
        moneyRepository?.observeTransactionsForMonth(month) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val reviewCount by remember(moneyRepository) {
        moneyRepository?.observeOpenReviewCount() ?: flowOf(sampleHomeSnapshot.reviewCount)
    }.collectAsState(initial = sampleHomeSnapshot.reviewCount)
    val summary = if (moneyRepository == null) {
        null
    } else {
        transactionsToMonthlySummary(month, transactions, reviewCount)
    }
    val calendar = if (moneyRepository == null) {
        sampleSpendCalendar
    } else {
        transactionsToSpendCalendar(month, transactions)
    }
    val incomeRows = if (moneyRepository == null) {
        emptyList()
    } else {
        transactionsToRows(transactions.filter { it.countsAsReportIncome() }, limit = 30)
    }
    val expenseRows = if (moneyRepository == null) {
        emptyList()
    } else {
        transactionsToRows(transactions.filter { it.countsAsActualExpense() }, limit = 30)
    }
    val savingRows = if (moneyRepository == null) {
        emptyList()
    } else {
        transactionsToRows(transactions.filter { it.countsAsSavingMovement() }, limit = 30)
    }
    var activeDetail by remember { mutableStateOf<DetailSheetState?>(null) }
    var selectedReportDay by remember(month) { mutableStateOf<Int?>(null) }
    val dayExpenseRows = remember(transactions, selectedReportDay) {
        val day = selectedReportDay
        if (day == null) {
            emptyList()
        } else {
            transactionsToRows(
                transactions
                    .filter { it.countsAsActualExpense() && it.occurredAt.atZone(AppDateZoneId).dayOfMonth == day }
                    .sortedByDescending { it.effectiveExpenseWon() },
                limit = 50
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MoneyTheme.colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "월간 보고서",
            subtitle = "충전은 제외하고 실제 사용액 중심으로 봐요"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { month = month.minusMonths(1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("이전 달")
            }
            OutlinedButton(
                onClick = { month = month.plusMonths(1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("다음 달")
            }
        }

        MoneyFlowHeroCard(
            title = "${month.monthValue}월 남은 돈",
            primaryValue = formatWon(summary?.netWon ?: 342000),
            helper = "수입 - 지출 - 저축 기준",
            reviewCount = reviewCount,
            spentLabel = formatWon(summary?.expenseWon ?: 898000),
            savedLabel = formatWon(summary?.savingWon ?: 0),
            onClick = {
                activeDetail = DetailSheetState(
                    title = "${month.monthValue}월 요약",
                    headlineValue = formatWon(summary?.netWon ?: 0),
                    caption = "남은 돈",
                    summaryLines = listOf(
                        "수입 ${formatWon(summary?.incomeWon ?: 0)}",
                        "지출 ${formatWon(summary?.expenseWon ?: 0)}",
                        "저축 ${formatWon(summary?.savingWon ?: 0)}"
                    ),
                    rows = incomeRows + expenseRows + savingRows
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                MetricTileUi("수입", formatWon(summary?.incomeWon ?: 1240000), 1f),
                MoneyGreen,
                Modifier.weight(1f),
                onClick = {
                    activeDetail = DetailSheetState(
                        title = "${month.monthValue}월 수입",
                        headlineValue = formatWon(summary?.incomeWon ?: 0),
                        rows = incomeRows
                    )
                }
            )
            MetricTile(
                MetricTileUi(
                    "지출",
                    formatWon(summary?.expenseWon ?: 898000),
                    if ((summary?.incomeWon ?: 1240000) > 0) {
                        (summary?.expenseWon ?: 898000) / (summary?.incomeWon ?: 1240000).toFloat()
                    } else {
                        0f
                    }
                ),
                MoneyCoral,
                Modifier.weight(1f),
                onClick = {
                    activeDetail = DetailSheetState(
                        title = "${month.monthValue}월 지출",
                        headlineValue = formatWon(summary?.expenseWon ?: 0),
                        rows = expenseRows
                    )
                }
            )
        }

        SpendingCalendarCard(
            title = "날짜별 사용",
            calendar = calendar,
            onDaySelected = { selectedReportDay = it }
        )

        selectedReportDay?.let { day ->
            FinanceSectionCard(
                title = "${month.monthValue}월 ${day}일 지출",
                subtitle = "이 날 사용한 내역",
                accent = MoneyCoral,
                icon = Icons.Filled.BarChart
            ) {
                if (dayExpenseRows.isEmpty()) {
                    Text("이 날은 지출 기록이 없어요")
                } else {
                    dayExpenseRows.forEach { row -> DayExpenseRow(row) }
                }
            }
        }

        FinanceSectionCard(
            title = "카테고리별 지출",
            subtitle = "많이 쓴 순서로 확인",
            accent = MoneyBlue,
            icon = Icons.Filled.BarChart
        ) {
            val categories = summary?.categorySpends ?: sampleCategorySpends
            categories.forEach { category ->
                CategoryBar(
                    category = category,
                    color = categoryAccentForName(category.name),
                    onClick = {
                        activeDetail = DetailSheetState(
                            title = category.name,
                            headlineValue = formatWon(category.amountWon),
                            rows = expenseRows.filter { it.category == category.name }
                        )
                    }
                )
            }
            if (categories.isEmpty()) {
                Text("아직 이번 달 지출 기록이 없어요")
            }
        }
    }

    activeDetail?.let { detail ->
        DetailBottomSheet(
            state = detail,
            onDismiss = { activeDetail = null }
        )
    }
}

@Composable
private fun DayExpenseRow(row: TransactionRowUi) {
    val colors = MoneyTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.merchant, fontWeight = FontWeight.Medium, color = colors.ink)
            Text(row.category, style = MaterialTheme.typography.labelSmall, color = colors.muted)
        }
        Text(
            formatWon(row.amountWon),
            fontWeight = FontWeight.Bold,
            color = colors.ink
        )
    }
}

