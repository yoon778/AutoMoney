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
    if (oldTransaction?.balanceImpact != null && newTransaction.balanceImpact != null) {
        return replaceExplicitBalance(accounts, oldTransaction, newTransaction)
    }
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
    val impact = transaction.balanceImpact
    if (impact != null) {
        return applyExplicitBalanceEffect(accounts, transaction, impact, multiplier)
    }
    return applyLegacyBalanceEffect(accounts, transaction, multiplier)
}

private fun applyExplicitBalanceEffect(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction,
    impact: BalanceImpact,
    multiplier: Int
): List<AssetAccount> {
    if (impact == BalanceImpact.NONE) return accounts
    val accountId = requireNotNull(transaction.linkedAssetAccountId) {
        "Explicit balance effect requires a linked account"
    }
    val accountIndex = accounts.indexOfFirst { account -> account.id == accountId }
    require(accountIndex >= 0) { "Linked account not found" }
    val sign = if (impact == BalanceImpact.CREDIT) 1 else -1
    val nextBalance = accounts[accountIndex].balanceWon + transaction.amount.won * sign * multiplier
    require(nextBalance >= 0) { "Balance effect cannot make an account negative" }
    return accounts.mapIndexed { index, account ->
        if (index == accountIndex) account.copy(balanceWon = nextBalance) else account
    }
}

private fun applyLegacyBalanceEffect(
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

private fun replaceExplicitBalance(
    accounts: List<AssetAccount>,
    oldTransaction: MoneyTransaction,
    newTransaction: MoneyTransaction
): List<AssetAccount> {
    val deltas = mutableMapOf<Long, Long>()

    fun add(transaction: MoneyTransaction, multiplier: Int) {
        val impact = requireNotNull(transaction.balanceImpact)
        if (impact == BalanceImpact.NONE) return
        val accountId = requireNotNull(transaction.linkedAssetAccountId) {
            "Explicit balance effect requires a linked account"
        }
        val sign = if (impact == BalanceImpact.CREDIT) 1 else -1
        deltas[accountId] = deltas.getOrDefault(accountId, 0L) +
            transaction.amount.won * sign * multiplier
    }

    add(oldTransaction, multiplier = -1)
    add(newTransaction, multiplier = 1)
    require(deltas.keys.all { id -> accounts.any { account -> account.id == id } }) {
        "Linked account not found"
    }
    return accounts.map { account ->
        val nextBalance = account.balanceWon + deltas.getOrDefault(account.id, 0L)
        require(nextBalance >= 0) { "Balance effect cannot make an account negative" }
        account.copy(balanceWon = nextBalance)
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
