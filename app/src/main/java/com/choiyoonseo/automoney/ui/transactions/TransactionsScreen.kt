package com.choiyoonseo.automoney.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.data.repository.AssetRepository
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.assets.buildCategoryBudgetUsages
import com.choiyoonseo.automoney.domain.manual.SaveManualTransactionUseCase
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.ui.components.AutoClearMessageEffect
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyDialog
import com.choiyoonseo.automoney.ui.components.MonthPagerInitialPage
import com.choiyoonseo.automoney.ui.components.MonthPagerPageCount
import com.choiyoonseo.automoney.ui.components.MonthPagerHeader
import com.choiyoonseo.automoney.ui.components.TransactionEditDialog
import com.choiyoonseo.automoney.ui.components.rememberMergedCategoryLabels
import com.choiyoonseo.automoney.ui.components.TransactionRow
import com.choiyoonseo.automoney.ui.components.monthForPagerPage
import com.choiyoonseo.automoney.ui.components.pagerPageForMonth
import com.choiyoonseo.automoney.ui.model.TransactionDateSectionUi
import com.choiyoonseo.automoney.ui.model.sampleHomeSnapshot
import com.choiyoonseo.automoney.ui.model.transactionsToDateSections
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

internal fun transactionSectionsForMonth(
    sections: List<TransactionDateSectionUi>,
    month: YearMonth
): List<TransactionDateSectionUi> = sections
    .filter { YearMonth.from(it.date) == month }
    .sortedBy { it.date }

private fun transactionDateLabel(date: LocalDate, today: LocalDate): String {
    val weekday = listOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")[
        date.dayOfWeek.value - 1
    ]
    val todayPrefix = if (date == today) "오늘 · " else ""
    return "$todayPrefix${date.monthValue}월 ${date.dayOfMonth}일 $weekday"
}

@Composable
fun TransactionsScreen(
    padding: PaddingValues,
    moneyRepository: MoneyRepository? = null,
    saveManualTransactionUseCase: SaveManualTransactionUseCase? = null,
    editTransactionUseCase: EditTransactionUseCase? = null,
    assetRepository: AssetRepository? = null,
    walletTopupNoticeStore: WalletTopupNoticeStore? = null,
    notificationAccessEnabled: Boolean? = null,
    userCategoryRepository: UserCategoryRepository? = null,
    onOpenNotificationSettings: () -> Unit = {}
) {
    val colors = MoneyTheme.colors
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now(AppDateZoneId) }
    val currentMonth = remember(today) { YearMonth.from(today) }
    val pagerState = rememberPagerState(
        initialPage = MonthPagerInitialPage,
        pageCount = { MonthPagerPageCount }
    )
    val selectedMonth = monthForPagerPage(pagerState.currentPage, currentMonth)
    var budgetMonth by remember { mutableStateOf(currentMonth) }
    val transactions by remember(moneyRepository) {
        moneyRepository?.observeAllTransactions() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val budgetMonthTransactions by remember(moneyRepository, budgetMonth) {
        moneyRepository?.observeTransactionsForMonth(budgetMonth) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val monthlyPlans by remember(assetRepository, budgetMonth) {
        assetRepository?.observeMonthlyPlanItems(budgetMonth) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val budgetUsages = remember(monthlyPlans, budgetMonthTransactions) {
        buildCategoryBudgetUsages(monthlyPlans, budgetMonthTransactions)
    }
    val fixedExpenses by remember(assetRepository, budgetMonth) {
        assetRepository?.observeFixedExpenses(budgetMonth) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val (expenseCategoryLabels, incomeCategoryLabels) = rememberMergedCategoryLabels(userCategoryRepository)
    val dateSections = if (moneyRepository == null) {
        listOf(
            TransactionDateSectionUi(
                date = today,
                dateLabel = "오늘",
                rows = sampleHomeSnapshot.recentTransactions
            )
        )
    } else {
        transactionsToDateSections(transactions)
    }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var manualFormMessage by remember { mutableStateOf<String?>(null) }
    var manualFormResetSignal by remember { mutableStateOf(0) }
    var isManualFormVisible by remember { mutableStateOf(false) }
    var isSavingManual by remember { mutableStateOf(false) }
    var activeEditTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }
    var isEditingTransaction by remember { mutableStateOf(false) }
    var showWalletTopupNotice by remember(walletTopupNoticeStore) {
        mutableStateOf(walletTopupNoticeStore?.shouldShowNotice() == true)
    }
    AutoClearMessageEffect(saveSuccessMessage) {
        saveSuccessMessage = null
    }

    fun dismissWalletTopupNotice() {
        walletTopupNoticeStore?.markNoticeSeen()
        showWalletTopupNotice = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(colors.canvas)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "거래",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
                Text(
                    text = "날짜순으로 정리된 거래를 확인하고 직접 추가해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            }
            FilledIconButton(
                onClick = {
                    budgetMonth = currentMonth
                    isManualFormVisible = true
                    manualFormMessage = null
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = "거래 추가")
            }
        }
        saveSuccessMessage?.let { message ->
            AssistChip(onClick = {}, label = { Text(message) })
        }

        MonthPagerHeader(
            month = selectedMonth,
            onPreviousMonth = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onNextMonth = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            key = { page -> monthForPagerPage(page, currentMonth).toString() }
        ) { page ->
            val pageMonth = monthForPagerPage(page, currentMonth)
            val sections = transactionSectionsForMonth(dateSections, pageMonth)
            val rowCount = sections.sumOf { it.rows.size }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp)
            ) {
                FinanceSectionCard(
                    title = "거래 ${rowCount}건",
                    subtitle = "날짜가 빠른 순서예요",
                    accent = MoneyBlue,
                    icon = Icons.AutoMirrored.Filled.List
                ) {
                    sections.forEach { section ->
                        Text(
                            text = transactionDateLabel(section.date, today),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.muted
                        )
                        section.rows.forEach { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                balanceImpact = transaction.id?.let { rowId ->
                                    transactions.firstOrNull { it.id == rowId }?.balanceImpact
                                },
                                onClick = transaction.id?.let { transactionId ->
                                    if (editTransactionUseCase == null) {
                                        null
                                    } else {
                                        {
                                            activeEditTransaction = transactions.firstOrNull { it.id == transactionId }
                                                ?.also { budgetMonth = it.monthKey }
                                            editErrorMessage = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                    if (sections.isEmpty()) {
                        if (pageMonth == currentMonth && notificationAccessEnabled == false) {
                            Text("\uc54c\ub9bc \uad8c\ud55c\uc744 \ucf1c\uba74 \uac70\ub798\uac00 \uc790\ub3d9\uc73c\ub85c \ub4e4\uc5b4\uc640\uc694", color = colors.muted)
                            TextButton(onClick = onOpenNotificationSettings) {
                                Text("\uad8c\ud55c \uc124\uc815 \uc5f4\uae30")
                            }
                        } else {
                            Text("이 달은 거래 기록이 없어요.", color = colors.muted)
                        }
                    }
                }
            }
        }
    }

    if (isManualFormVisible) {
        MoneyDialog(
            title = "거래 추가",
            subtitle = "자동 알림에 없는 거래만 직접 추가",
            onDismiss = {
                if (!isSavingManual) {
                    isManualFormVisible = false
                    manualFormMessage = null
                }
            },
            buttons = {
                OutlinedButton(
                    enabled = !isSavingManual,
                    onClick = {
                        isManualFormVisible = false
                        manualFormMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("닫기")
                }
            }
        ) {
            manualFormMessage?.let { Text(it, fontWeight = FontWeight.Medium, color = colors.negative) }
            ManualTransactionForm(
                isSaving = isSavingManual,
                resetSignal = manualFormResetSignal,
                budgetUsages = budgetUsages,
                fixedExpenses = fixedExpenses,
                expenseCategoryLabels = expenseCategoryLabels,
                incomeCategoryLabels = incomeCategoryLabels,
                onDateChanged = { budgetMonth = YearMonth.from(it) }
            ) { type, amountWon, category, memo, occurredAt, budgetPlanId, fixedExpensePlanId ->
                val useCase = saveManualTransactionUseCase
                if (useCase == null) {
                    saveSuccessMessage = null
                    manualFormMessage = "미리보기에서는 저장하지 않아요."
                    return@ManualTransactionForm
                }

                scope.launch {
                    isSavingManual = true
                    try {
                        useCase.save(
                            type = type,
                            amountWon = amountWon,
                            categoryText = category,
                            memo = memo,
                            occurredAt = occurredAt,
                            budgetPlanId = budgetPlanId,
                            fixedExpensePlanId = fixedExpensePlanId
                        )
                        val savedMonth = YearMonth.from(occurredAt.atZone(manualTransactionZoneId))
                        saveSuccessMessage = "수동 거래를 저장했어요. 선택한 달에 반영했어요."
                        manualFormMessage = null
                        isManualFormVisible = false
                        manualFormResetSignal += 1
                        pagerState.animateScrollToPage(pagerPageForMonth(savedMonth, currentMonth))
                    } catch (e: IllegalArgumentException) {
                        saveSuccessMessage = null
                        manualFormMessage = e.message ?: "입력값을 확인해 주세요."
                    } catch (e: RuntimeException) {
                        saveSuccessMessage = null
                        manualFormMessage = "저장 중 문제가 생겼어요."
                    } finally {
                        isSavingManual = false
                    }
                }
            }
        }
    }

    activeEditTransaction?.let { transaction ->
        TransactionEditDialog(
            transaction = transaction,
            isSaving = isEditingTransaction,
            errorMessage = editErrorMessage,
            budgetUsages = budgetUsages,
            fixedExpenses = fixedExpenses,
            expenseCategoryLabels = expenseCategoryLabels,
            incomeCategoryLabels = incomeCategoryLabels,
            onDateChanged = { budgetMonth = YearMonth.from(it) },
            onDismiss = {
                activeEditTransaction = null
                editErrorMessage = null
            },
            onSave = { amountWon, category, memo, occurredAt, budgetPlanId, fixedExpensePlanId, transactionType ->
                val useCase = editTransactionUseCase ?: return@TransactionEditDialog
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
                        saveSuccessMessage = "거래를 수정했어요. 최근 거래에 반영했어요."
                        activeEditTransaction = null
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
                val useCase = editTransactionUseCase ?: return@TransactionEditDialog
                scope.launch {
                    isEditingTransaction = true
                    editErrorMessage = null
                    try {
                        useCase.exclude(transaction)
                        saveSuccessMessage = "거래를 지출에서 제외했어요."
                        activeEditTransaction = null
                    } catch (e: RuntimeException) {
                        editErrorMessage = "제외 처리 중 문제가 생겼어요."
                    } finally {
                        isEditingTransaction = false
                    }
                }
            },
            onDelete = {
                val useCase = editTransactionUseCase ?: return@TransactionEditDialog
                scope.launch {
                    isEditingTransaction = true
                    editErrorMessage = null
                    try {
                        useCase.delete(transaction)
                        saveSuccessMessage = "거래를 삭제했어요."
                        activeEditTransaction = null
                    } catch (e: RuntimeException) {
                        editErrorMessage = "삭제 중 문제가 생겼어요."
                    } finally {
                        isEditingTransaction = false
                    }
                }
            }
        )
    }

    if (showWalletTopupNotice) {
        AlertDialog(
            onDismissRequest = { dismissWalletTopupNotice() },
            title = { Text("충전/포인트 안내") },
            text = { Text("충전과 포인트 이동은 거래 목록에서 제외하고, 실제 사용 금액만 지출로 기록해요.") },
            confirmButton = {
                TextButton(onClick = { dismissWalletTopupNotice() }) {
                    Text("확인")
                }
            }
        )
    }
}
