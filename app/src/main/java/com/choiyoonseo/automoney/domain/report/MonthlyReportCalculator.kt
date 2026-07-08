package com.choiyoonseo.automoney.domain.report

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyTransaction

data class MonthlyReport(
    val incomeWon: Long,
    val expenseWon: Long,
    val savingWon: Long,
    val netWon: Long,
    val categoryExpenseWon: Map<Category, Long>
)

class MonthlyReportCalculator {
    fun calculate(transactions: List<MoneyTransaction>): MonthlyReport {
        val income = transactions
            .filter { it.countsAsReportIncome() }
            .sumOf { it.amount.won }

        val expenseTransactions = transactions.filter { it.countsAsActualExpense() }
        val expense = expenseTransactions.sumOf { it.amount.won }
        val saving = transactions
            .filter { it.countsAsSavingMovement() }
            .sumOf { it.amount.won }
        val categoryTotals = expenseTransactions
            .mapNotNull { transaction ->
                transaction.category?.let { category -> category to transaction.amount.won }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }

        return MonthlyReport(
            incomeWon = income,
            expenseWon = expense,
            savingWon = saving,
            netWon = income - expense - saving,
            categoryExpenseWon = categoryTotals
        )
    }
}
