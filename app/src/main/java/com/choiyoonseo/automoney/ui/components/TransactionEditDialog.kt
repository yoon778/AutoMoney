package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditDialog(
    transaction: MoneyTransaction,
    isSaving: Boolean,
    errorMessage: String?,
    accountNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        amountWon: Long,
        category: String,
        memo: String,
        occurredAt: Instant,
        paymentMethod: String?,
        transactionType: TransactionType
    ) -> Unit,
    onExclude: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
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
        mutableStateOf(defaultCategoryLabelForEdit(transaction.type, transaction.category))
    }
    var categoryMenuExpanded by remember(transaction.id) { mutableStateOf(false) }
    var typeMenuExpanded by remember(transaction.id) { mutableStateOf(false) }
    val accountOptions = remember(transaction.id, accountNames, transaction.paymentMethod) {
        accountOptionsForEdit(accountNames, transaction.paymentMethod)
    }
    var selectedAccountLabel by remember(transaction.id, accountOptions) {
        mutableStateOf(accountLabelForEdit(transaction.paymentMethod, accountNames))
    }
    var accountMenuExpanded by remember(transaction.id, accountOptions) { mutableStateOf(false) }
    var memoText by remember(transaction.id) {
        mutableStateOf(transaction.memo ?: "")
    }
    var localErrorMessage by remember(transaction.id) { mutableStateOf<String?>(null) }
    val title = transaction.merchant ?: transaction.counterparty ?: "거래"
    val categoryOptions = transactionEditCategoryOptionsFor(selectedType)

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("거래 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("금액") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { isDatePickerOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("날짜: ${selectedDate.toTransactionEditDateText()}")
                }
                OutlinedButton(
                    onClick = { isTimePickerOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("시간: ${selectedTime.toTransactionEditTimeText()}")
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("처리 유형: ${typeLabelForEdit(selectedType)}")
                    }
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
                                    if (!isCategoryLabelValidForEdit(option.type, selectedCategoryLabel)) {
                                        selectedCategoryLabel = defaultCategoryLabelForEdit(option.type, transaction.category)
                                    }
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                if (categoryOptions.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("분류: $selectedCategoryLabel")
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
                        OutlinedButton(
                            onClick = { accountMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("계좌: ${selectedAccountLabel ?: "선택 안 함"}")
                        }
                        DropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            accountOptions.forEach { accountName ->
                                DropdownMenuItem(
                                    text = { Text(accountName) },
                                    onClick = {
                                        selectedAccountLabel = accountName
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
                (localErrorMessage ?: errorMessage)?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
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
                                selectedAccountLabel,
                                selectedType
                            )
                        }
                    }
                }
            ) {
                Text(if (isSaving) "저장 중" else "저장")
            }
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete, enabled = !isSaving) {
                        Text("삭제")
                    }
                }
                OutlinedButton(onClick = onExclude, enabled = !isSaving) {
                    Text("지출 제외")
                }
                OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("취소")
                }
            }
        }
    )
}
