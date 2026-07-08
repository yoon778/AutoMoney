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
    isReportableTransaction() && direction == TransactionDirection.INCOME

fun MoneyTransaction.countsAsActualExpense(): Boolean =
    isReportableTransaction() && type.countsAsMonthlyExpense

fun MoneyTransaction.countsAsSavingMovement(): Boolean =
    isReportableTransaction() &&
        (type == TransactionType.SAVING || type == TransactionType.INVESTMENT)
