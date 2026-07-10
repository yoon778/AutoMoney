package com.choiyoonseo.automoney.domain.assets

sealed interface AssetAccountMatch {
    data class Matched(val account: AssetAccount) : AssetAccountMatch
    data object Missing : AssetAccountMatch
    data object Ambiguous : AssetAccountMatch
}

fun matchAssetAccount(accounts: List<AssetAccount>, hint: BankAccountHint): AssetAccountMatch {
    val matches = accounts.filter { account ->
        account.kind == AssetAccountKind.BANK &&
            account.bankProvider == hint.provider &&
            account.accountLast4 == hint.accountLast4
    }
    return when (matches.size) {
        0 -> AssetAccountMatch.Missing
        1 -> AssetAccountMatch.Matched(matches.single())
        else -> AssetAccountMatch.Ambiguous
    }
}
