package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.categoryDisplayName
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.countsAsReportIncome
import com.choiyoonseo.automoney.domain.report.countsAsSavingMovement
import com.choiyoonseo.automoney.domain.report.effectiveExpenseWon
import com.choiyoonseo.automoney.domain.report.isReportableTransaction
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class MonthlySummaryUi(
    val monthTitle: String,
    val incomeWon: Long,
    val expenseWon: Long,
    val savingWon: Long,
    val netWon: Long,
    val savingsRatePercent: Int,
    val homeSnapshot: HomeSnapshot,
    val categorySpends: List<CategorySpendUi>
)

fun transactionsToMonthlySummary(
    month: YearMonth,
    transactions: List<MoneyTransaction>,
    reviewCount: Int
): MonthlySummaryUi {
    val monthlyTransactions = transactions.filter { it.monthKey == month && it.isReportableTransaction() }
    val incomeWon = monthlyTransactions
        .filter { it.countsAsReportIncome() }
        .sumOf { it.amount.won }
    val expenseTransactions = monthlyTransactions.filter { it.countsAsActualExpense() }
    val expenseWon = expenseTransactions.sumOf { it.effectiveExpenseWon() }
    val savingWon = monthlyTransactions
        .filter { it.countsAsSavingMovement() }
        .sumOf { it.amount.won }
    val netWon = incomeWon - expenseWon - savingWon
    val savingsRatePercent = if (incomeWon > 0) {
        ((savingWon / incomeWon.toFloat()) * 100).roundToInt()
    } else {
        0
    }
    val categorySpends = expenseTransactions
        .mapNotNull { transaction ->
            transaction.categoryDisplayName()?.let { it to transaction.effectiveExpenseWon() }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, amounts) -> amounts.sum() }
        .toList()
        .sortedByDescending { (_, amount) -> amount }
        .map { (name, amount) ->
            CategorySpendUi(
                name = name,
                amountWon = amount,
                ratio = if (expenseWon > 0) amount / expenseWon.toFloat() else 0f
            )
        }

    val homeSnapshot = HomeSnapshot(
        monthTitle = "${month.monthValue}\uc6d4 \ub3c8 \ud750\ub984",
        netSavedWon = netWon,
        reviewCount = reviewCount,
        metrics = listOf(
            MetricTileUi(
                title = "\uc774\ubc88 \ub2ec \uc9c0\ucd9c",
                value = formatWon(expenseWon),
                progress = if (incomeWon > 0) expenseWon / incomeWon.toFloat() else 0f,
                helper = if (incomeWon > 0) {
                    "\uc218\uc785 \ub300\ube44 ${((expenseWon / incomeWon.toFloat()) * 100).roundToInt()}%"
                } else {
                    "\uc218\uc785 \uae30\ub85d \uc5c6\uc74c"
                }
            ),
            MetricTileUi(
                title = "\uc800\ucd95\ub960",
                value = "${savingsRatePercent}%",
                progress = savingsRatePercent / 100f,
                helper = if (savingWon > 0) "\uc800\ucd95 ${formatWon(savingWon)}" else "\uc800\ucd95 \uae30\ub85d \uc5c6\uc74c"
            ),
            MetricTileUi(
                title = "\uac80\ud1a0 \ud544\uc694",
                value = "${reviewCount}\uac74",
                progress = (reviewCount / 10f).coerceIn(0f, 1f),
                helper = if (reviewCount == 0) "\uc815\ub9ac \uc644\ub8cc" else "\ud655\uc778 \ud544\uc694"
            )
        ),
        recentTransactions = monthlyTransactions
            .filter { it.countsAsReportIncome() || it.countsAsActualExpense() }
            .sortedByDescending { it.occurredAt }
            .take(3)
            .map { it.toTransactionRowUi() }
    )

    return MonthlySummaryUi(
        monthTitle = "${month.monthValue}\uc6d4 \ub3c8 \ud750\ub984",
        incomeWon = incomeWon,
        expenseWon = expenseWon,
        savingWon = savingWon,
        netWon = netWon,
        savingsRatePercent = savingsRatePercent,
        homeSnapshot = homeSnapshot,
        categorySpends = categorySpends
    )
}

fun transactionsToRows(
    transactions: List<MoneyTransaction>,
    limit: Int = 20
): List<TransactionRowUi> =
    transactions
        .filter { it.isVisibleInLedger() }
        .sortedByDescending { it.occurredAt }
        .take(limit)
        .map { it.toTransactionRowUi() }

fun transactionsToDateSections(
    transactions: List<MoneyTransaction>,
    limit: Int = Int.MAX_VALUE,
    zoneId: ZoneId = AppDateZoneId
): List<TransactionDateSectionUi> =
    transactions
        .filter { it.isVisibleInLedger() }
        .sortedByDescending { it.occurredAt }
        .take(limit)
        .groupBy { it.occurredAt.atZone(zoneId).toLocalDate() }
        .map { (date, datedTransactions) ->
            TransactionDateSectionUi(
                date = date,
                dateLabel = date.format(transactionSectionDateFormatter),
                rows = datedTransactions.map { it.toTransactionRowUi() }
            )
        }

fun MoneyTransaction.isVisibleInLedger(): Boolean =
    status != TransactionStatus.NEEDS_REVIEW &&
        type != TransactionType.WALLET_TOPUP

private val transactionSectionDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일")

private fun MoneyTransaction.toTransactionRowUi(): TransactionRowUi {
    val displayAmount = if (direction == TransactionDirection.EXPENSE || type.countsAsMonthlyExpense) {
        -amount.won
    } else {
        amount.won
    }
    val categoryText = categoryDisplayName() ?: if (type.countsAsMonthlyExpense) "\uae30\ud0c0" else "\uc9c0\ucd9c \uc81c\uc678"

    return TransactionRowUi(
        merchant = displayTitle(),
        category = categoryText,
        amountWon = displayAmount,
        method = paymentMethod?.trim()?.takeIf { it.isNotBlank() } ?: sourceType.name,
        iconText = categoryText.take(1),
        id = id.takeIf { it > 0 },
        isExcluded = status == TransactionStatus.EXCLUDED || type == TransactionType.EXCLUDED,
        sourceApp = sourceAppUiForPackage(sourceApp),
        sourceLabel = if (sourceType == SourceType.NOTIFICATION) "\uc790\ub3d9" else null
    )
}

private fun MoneyTransaction.displayTitle(): String =
    listOfNotNull(
        merchant.cleanOrNull(),
        counterparty.cleanOrNull(),
        memo.cleanOrNull(),
        incomeCategoryTitle()
    ).firstOrNull() ?: fallbackTitle()

private fun MoneyTransaction.incomeCategoryTitle(): String? =
    categoryDisplayName()?.takeIf { direction == TransactionDirection.INCOME }

private fun MoneyTransaction.fallbackTitle(): String =
    when (type) {
        TransactionType.WALLET_TOPUP -> "\ucda9\uc804/\ud3ec\uc778\ud2b8"
        TransactionType.TRANSFER -> "\uc1a1\uae08/\uacc4\uc88c \uc774\ub3d9"
        TransactionType.INCOME -> "\uc785\uae08"
        TransactionType.REFUND -> "\ud658\ubd88/\ucde8\uc18c"
        TransactionType.EXCLUDED -> "\uc9c0\ucd9c \uc544\ub2d8"
        TransactionType.SAVING -> "\uc800\ucd95"
        TransactionType.INVESTMENT -> "\ud22c\uc790"
        else -> when (direction) {
            TransactionDirection.INCOME -> "\uc218\uc785"
            TransactionDirection.EXPENSE -> "\uc9c0\ucd9c"
            TransactionDirection.NEUTRAL -> "\ub3c8 \uc774\ub3d9"
        }
    }

private fun String?.cleanOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
