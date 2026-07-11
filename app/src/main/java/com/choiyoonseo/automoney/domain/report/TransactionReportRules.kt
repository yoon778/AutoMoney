package com.choiyoonseo.automoney.domain.report

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
        (type.countsAsMonthlyExpense ||
            (type == TransactionType.SETTLEMENT && settlementMyShareWon != null))

fun MoneyTransaction.effectiveExpenseWon(): Long =
    if (type == TransactionType.SETTLEMENT) settlementMyShareWon ?: 0 else amount.won

fun MoneyTransaction.countsAsSavingMovement(): Boolean =
    isReportableTransaction() &&
        (type == TransactionType.SAVING || type == TransactionType.INVESTMENT)
