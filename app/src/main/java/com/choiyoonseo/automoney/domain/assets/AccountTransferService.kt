package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Duration

data class AccountTransferResult(
    val fromAccount: AssetAccount,
    val toAccount: AssetAccount
)

data class AccountTransferCandidate(
    val outgoingTransactionId: Long,
    val incomingTransactionId: Long,
    val amountWon: Long
)

fun applyAccountTransfer(
    accounts: List<AssetAccount>,
    fromAccountName: String,
    toAccountName: String,
    amountWon: Long
): AccountTransferResult {
    require(amountWon > 0) { "이동 금액은 0원보다 커야 해요." }
    require(fromAccountName != toAccountName) { "출금 계좌와 입금 계좌가 달라야 해요." }

    val fromAccount = accounts.firstOrNull { it.name == fromAccountName }
        ?: throw IllegalArgumentException("출금 계좌를 찾을 수 없어요.")
    val toAccount = accounts.firstOrNull { it.name == toAccountName }
        ?: throw IllegalArgumentException("입금 계좌를 찾을 수 없어요.")

    return AccountTransferResult(
        fromAccount = fromAccount.copy(balanceWon = fromAccount.balanceWon - amountWon),
        toAccount = toAccount.copy(balanceWon = toAccount.balanceWon + amountWon)
    )
}

fun findAccountTransferCandidates(
    transactions: List<MoneyTransaction>,
    tolerance: Duration = Duration.ofMinutes(10)
): List<AccountTransferCandidate> {
    val outgoingTransfers = transactions.filter { transaction ->
        transaction.status == TransactionStatus.NEEDS_REVIEW &&
            transaction.type == TransactionType.TRANSFER &&
            transaction.direction == TransactionDirection.NEUTRAL
    }
    val incomingTransfers = transactions.filter { transaction ->
        transaction.status == TransactionStatus.NEEDS_REVIEW &&
            transaction.type == TransactionType.INCOME &&
            transaction.direction == TransactionDirection.INCOME
    }

    return outgoingTransfers.flatMap { outgoing ->
        incomingTransfers
            .filter { incoming ->
                incoming.amount == outgoing.amount &&
                    timeDistance(outgoing, incoming) <= tolerance
            }
            .map { incoming ->
                AccountTransferCandidate(
                    outgoingTransactionId = outgoing.id,
                    incomingTransactionId = incoming.id,
                    amountWon = outgoing.amount.won
                )
            }
    }
}

private fun timeDistance(
    first: MoneyTransaction,
    second: MoneyTransaction
): Duration {
    val duration = Duration.between(first.occurredAt, second.occurredAt)
    return if (duration.isNegative) duration.negated() else duration
}
