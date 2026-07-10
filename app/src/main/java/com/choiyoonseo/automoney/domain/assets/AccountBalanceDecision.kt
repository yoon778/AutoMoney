package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.ReviewReason

data class AccountBalanceDecision(
    val linkedAssetAccountId: Long?,
    val balanceImpact: BalanceImpact,
    val reviewReason: ReviewReason?
)

fun decideAccountBalance(
    accounts: List<AssetAccount>,
    hint: BankAccountHint?,
    amountWon: Long
): AccountBalanceDecision {
    if (hint == null) return AccountBalanceDecision(null, BalanceImpact.NONE, null)

    return when (val match = matchAssetAccount(accounts, hint)) {
        AssetAccountMatch.Missing ->
            AccountBalanceDecision(null, BalanceImpact.NONE, ReviewReason.ACCOUNT_UNMATCHED)
        AssetAccountMatch.Ambiguous ->
            AccountBalanceDecision(null, BalanceImpact.NONE, ReviewReason.ACCOUNT_AMBIGUOUS)
        is AssetAccountMatch.Matched -> decideMatchedAccountBalance(match.account, hint.direction, amountWon)
    }
}

private fun decideMatchedAccountBalance(
    account: AssetAccount,
    direction: AccountMovementDirection,
    amountWon: Long
): AccountBalanceDecision = when (direction) {
    AccountMovementDirection.CREDIT ->
        AccountBalanceDecision(account.id, BalanceImpact.CREDIT, null)
    AccountMovementDirection.DEBIT -> {
        if (account.balanceWon < amountWon) {
            AccountBalanceDecision(null, BalanceImpact.NONE, ReviewReason.BALANCE_MISMATCH)
        } else {
            AccountBalanceDecision(account.id, BalanceImpact.DEBIT, null)
        }
    }
    AccountMovementDirection.UNKNOWN ->
        AccountBalanceDecision(null, BalanceImpact.NONE, ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN)
}
