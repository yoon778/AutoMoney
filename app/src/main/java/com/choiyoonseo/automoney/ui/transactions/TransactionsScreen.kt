package com.choiyoonseo.automoney.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.data.repository.AssetRepository
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.manual.SaveManualTransactionUseCase
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyCanvas
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.components.TransactionEditDialog
import com.choiyoonseo.automoney.ui.components.TransactionRow
import com.choiyoonseo.automoney.ui.model.sampleHomeSnapshot
import com.choiyoonseo.automoney.ui.model.transactionsToRows
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.YearMonth

@Composable
fun TransactionsScreen(
    padding: PaddingValues,
    moneyRepository: MoneyRepository? = null,
    saveManualTransactionUseCase: SaveManualTransactionUseCase? = null,
    editTransactionUseCase: EditTransactionUseCase? = null,
    assetRepository: AssetRepository? = null
) {
    val scope = rememberCoroutineScope()
    val month = remember { YearMonth.now() }
    val scrollState = rememberScrollState()
    val transactions by remember(moneyRepository, month) {
        moneyRepository?.observeTransactionsForMonth(month) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val assetAccounts by remember(assetRepository) {
        assetRepository?.observeAccounts() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val rows = if (moneyRepository == null) {
        sampleHomeSnapshot.recentTransactions
    } else {
        transactionsToRows(transactions)
    }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var manualFormMessage by remember { mutableStateOf<String?>(null) }
    var manualFormResetSignal by remember { mutableStateOf(0) }
    var isSavingManual by remember { mutableStateOf(false) }
    var activeEditTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }
    var isEditingTransaction by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MoneyCanvas)
            .verticalScroll(scrollState)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "거래",
            subtitle = "자동 기록된 결제와 직접 입력한 거래를 모아요."
        )
        saveSuccessMessage?.let { message ->
            AssistChip(onClick = {}, label = { Text(message) })
        }

        FinanceSectionCard(
            title = "최근 거래",
            subtitle = "검토 완료된 기록만 보여줘요",
            accent = MoneyBlue,
            icon = Icons.AutoMirrored.Filled.List
        ) {
            rows.forEach { transaction ->
                TransactionRow(
                    transaction = transaction,
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
            if (rows.isEmpty()) {
                Text("아직 이번 달 기록이 없어요.")
            }
        }

        FinanceSectionCard(
            title = "수동 입력",
            subtitle = "자동 알림에 없는 거래만 직접 추가",
            accent = MoneyBlue,
            icon = Icons.AutoMirrored.Filled.List
        ) {
            manualFormMessage?.let { Text(it, fontWeight = FontWeight.Medium) }
            ManualTransactionForm(
                isSaving = isSavingManual,
                resetSignal = manualFormResetSignal,
                accountNames = assetAccounts.map { it.name }
            ) { type, amountWon, category, memo, occurredAt, accountName ->
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
                            paymentMethod = accountName
                        )
                        val savedMonth = YearMonth.from(occurredAt.atZone(manualTransactionZoneId))
                        saveSuccessMessage = if (savedMonth == month) {
                            "수동 거래를 저장했어요. 최근 거래에 반영했어요."
                        } else {
                            "수동 거래를 저장했어요. 선택한 달 기록에 반영했어요."
                        }
                        manualFormMessage = null
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
            accountNames = assetAccounts.map { it.name },
            onDismiss = {
                activeEditTransaction = null
                editErrorMessage = null
            },
            onSave = { amountWon, category, memo, occurredAt, paymentMethod, transactionType ->
                val useCase = editTransactionUseCase ?: return@TransactionEditDialog
                scope.launch {
                    isEditingTransaction = true
                    editErrorMessage = null
                    try {
                        useCase.update(transaction, amountWon, category, memo, occurredAt, paymentMethod, transactionType)
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
}
