package com.choiyoonseo.automoney.domain.report

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType

fun MoneyTransaction.isReportableTransaction(): Boolean =
    status != TransactionStatus.NEEDS_REVIEW &&
        status != TransactionStatus.EXCLUDED &&
        type != TransactionType.EXCLUDED

fun MoneyTransaction.countsAsReportIncome(): Boolean =
    isReportableTransaction() &&
        direction == TransactionDirection.INCOME &&
        settlementParentId == null

fun MoneyTransaction.countsAsActualExpense(): Boolean =
    isReportableTransaction() &&
        !isSavingCategory() &&
        (type.countsAsMonthlyExpense ||
            (type == TransactionType.SETTLEMENT && settlementMyShareWon != null))

fun MoneyTransaction.effectiveExpenseWon(): Long =
    if (type == TransactionType.SETTLEMENT) settlementMyShareWon ?: 0 else amount.won

fun MoneyTransaction.countsAsSavingMovement(): Boolean =
    isReportableTransaction() &&
        (type == TransactionType.SAVING || type == TransactionType.INVESTMENT || isSavingCategory())

fun MoneyTransaction.countsAsPlannedUse(): Boolean =
    countsAsActualExpense() ||
        isReportableTransaction() && (type == TransactionType.SAVING || type == TransactionType.INVESTMENT)

// 고정지출 계획에 연결돼 종류가 FIXED_EXPENSE로 고정된 저축도 인식하려면 카테고리를 본다.
private fun MoneyTransaction.isSavingCategory(): Boolean =
    category == Category.SAVING && direction == TransactionDirection.EXPENSE
