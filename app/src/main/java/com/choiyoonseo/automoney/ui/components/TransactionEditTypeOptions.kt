package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.TransactionType

data class TransactionEditTypeOption(
    val type: TransactionType,
    val label: String
)

val transactionEditTypeOptions: List<TransactionEditTypeOption> = listOf(
    TransactionEditTypeOption(TransactionType.EXPENSE, "지출"),
    TransactionEditTypeOption(TransactionType.INCOME, "수입/입금"),
    TransactionEditTypeOption(TransactionType.TRANSFER, "계좌 이동/송금"),
    TransactionEditTypeOption(TransactionType.SETTLEMENT, "n분의1 정산"),
    TransactionEditTypeOption(TransactionType.EXCLUDED, "지출 아님")
)

fun typeLabelForEdit(type: TransactionType): String =
    transactionEditTypeOptions.firstOrNull { it.type == type }?.label ?: type.name
