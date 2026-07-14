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
import com.choiyoonseo.automoney.domain.assets.buildCategoryBudgetUsages
import com.choiyoonseo.automoney.domain.manual.SaveManualTransactionUseCase
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.ui.components.AutoClearMessageEffect
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyDialog
import com.choiyoonseo.automoney.ui.components.TransactionEditDialog
import com.choiyoonseo.automoney.ui.components.TransactionRow
import com.choiyoonseo.automoney.ui.model.TransactionDateSectionUi
import com.choiyoonseo.automoney.ui.model.sampleHomeSnapshot
import com.choiyoonseo.automoney.ui.model.transactionsToDateSections
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TransactionsScreen(
    padding: PaddingValues,
    moneyRepository: MoneyRepository? = null,
    saveManualTransactionUseCase: SaveManualTransactionUseCase? = null,
    editTransactionUseCase: EditTransactionUseCase? = null,
    assetRepository: AssetRepository? = null,
    walletTopupNoticeStore: WalletTopupNoticeStore? = null,
    notificationAccessEnabled: Boolean? = null,
    onOpenNotificationSettings: () -> Unit = {}
) {
    val colors = MoneyTheme.colors
    val scope = rememberCoroutineScope()
    val month = remember { YearMonth.now(AppDateZoneId) }
    val scrollState = rememberScrollState()
    val transactions by remember(moneyRepository, month) {
        moneyRepository?.observeTransactionsForMonth(month) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val monthlyPlans by remember(assetRepository) {
        assetRepository?.observeMonthlyPlanItems() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val budgetUsages = remember(monthlyPlans, transactions) {
        buildCategoryBudgetUsages(monthlyPlans, transactions)
    }
    val fixedExpenses by remember(assetRepository) {
        assetRepository?.observeFixedExpenses() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val dateSections = if (moneyRepository == null) {
        listOf(
            TransactionDateSectionUi(
                date = LocalDate.now(AppDateZoneId),
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
            .verticalScroll(scrollState)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
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

        FinanceSectionCard(
            title = "최근 거래",
            subtitle = "검토 완료된 기록만 보여줘요",
            accent = MoneyBlue,
            icon = Icons.AutoMirrored.Filled.List
        ) {
            dateSections.forEach { section ->
                Text(
                    text = section.dateLabel,
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
                                    editErrorMessage = null
                                }
                            }
                        }
                    )
                }
            }
            if (dateSections.all { it.rows.isEmpty() }) {
                if (notificationAccessEnabled == false) {
                    Text("\uc54c\ub9bc \uad8c\ud55c\uc744 \ucf1c\uba74 \uac70\ub798\uac00 \uc790\ub3d9\uc73c\ub85c \ub4e4\uc5b4\uc640\uc694", color = colors.muted)
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text("\uad8c\ud55c \uc124\uc815 \uc5f4\uae30")
                    }
                } else {
                    Text("아직 이번 달 기록이 없어요.", color = colors.muted)
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
                fixedExpenses = fixedExpenses
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
                        saveSuccessMessage = if (savedMonth == month) {
                            "수동 거래를 저장했어요. 최근 거래에 반영했어요."
                        } else {
                            "수동 거래를 저장했어요. 선택한 달 기록에 반영했어요."
                        }
                        manualFormMessage = null
                        isManualFormVisible = false
                        manualFormResetSignal += 1
                        scrollState.animateScrollTo(0)
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
            onDismiss = {
                activeEditTransaction = null
                editErrorMessage = null
            },
            onSave = { amountWon, category, memo, occurredAt, budgetPlanId, transactionType ->
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
                            budgetPlanId = budgetPlanId
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
