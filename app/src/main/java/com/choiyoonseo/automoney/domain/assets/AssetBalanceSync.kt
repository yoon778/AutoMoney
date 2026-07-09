package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType

fun applyTransactionBalance(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction
): List<AssetAccount> = applyBalanceEffect(accounts, transaction, multiplier = 1)

fun removeTransactionBalance(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction
): List<AssetAccount> = applyBalanceEffect(accounts, transaction, multiplier = -1)

fun replaceTransactionBalance(
    accounts: List<AssetAccount>,
    oldTransaction: MoneyTransaction?,
    newTransaction: MoneyTransaction
): List<AssetAccount> {
    val restored = oldTransaction?.let { removeTransactionBalance(accounts, it) } ?: accounts
    return applyTransactionBalance(restored, newTransaction)
}

fun needsAccountMatchReview(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction
): Boolean {
    if (!transaction.canAffectBalance()) return false

    if (transaction.type == TransactionType.WALLET_TOPUP) {
        val from = transaction.paymentMethod.cleanOrNull() ?: return true
        val to = transaction.merchant.cleanOrNull() ?: transaction.counterparty.cleanOrNull() ?: return true
        return accounts.indexOfPaymentMethod(from) == -1 || accounts.indexOfPaymentMethod(to) == -1
    }

    if (transaction.singleAccountDeltaWon() == null) return false
    val paymentMethod = transaction.paymentMethod.cleanOrNull() ?: return true
    return accounts.indexOfPaymentMethod(paymentMethod) == -1
}

private fun applyBalanceEffect(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction,
    multiplier: Int
): List<AssetAccount> {
    if (transaction.type == TransactionType.WALLET_TOPUP) {
        return applyWalletTopup(accounts, transaction, multiplier)
    }

    if (!transaction.canAffectBalance()) return accounts

    val paymentMethod = transaction.paymentMethod.cleanOrNull() ?: return accounts
    val deltaWon = transaction.singleAccountDeltaWon() ?: return accounts
    val accountIndex = accounts.indexOfPaymentMethod(paymentMethod)
    if (accountIndex == -1) return accounts

    return accounts.mapIndexed { index, account ->
        if (index == accountIndex) {
            account.copy(balanceWon = account.balanceWon + (deltaWon * multiplier))
        } else {
            account
        }
    }
}

private fun applyWalletTopup(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction,
    multiplier: Int
): List<AssetAccount> {
    if (!transaction.canAffectBalance()) return accounts

    val from = transaction.paymentMethod.cleanOrNull() ?: return accounts
    val to = transaction.merchant.cleanOrNull() ?: transaction.counterparty.cleanOrNull() ?: return accounts
    val movement = BalanceMovement(from = from, to = to, amountWon = transaction.amount.won)
    return applyMovement(accounts, movement, multiplier)
}

private fun applyMovement(
    accounts: List<AssetAccount>,
    movement: BalanceMovement,
    multiplier: Int
): List<AssetAccount> {
    var fromMatched = false
    var toMatched = false

    val updated = accounts.map { account ->
        val isFrom = account.matchesPaymentMethod(movement.from)
        val isTo = account.matchesPaymentMethod(movement.to)
        when {
            isFrom && !isTo -> {
                fromMatched = true
                account.copy(balanceWon = account.balanceWon - (movement.amountWon * multiplier))
            }
            isTo && !isFrom -> {
                toMatched = true
                account.copy(balanceWon = account.balanceWon + (movement.amountWon * multiplier))
            }
            else -> account
        }
    }

    return if (fromMatched && toMatched) updated else accounts
}

private fun MoneyTransaction.canAffectBalance(): Boolean =
    status != TransactionStatus.NEEDS_REVIEW && status != TransactionStatus.EXCLUDED

private fun MoneyTransaction.singleAccountDeltaWon(): Long? {
    if (type == TransactionType.EXCLUDED ||
        type == TransactionType.TRANSFER ||
        type == TransactionType.WALLET_TOPUP
    ) {
        return null
    }

    return when {
        direction == TransactionDirection.INCOME -> amount.won
        direction == TransactionDirection.EXPENSE || type.countsAsMonthlyExpense -> -amount.won
        type == TransactionType.SAVING || type == TransactionType.INVESTMENT -> -amount.won
        else -> null
    }
}

private data class BalanceMovement(
    val from: String,
    val to: String,
    val amountWon: Long
)

private fun List<AssetAccount>.indexOfPaymentMethod(paymentMethod: String): Int =
    indexOfFirst { it.matchesPaymentMethod(paymentMethod) }

private fun AssetAccount.matchesPaymentMethod(paymentMethod: String): Boolean {
    return moneyNamesMatch(name, paymentMethod)
}

private fun String?.cleanOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
