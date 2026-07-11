package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.ui.settings.SharedPreferencesCategoryPreferenceStore
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditDialog(
    transaction: MoneyTransaction,
    isSaving: Boolean,
    errorMessage: String?,
    accounts: List<AssetAccount> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        amountWon: Long,
        category: String,
        memo: String,
        occurredAt: Instant,
        account: AssetAccount?,
        transactionType: TransactionType
    ) -> Unit,
    onExclude: () -> Unit,
    onDelete: (() -> Unit)? = null,
    excludeLabel: String = "\uc9c0\ucd9c\uc5d0\uc11c \uc81c\uc678",
    deleteLabel: String = "\uc0ad\uc81c"
) {
    val context = LocalContext.current
    val categoryStore = remember { SharedPreferencesCategoryPreferenceStore(context) }
    val enabledExpense = remember { categoryStore.enabledExpenseCategories() }
    val enabledIncome = remember { categoryStore.enabledIncomeCategories() }
    var amountText by remember(transaction.id) { mutableStateOf(transaction.amount.won.toString()) }
    var selectedDate by remember(transaction.id) {
        mutableStateOf(transaction.occurredAt.toTransactionEditLocalDate())
    }
    var selectedTime by remember(transaction.id) {
        mutableStateOf(transaction.occurredAt.toTransactionEditLocalTime())
    }
    var isDatePickerOpen by remember(transaction.id) { mutableStateOf(false) }
    var isTimePickerOpen by remember(transaction.id) { mutableStateOf(false) }
    var selectedType by remember(transaction.id) { mutableStateOf(transaction.type) }
    var selectedCategoryLabel by remember(transaction.id) {
        mutableStateOf(defaultCategoryLabelForEdit(transaction.type, transaction.category, enabledExpense, enabledIncome))
    }
    var categoryMenuExpanded by remember(transaction.id) { mutableStateOf(false) }
    var typeMenuExpanded by remember(transaction.id) { mutableStateOf(false) }
    val accountOptions = remember(
        transaction.id,
        accounts,
        transaction.linkedAssetAccountId,
        transaction.paymentMethod
    ) {
        accountOptionsForEdit(accounts, transaction.linkedAssetAccountId, transaction.paymentMethod)
    }
    var selectedAccountOption by remember(transaction.id, accountOptions) {
        mutableStateOf(selectedAccountOptionForEdit(accountOptions, transaction.linkedAssetAccountId))
    }
    val selectedAccountLabel = selectedAccountOption?.label
    var accountMenuExpanded by remember(transaction.id, accountOptions) { mutableStateOf(false) }
    var memoText by remember(transaction.id) {
        mutableStateOf(transaction.memo ?: "")
    }
    var localErrorMessage by remember(transaction.id) { mutableStateOf<String?>(null) }
    val title = transaction.merchant ?: transaction.counterparty ?: "거래"
    val categoryOptions = transactionEditCategoryOptionsFor(selectedType, enabledExpense, enabledIncome)

    if (isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toTransactionEditDatePickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            selectedDate = selectedMillis.toTransactionEditDatePickerLocalDate()
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

    if (isTimePickerOpen) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { isTimePickerOpen = false },
            title = { Text("시간 선택") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = selectedTime
                            .withHour(timePickerState.hour)
                            .withMinute(timePickerState.minute)
                        isTimePickerOpen = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { isTimePickerOpen = false }) {
                    Text("취소")
                }
            }
        )
    }

    val colors = MoneyTheme.colors
    MoneyDialog(
        title = "거래 수정",
        subtitle = title,
        onDismiss = onDismiss,
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.negative)
                    ) {
                        Text(deleteLabel)
                    }
                }
                OutlinedButton(
                    onClick = onExclude,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(excludeLabel)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("취소")
                }
                Button(
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val amountWon = amountText.replace(",", "").trim().toLongOrNull()
                        val occurredAt = selectedDate.toTransactionEditInstant(selectedTime)
                        when {
                            amountWon == null -> localErrorMessage = "금액을 확인해 주세요."
                            else -> {
                                localErrorMessage = null
                                onSave(
                                    amountWon,
                                    selectedCategoryLabel,
                                    memoText,
                                    occurredAt,
                                    selectedAccountOption?.accountId?.let { accountId ->
                                        accounts.firstOrNull { it.id == accountId }
                                    },
                                    selectedType
                                )
                            }
                        }
                    }
                ) {
                    Text(if (isSaving) "저장 중" else "저장")
                }
            }
        }
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("금액") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoneyPickerField(
                label = "날짜",
                value = selectedDate.toTransactionEditDateText(),
                onClick = { isDatePickerOpen = true },
                modifier = Modifier.weight(1f)
            )
            MoneyPickerField(
                label = "시간",
                value = selectedTime.toTransactionEditTimeText(),
                onClick = { isTimePickerOpen = true },
                modifier = Modifier.weight(1f)
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            MoneyPickerField(
                label = "처리 유형",
                value = typeLabelForEdit(selectedType),
                onClick = { typeMenuExpanded = true }
            )
            DropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                transactionEditTypeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            selectedType = option.type
                            if (!isCategoryLabelValidForEdit(option.type, selectedCategoryLabel, enabledExpense, enabledIncome)) {
                                selectedCategoryLabel = defaultCategoryLabelForEdit(option.type, transaction.category, enabledExpense, enabledIncome)
                            }
                            typeMenuExpanded = false
                        }
                    )
                }
            }
        }
        if (categoryOptions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MoneyPickerField(
                    label = "분류",
                    value = selectedCategoryLabel,
                    onClick = { categoryMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                selectedCategoryLabel = option.label
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        if (accountOptions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MoneyPickerField(
                    label = "계좌",
                    value = selectedAccountLabel ?: "선택 안 함",
                    onClick = { accountMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    accountOptions.forEach { accountOption ->
                        DropdownMenuItem(
                            text = { Text(accountOption.label) },
                            onClick = {
                                selectedAccountOption = accountOption
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = memoText,
            onValueChange = { memoText = it },
            label = { Text("메모") },
            modifier = Modifier.fillMaxWidth()
        )
        (localErrorMessage ?: errorMessage)?.let {
            Text(it, color = colors.negative, style = MaterialTheme.typography.bodySmall)
        }
    }
}
