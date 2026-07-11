package com.choiyoonseo.automoney.domain.settlement

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

data class SettlementRepaymentMatch(
    val settlementTransactionId: Long,
    val expectedAmountWon: Long
)

fun findSettlementMatch(
    incoming: MoneyTransaction,
    settlements: List<MoneyTransaction>,
    linkedRecoveries: List<MoneyTransaction>
): SettlementRepaymentMatch? {
    if (incoming.direction != TransactionDirection.INCOME || incoming.settlementParentId != null) {
        return null
    }

    return settlements.mapNotNull { settlement ->
        settlement.matchFor(incoming, linkedRecoveries)
    }.singleOrNull()
}

private fun MoneyTransaction.matchFor(
    incoming: MoneyTransaction,
    linkedRecoveries: List<MoneyTransaction>
): SettlementRepaymentMatch? {
    val partyCount = settlementPartyCount ?: return null
    val myShareWon = settlementMyShareWon ?: return null
    if (
        id <= 0 ||
        type != TransactionType.SETTLEMENT ||
        settlementTrackingHidden ||
        partyCount <= 0 ||
        myShareWon >= amount.won ||
        incoming.occurredAt.isBefore(occurredAt) ||
        incoming.occurredAt.isAfter(occurredAt.plus(14, ChronoUnit.DAYS))
    ) {
        return null
    }

    val remainingWon = (amount.won - myShareWon - linkedRecoveries
        .filter { it.settlementParentId == id }
        .sumOf { it.amount.won })
        .coerceAtLeast(0)
    if (remainingWon == 0L) return null

    val splitShareWon = (amount.won.toDouble() / partyCount).roundToLong()
    if (incoming.amount.won != splitShareWon && incoming.amount.won != remainingWon) return null

    return SettlementRepaymentMatch(
        settlementTransactionId = id,
        expectedAmountWon = incoming.amount.won
    )
}
