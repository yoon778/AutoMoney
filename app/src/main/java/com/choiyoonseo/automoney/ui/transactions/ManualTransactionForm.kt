package com.choiyoonseo.automoney.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.choiyoonseo.automoney.ui.components.MoneyPickerField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.domain.assets.CategoryBudgetUsage
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.manual.ManualEntryType
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTransactionForm(
    isSaving: Boolean = false,
    resetSignal: Int = 0,
    initialAmountWon: Long? = null,
    budgetUsages: List<CategoryBudgetUsage> = emptyList(),
    fixedExpenses: List<FixedExpensePlan> = emptyList(),
    expenseCategoryLabels: List<String> = emptyList(),
    incomeCategoryLabels: List<String> = emptyList(),
    onSave: (
        type: ManualEntryType,
        amountWon: Long,
        category: String,
        memo: String,
        occurredAt: Instant,
        budgetPlanId: Long?,
        fixedExpensePlanId: Long?
    ) -> Unit
) {
    val defaults = remember(resetSignal) {
        defaultManualTransactionFormValues(LocalDate.now(manualTransactionZoneId))
    }
    var entryType by remember(resetSignal) { mutableStateOf(defaults.entryType) }
    var selectedDate by remember(resetSignal) { mutableStateOf(defaults.selectedDate) }
    var amount by remember(resetSignal) {
        mutableStateOf(initialAmountWon?.takeIf { it > 0 }?.toString() ?: defaults.amount)
    }
    var expenseCategoryLabel by remember(resetSignal) { mutableStateOf(defaults.expenseCategory.label) }
    var incomeCategoryLabel by remember(resetSignal) { mutableStateOf(defaults.incomeCategory.label) }
    var memo by remember(resetSignal) { mutableStateOf(defaults.memo) }
    var inputError by remember(resetSignal) { mutableStateOf(defaults.inputError) }
    var categoryMenuExpanded by remember(resetSignal) { mutableStateOf(defaults.isCategoryMenuExpanded) }
    var budgetMenuExpanded by remember(resetSignal) { mutableStateOf(false) }
    var selectedBudgetPlanId by remember(resetSignal) { mutableStateOf<Long?>(null) }
    var selectedFixedPlanId by remember(resetSignal) { mutableStateOf<Long?>(null) }
    val expenseOptions = expenseCategoryLabels.ifEmpty { manualExpenseCategoryOptions.map { it.label } }
    val incomeOptions = incomeCategoryLabels.ifEmpty { manualIncomeCategoryOptions.map { it.label } }
    var isDatePickerOpen by remember(resetSignal) { mutableStateOf(defaults.isDatePickerOpen) }

    if (isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toDatePickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            selectedDate = selectedMillis.toDatePickerLocalDate()
                        }
                        isDatePickerOpen = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("종류")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ManualEntryType.entries.forEach { option ->
                if (entryType == option) {
                    Button(
                        onClick = { entryType = option },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(option.label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { entryType = option },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(option.label)
                    }
                }
            }
        }
        Text("날짜")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val today = LocalDate.now(manualTransactionZoneId)
            val yesterday = today.minusDays(1)
            if (selectedDate == today) {
                Button(
                    onClick = { selectedDate = today },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("오늘")
                }
            } else {
                OutlinedButton(
                    onClick = { selectedDate = today },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("오늘")
                }
            }
            if (selectedDate == yesterday) {
                Button(
                    onClick = { selectedDate = yesterday },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("어제")
                }
            } else {
                OutlinedButton(
                    onClick = { selectedDate = yesterday },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("어제")
                }
            }
            OutlinedButton(
                onClick = { isDatePickerOpen = true },
                modifier = Modifier.weight(1.3f)
            ) {
                Text(selectedDate.toManualTransactionDateLabel())
            }
        }
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("금액") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (entryType == ManualEntryType.EXPENSE && (budgetUsages.isNotEmpty() || fixedExpenses.isNotEmpty())) {
            Box(Modifier.fillMaxWidth()) {
                MoneyPickerField(
                    label = "차감 예산",
                    value = when {
                        selectedFixedPlanId != null ->
                            fixedExpenses.firstOrNull { it.id == selectedFixedPlanId }
                                ?.let { "고정 · ${it.name}" }
                                ?: "분류 따라 자동"
                        selectedBudgetPlanId != null ->
                            budgetUsages.firstOrNull { it.plan.id == selectedBudgetPlanId }
                                ?.let { "예산 · ${it.plan.label} 남음 ${formatWon(it.remainingWon)}" }
                                ?: "분류 따라 자동"
                        else -> "분류 따라 자동"
                    },
                    onClick = { budgetMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = budgetMenuExpanded,
                    onDismissRequest = { budgetMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("분류 따라 자동") },
                        onClick = {
                            selectedBudgetPlanId = null
                            selectedFixedPlanId = null
                            budgetMenuExpanded = false
                        }
                    )
                    if (budgetUsages.isNotEmpty()) {
                        DropdownSectionHeader("변동 예산")
                        budgetUsages.forEach { usage ->
                            DropdownMenuItem(
                                text = { Text("${usage.plan.label} · 남음 ${formatWon(usage.remainingWon)}") },
                                onClick = {
                                    selectedBudgetPlanId = usage.plan.id
                                    selectedFixedPlanId = null
                                    budgetMenuExpanded = false
                                }
                            )
                        }
                    }
                    if (fixedExpenses.isNotEmpty()) {
                        if (budgetUsages.isNotEmpty()) {
                            HorizontalDivider(color = MoneyTheme.colors.canvas)
                        }
                        DropdownSectionHeader("고정지출")
                        fixedExpenses.forEach { plan ->
                            DropdownMenuItem(
                                text = { Text("${plan.name} · ${formatWon(plan.amountWon)}") },
                                onClick = {
                                    selectedFixedPlanId = plan.id
                                    selectedBudgetPlanId = null
                                    budgetMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        if (entryType != ManualEntryType.TRANSFER) {
            val categoryOptions = when (entryType) {
                ManualEntryType.EXPENSE,
                ManualEntryType.SPECIAL_EXPENSE -> expenseOptions
                ManualEntryType.INCOME -> incomeOptions
                ManualEntryType.TRANSFER -> emptyList()
            }
            val selectedCategory = when (entryType) {
                ManualEntryType.EXPENSE,
                ManualEntryType.SPECIAL_EXPENSE -> expenseCategoryLabel
                ManualEntryType.INCOME -> incomeCategoryLabel
                ManualEntryType.TRANSFER -> defaultManualCategoryOption.label
            }
            Text("분류")
            Box(Modifier.fillMaxWidth()) {
                Button(
                    onClick = { categoryMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedCategory, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "분류 선택")
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryOptions.forEach { label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                when (entryType) {
                                    ManualEntryType.EXPENSE,
                                    ManualEntryType.SPECIAL_EXPENSE -> expenseCategoryLabel = label
                                    ManualEntryType.INCOME -> incomeCategoryLabel = label
                                    ManualEntryType.TRANSFER -> Unit
                                }
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            label = {
                Text(if (entryType == ManualEntryType.SPECIAL_EXPENSE) "특별지출 사유" else "메모")
            },
            modifier = Modifier.fillMaxWidth()
        )
        inputError?.let { Text(it) }
        Row(Modifier.padding(top = 8.dp).fillMaxWidth()) {
            Button(
                enabled = !isSaving,
                onClick = {
                    val parsed = amount.replace(",", "").toLongOrNull()
                    if (parsed == null || parsed <= 0) {
                        inputError = "금액은 0원보다 크게 입력해 주세요."
                    } else {
                        inputError = null
                        val categoryForSave = when (entryType) {
                            ManualEntryType.EXPENSE,
                            ManualEntryType.SPECIAL_EXPENSE -> expenseCategoryLabel
                            ManualEntryType.INCOME -> incomeCategoryLabel
                            ManualEntryType.TRANSFER -> "기타"
                        }
                        onSave(
                            entryType,
                            parsed,
                            categoryForSave,
                            memo,
                            selectedDate.toManualTransactionInstant(),
                            selectedBudgetPlanId.takeIf { entryType == ManualEntryType.EXPENSE },
                            selectedFixedPlanId.takeIf { entryType == ManualEntryType.EXPENSE }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "저장 중" else "거래 저장")
            }
        }
    }
}

@Composable
private fun DropdownSectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MoneyTheme.colors.muted,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp)
    )
}
