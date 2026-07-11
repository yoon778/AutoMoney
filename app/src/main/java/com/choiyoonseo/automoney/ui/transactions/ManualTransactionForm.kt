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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.choiyoonseo.automoney.ui.components.MoneyPickerField
import com.choiyoonseo.automoney.ui.settings.SharedPreferencesCategoryPreferenceStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.domain.manual.ManualEntryType
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTransactionForm(
    isSaving: Boolean = false,
    resetSignal: Int = 0,
    accounts: List<AssetAccount> = emptyList(),
    onSave: (
        type: ManualEntryType,
        amountWon: Long,
        category: String,
        memo: String,
        occurredAt: Instant,
        account: AssetAccount?
    ) -> Unit
) {
    val defaults = remember(resetSignal) {
        defaultManualTransactionFormValues(LocalDate.now(manualTransactionZoneId))
    }
    var entryType by remember(resetSignal) { mutableStateOf(defaults.entryType) }
    var selectedDate by remember(resetSignal) { mutableStateOf(defaults.selectedDate) }
    var amount by remember(resetSignal) { mutableStateOf(defaults.amount) }
    var category by remember(resetSignal) { mutableStateOf(defaults.expenseCategory) }
    var incomeCategory by remember(resetSignal) { mutableStateOf(defaults.incomeCategory) }
    var memo by remember(resetSignal) { mutableStateOf(defaults.memo) }
    var inputError by remember(resetSignal) { mutableStateOf(defaults.inputError) }
    var categoryMenuExpanded by remember(resetSignal) { mutableStateOf(defaults.isCategoryMenuExpanded) }
    var accountMenuExpanded by remember(resetSignal) { mutableStateOf(false) }
    val selectableAccounts = accounts.filter { it.id > 0 }
    val categoryContext = LocalContext.current
    val categoryStore = remember { SharedPreferencesCategoryPreferenceStore(categoryContext) }
    val enabledExpenseOptions = remember { categoryStore.enabledExpenseCategories().map { ManualCategoryOption(it) } }
    val enabledIncomeOptions = remember { categoryStore.enabledIncomeCategories().map { ManualCategoryOption(it) } }
    var selectedAccountId by remember(resetSignal, selectableAccounts) {
        mutableStateOf(selectableAccounts.firstOrNull()?.id)
    }
    val selectedAccount = selectableAccounts.firstOrNull { account -> account.id == selectedAccountId }
    val selectedAccountName = selectedAccount?.name.orEmpty()
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
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("수동 입력")
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
        if (entryType != ManualEntryType.TRANSFER && selectableAccounts.isNotEmpty()) {
            Box(Modifier.fillMaxWidth()) {
                MoneyPickerField(
                    label = "사용 계좌",
                    value = selectedAccountName.ifBlank { "계좌 선택" },
                    onClick = { accountMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectableAccounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccountId = account.id
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        if (entryType != ManualEntryType.TRANSFER) {
            val categoryOptions = when (entryType) {
                ManualEntryType.EXPENSE -> enabledExpenseOptions
                ManualEntryType.INCOME -> enabledIncomeOptions
                ManualEntryType.TRANSFER -> emptyList()
            }
            val selectedCategory = when (entryType) {
                ManualEntryType.EXPENSE -> category
                ManualEntryType.INCOME -> incomeCategory
                ManualEntryType.TRANSFER -> defaultManualCategoryOption
            }
            Text("분류")
            Box(Modifier.fillMaxWidth()) {
                Button(
                    onClick = { categoryMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedCategory.label, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "분류 선택")
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                when (entryType) {
                                    ManualEntryType.EXPENSE -> category = option
                                    ManualEntryType.INCOME -> incomeCategory = option
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
            label = { Text("메모") },
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
                            ManualEntryType.EXPENSE -> category.label
                            ManualEntryType.INCOME -> incomeCategory.label
                            ManualEntryType.TRANSFER -> "기타"
                        }
                        onSave(
                            entryType,
                            parsed,
                            categoryForSave,
                            memo,
                            selectedDate.toManualTransactionInstant(),
                            selectedAccount.takeIf { entryType != ManualEntryType.TRANSFER }
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
