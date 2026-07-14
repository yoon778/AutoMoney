package com.choiyoonseo.automoney.domain.assets

data class AccountTransferResult(
    val fromAccount: AssetAccount,
    val toAccount: AssetAccount
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
