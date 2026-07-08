package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    var dateText by remember(transaction.id) { mutableStateOf(transaction.occurredAt.toEditDateText()) }
    var timeText by remember(transaction.id) { mutableStateOf(transaction.occurredAt.toEditTimeText()) }
    var selectedCategoryLabel by remember(transaction.id) {
        mutableStateOf(categoryLabelForEdit(transaction.category))
    }
    var categoryMenuExpanded by remember(transaction.id) { mutableStateOf(false) }
    var selectedType by remember(transaction.id) { mutableStateOf(transaction.type) }
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
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("날짜") },
                    placeholder = { Text("2026-07-06") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("시간") },
                    placeholder = { Text("14:30") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
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
                        transactionEditCategoryOptions.forEach { option ->
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
                    val occurredAt = parseEditDateTime(dateText, timeText)
                    when {
                        amountWon == null -> localErrorMessage = "금액을 확인해 주세요."
                        occurredAt == null -> localErrorMessage = "날짜와 시간을 확인해 주세요."
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

private val editZoneId: ZoneId = ZoneId.of("Asia/Seoul")
private val editDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val editTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun Instant.toEditDateText(): String =
    atZone(editZoneId).toLocalDate().format(editDateFormatter)

private fun Instant.toEditTimeText(): String =
    atZone(editZoneId).toLocalTime().format(editTimeFormatter)

private fun parseEditDateTime(dateText: String, timeText: String): Instant? {
    return try {
        val date = LocalDate.parse(dateText.trim(), editDateFormatter)
        val time = LocalTime.parse(timeText.trim(), editTimeFormatter)
        date.atTime(time).atZone(editZoneId).toInstant()
    } catch (_: RuntimeException) {
        null
    }
}
