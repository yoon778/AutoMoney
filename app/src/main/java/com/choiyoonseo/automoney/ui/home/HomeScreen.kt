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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.data.repository.AssetRepository
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.assets.buildCategoryBudgetUsages
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.countsAsReportIncome
import com.choiyoonseo.automoney.domain.report.PlannedUseContribution
import com.choiyoonseo.automoney.domain.report.plannedUseContributions
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
import com.choiyoonseo.automoney.ui.components.TransactionEditDialog
import com.choiyoonseo.automoney.ui.components.rememberMergedCategoryLabels
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.ui.components.TransactionRow
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.model.sampleHomeSnapshot
import com.choiyoonseo.automoney.ui.model.transactionsToRows
import com.choiyoonseo.automoney.ui.model.transactionsToMonthlySummary
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    padding: PaddingValues,
    moneyRepository: MoneyRepository? = null,
    onReviewClick: () -> Unit = {},
    editTransactionUseCase: EditTransactionUseCase? = null,
    assetRepository: AssetRepository? = null,
    notificationAccessEnabled: Boolean? = null,
    showNotificationOnboarding: Boolean = false,
    onDismissNotificationOnboarding: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    userCategoryRepository: UserCategoryRepository? = null
) {
    val colors = MoneyTheme.colors
    val month = remember { YearMonth.now(AppDateZoneId) }
    val transactions by remember(moneyRepository, month) {
        moneyRepository?.observeTransactionsForMonth(month) ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val reviewCount by remember(moneyRepository) {
        moneyRepository?.observeOpenReviewCount() ?: flowOf(sampleHomeSnapshot.reviewCount)
    }.collectAsStateWithLifecycle(initialValue = sampleHomeSnapshot.reviewCount)
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
    val scope = rememberCoroutineScope()
    val monthlyPlans by remember(assetRepository, month) {
        assetRepository?.observeMonthlyPlanItems(month) ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val budgetUsages = remember(monthlyPlans, transactions) {
        buildCategoryBudgetUsages(monthlyPlans, transactions)
    }
    val fixedExpenses by remember(assetRepository, month) {
        assetRepository?.observeFixedExpenses(month) ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val (expenseCategoryLabels, incomeCategoryLabels) = rememberMergedCategoryLabels(userCategoryRepository)
    var activeEditTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var isEditingTransaction by remember { mutableStateOf(false) }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }

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
                    headlineValue = formatWon(dashboard.netSavedWon),
                    caption = "남은 돈",
                    summaryLines = listOf(
                        "수입 ${formatWon(summary?.incomeWon ?: 2_850_000)}",
                        "지출 ${formatWon(summary?.expenseWon ?: 2_318_000)}",
                        "저축 ${formatWon(summary?.savingWon ?: 532_000)}"
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
                        headlineValue = formatWon(todayExpenseWon),
                        caption = "${todayExpenseRows.size}건",
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
                        headlineValue = formatWon(weekExpenseWon),
                        caption = "${weekExpenseRows.size}건",
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
                TransactionRow(
                    transaction = transaction,
                    balanceImpact = transaction.id?.let { rowId ->
                        transactions.firstOrNull { it.id == rowId }?.balanceImpact
                    }
                )
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
        HomeDetailSheet(
            detail = detail,
            onDismiss = { activeDetail = null },
            balanceImpactFor = { rowId -> transactions.firstOrNull { it.id == rowId }?.balanceImpact },
            onRowClick = if (editTransactionUseCase == null) {
                null
            } else {
                { row ->
                    row.id?.let { rowId ->
                        transactions.firstOrNull { it.id == rowId }?.let {
                            activeEditTransaction = it
                            editErrorMessage = null
                        }
                    }
                }
            }
        )
    }

    activeEditTransaction?.let { transaction ->
        val useCase = editTransactionUseCase
        if (useCase != null) {
            TransactionEditDialog(
                transaction = transaction,
                isSaving = isEditingTransaction,
                errorMessage = editErrorMessage,
                budgetUsages = budgetUsages,
                fixedExpenses = fixedExpenses,
                expenseCategoryLabels = expenseCategoryLabels,
                incomeCategoryLabels = incomeCategoryLabels,
                onDismiss = {
                    activeEditTransaction = null
                    editErrorMessage = null
                },
                onSave = { amountWon, category, memo, occurredAt, budgetPlanId, fixedExpensePlanId, transactionType ->
                    scope.launch {
                        isEditingTransaction = true
                        editErrorMessage = null
                        try {
                            useCase.update(
                                transaction,
                                amountWon,
                                category,
                                memo,
                                occurredAt,
                                transactionType = transactionType,
                                budgetPlanId = budgetPlanId,
                                fixedExpensePlanId = fixedExpensePlanId
                            )
                            activeEditTransaction = null
                            activeDetail = null
                        } catch (e: IllegalArgumentException) {
                            editErrorMessage = e.message ?: "입력값을 확인해 주세요."
                        } catch (e: RuntimeException) {
                            editErrorMessage = "수정 중 문제가 생겼어요."
                        } finally {
                            isEditingTransaction = false
                        }
                    }
                },
                onExclude = {
                    scope.launch {
                        isEditingTransaction = true
                        editErrorMessage = null
                        try {
                            useCase.exclude(transaction)
                            activeEditTransaction = null
                            activeDetail = null
                        } catch (e: RuntimeException) {
                            editErrorMessage = "제외 처리 중 문제가 생겼어요."
                        } finally {
                            isEditingTransaction = false
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        isEditingTransaction = true
                        editErrorMessage = null
                        try {
                            useCase.delete(transaction)
                            activeEditTransaction = null
                            activeDetail = null
                        } catch (e: RuntimeException) {
                            editErrorMessage = "삭제 중 문제가 생겼어요."
                        } finally {
                            isEditingTransaction = false
                        }
                    }
                }
            )
        }
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

// 연결된 환급을 뺀 순사용액으로 합산한다. 환급이 다른 날에 들어와도 원결제 쪽에서 차감된다.
private fun List<MoneyTransaction>.expenseWonOn(date: LocalDate): Long =
    plannedUseContributions(this)
        .filter { it.transaction.countsAsActualExpense() && it.transaction.localDate() == date }
        .sumOf(PlannedUseContribution::amountWon)

private fun List<MoneyTransaction>.expenseTransactionsOn(date: LocalDate): List<MoneyTransaction> =
    filter { transaction ->
        transaction.countsAsActualExpense() && transaction.localDate() == date
    }

private fun List<MoneyTransaction>.expenseWonSince(start: LocalDate, end: LocalDate): Long =
    plannedUseContributions(this)
        .filter {
            val date = it.transaction.localDate()
            it.transaction.countsAsActualExpense() &&
                !date.isBefore(start) && !date.isAfter(end)
        }
        .sumOf(PlannedUseContribution::amountWon)

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
    val headlineValue: String,
    val caption: String? = null,
    val summaryLines: List<String> = emptyList(),
    val rows: List<TransactionRowUi>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDetailSheet(
    detail: HomeDetailDialogState,
    onDismiss: () -> Unit,
    onRowClick: ((TransactionRowUi) -> Unit)? = null,
    balanceImpactFor: ((Long) -> BalanceImpact?)? = null
) {
    val colors = MoneyTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(
                detail.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    detail.headlineValue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
                detail.caption?.let { caption ->
                    Text(
                        caption,
                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.muted
                    )
                }
            }
            if (detail.summaryLines.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detail.summaryLines.forEach { line ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.soft(colors.primary)
                        ) {
                            Text(
                                line,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                if (detail.rows.isEmpty()) {
                    Text(
                        "표시할 거래가 없어요",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = colors.muted
                    )
                } else {
                    detail.rows.take(20).forEachIndexed { index, row ->
                        if (index > 0) {
                            HorizontalDivider(color = colors.divider)
                        }
                        TransactionRow(
                            transaction = row,
                            balanceImpact = row.id?.let { rowId -> balanceImpactFor?.invoke(rowId) },
                            onClick = if (onRowClick != null && row.id != null) {
                                { onRowClick(row) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
            if (onRowClick != null && detail.rows.any { it.id != null }) {
                Text(
                    "항목을 누르면 바로 수정할 수 있어요",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
