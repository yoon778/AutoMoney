package com.choiyoonseo.automoney.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.countsAsReportIncome
import com.choiyoonseo.automoney.domain.report.countsAsSavingMovement
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MetricTile
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.MoneyGreen
import com.choiyoonseo.automoney.ui.components.MonthlyFlowCard
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.model.MetricTileUi
import com.choiyoonseo.automoney.ui.model.TransactionRowUi
import com.choiyoonseo.automoney.ui.components.TransactionRow
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.model.sampleHomeSnapshot
import com.choiyoonseo.automoney.ui.model.transactionsToRows
import com.choiyoonseo.automoney.ui.model.transactionsToMonthlySummary
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    padding: PaddingValues,
    moneyRepository: MoneyRepository? = null,
    onReviewClick: () -> Unit = {},
    notificationAccessEnabled: Boolean? = null,
    showNotificationOnboarding: Boolean = false,
    onDismissNotificationOnboarding: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {}
) {
    val colors = MoneyTheme.colors
    val month = remember { YearMonth.now(AppDateZoneId) }
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
    val dashboard = summary?.homeSnapshot ?: sampleHomeSnapshot
    val today = remember { LocalDate.now(AppDateZoneId) }
    val todayExpenseWon = if (moneyRepository == null) 10900 else transactions.expenseWonOn(today)
    val weekExpenseWon = if (moneyRepository == null) 73400 else transactions.expenseWonSince(today.minusDays(6), today)
    val todayExpenseRows = if (moneyRepository == null) {
        sampleHomeSnapshot.recentTransactions
    } else {
        transactionsToRows(transactions.expenseTransactionsOn(today), limit = 20)
    }
    val weekExpenseRows = if (moneyRepository == null) {
        sampleHomeSnapshot.recentTransactions
    } else {
        transactionsToRows(transactions.expenseTransactionsSince(today.minusDays(6), today), limit = 20)
    }
    val monthRows = if (moneyRepository == null) {
        sampleHomeSnapshot.recentTransactions
    } else {
        transactionsToRows(
            transactions.filter {
                it.countsAsReportIncome() ||
                    it.countsAsActualExpense() ||
                    it.countsAsSavingMovement()
            },
            limit = 30
        )
    }
    var activeDetail by remember { mutableStateOf<HomeDetailDialogState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "홈",
            subtitle = "자동 기록된 돈 흐름을 한눈에 봐요"
        )

        MonthlyFlowCard(
            title = "이번 달 돈 흐름",
            period = "${month.monthValue}월 1일 - ${month.monthValue}월 ${month.lengthOfMonth()}일",
            remainingValue = formatWon(dashboard.netSavedWon),
            incomeValue = summary?.incomeWon?.let(::formatWon) ?: formatWon(2_850_000),
            expenseValue = summary?.expenseWon?.let(::formatWon) ?: formatWon(2_318_000),
            savingsValue = summary?.savingWon?.let(::formatWon) ?: formatWon(532_000),
            onClick = {
                activeDetail = HomeDetailDialogState(
                    title = "이번 달 돈 흐름",
                    summaryLines = listOf(
                        "수입 ${formatWon(summary?.incomeWon ?: 2_850_000)}",
                        "지출 ${formatWon(summary?.expenseWon ?: 2_318_000)}",
                        "저축 ${formatWon(summary?.savingWon ?: 532_000)}",
                        "남은 돈 ${formatWon(dashboard.netSavedWon)}"
                    ),
                    rows = monthRows
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                MetricTileUi("오늘 사용", formatWon(todayExpenseWon), progress = 0.28f, helper = "실제 지출"),
                MoneyCoral,
                Modifier.weight(1f),
                onClick = {
                    activeDetail = HomeDetailDialogState(
                        title = "오늘 사용",
                        summaryLines = listOf("합계 ${formatWon(todayExpenseWon)}"),
                        rows = todayExpenseRows
                    )
                }
            )
            MetricTile(
                MetricTileUi("최근 7일", formatWon(weekExpenseWon), progress = 0.46f, helper = "자동 기록"),
                MoneyBlue,
                Modifier.weight(1f),
                onClick = {
                    activeDetail = HomeDetailDialogState(
                        title = "최근 7일 사용",
                        summaryLines = listOf("합계 ${formatWon(weekExpenseWon)}"),
                        rows = weekExpenseRows
                    )
                }
            )
        }

        FinanceSectionCard(
            title = "오늘 확인할 일",
            subtitle = "애매한 돈 이동만 따로 모았어요",
            accent = MoneyCoral,
            icon = Icons.Filled.CheckCircle,
            onClick = onReviewClick
        ) {
            Text("검토 필요 ${dashboard.reviewCount}건", fontWeight = FontWeight.Bold)
            Text("충전, 송금, 입금은 메모를 남긴 뒤 정리해요")
        }

        FinanceSectionCard(
            title = "최근 자동 기록",
            subtitle = "확정된 거래만 보여줘요",
            accent = MoneyGreen,
            icon = Icons.AutoMirrored.Filled.List
        ) {
            dashboard.recentTransactions.forEach { transaction ->
                TransactionRow(transaction)
            }
            if (dashboard.recentTransactions.isEmpty()) {
                if (notificationAccessEnabled == false) {
                    Text("\uc54c\ub9bc \uad8c\ud55c\uc744 \ucf1c\uba74 \uacb0\uc81c/\uc785\uae08 \uc54c\ub9bc\uc744 \uc790\ub3d9 \uae30\ub85d\ud560 \uc218 \uc788\uc5b4\uc694")
                    Button(onClick = onOpenNotificationSettings) {
                        Text("\uad8c\ud55c \uc124\uc815 \uc5f4\uae30")
                    }
                } else {
                    Text("\uc544\uc9c1 \uc774\ubc88 \ub2ec \uae30\ub85d\uc774 \uc5c6\uc5b4\uc694")
                }
            }
        }
    }

    activeDetail?.let { detail ->
        HomeDetailDialog(
            detail = detail,
            onDismiss = { activeDetail = null }
        )
    }

    if (showNotificationOnboarding) {
        AlertDialog(
            onDismissRequest = onDismissNotificationOnboarding,
            title = { Text("\uc54c\ub9bc\uc73c\ub85c \uc790\ub3d9 \uae30\ub85d") },
            text = { Text("\uacb0\uc81c\u00b7\uc785\uae08 \uc54c\ub9bc\uc744 \uc77d\uc5b4\uc11c \uac70\ub798\ub97c \uc790\ub3d9\uc73c\ub85c \uae30\ub85d\ud574\uc694. \uad8c\ud55c\uc744 \ucf1c\uba74 \uc190\ub300\uc9c0 \uc54a\uc544\ub3c4 \uac00\uacc4\ubd80\uac00 \ucc44\uc6cc\uc838\uc694.") },
            confirmButton = {
                Button(onClick = onOpenNotificationSettings) {
                    Text("\uad8c\ud55c \uc124\uc815 \uc5f4\uae30")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissNotificationOnboarding) {
                    Text("\ub098\uc911\uc5d0")
                }
            }
        )
    }
}

private fun List<MoneyTransaction>.expenseWonOn(date: LocalDate): Long =
    filter { transaction ->
        transaction.countsAsActualExpense() && transaction.localDate() == date
    }.sumOf { it.amount.won }

private fun List<MoneyTransaction>.expenseTransactionsOn(date: LocalDate): List<MoneyTransaction> =
    filter { transaction ->
        transaction.countsAsActualExpense() && transaction.localDate() == date
    }

private fun List<MoneyTransaction>.expenseWonSince(start: LocalDate, end: LocalDate): Long =
    filter { transaction ->
        val date = transaction.localDate()
        transaction.countsAsActualExpense() &&
            !date.isBefore(start) && !date.isAfter(end)
    }.sumOf { it.amount.won }

private fun List<MoneyTransaction>.expenseTransactionsSince(start: LocalDate, end: LocalDate): List<MoneyTransaction> =
    filter { transaction ->
        val date = transaction.localDate()
        transaction.countsAsActualExpense() &&
            !date.isBefore(start) && !date.isAfter(end)
    }

private fun MoneyTransaction.localDate(): LocalDate =
    occurredAt.atZone(AppDateZoneId).toLocalDate()

private data class HomeDetailDialogState(
    val title: String,
    val summaryLines: List<String>,
    val rows: List<TransactionRowUi>
)

@Composable
private fun HomeDetailDialog(
    detail: HomeDetailDialogState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(detail.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.summaryLines.forEach { line ->
                    Text(line, fontWeight = FontWeight.Medium)
                }
                if (detail.rows.isEmpty()) {
                    Text("표시할 거래가 없어요")
                } else {
                    detail.rows.take(10).forEach { row ->
                        Text("${row.merchant} · ${row.category} · ${formatWon(row.amountWon)}")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}
