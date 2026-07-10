package com.choiyoonseo.automoney.domain.assets

enum class AccountMovementDirection {
    CREDIT,
    DEBIT,
    UNKNOWN
}

enum class BankEventKind {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}

data class BankAccountHint(
    val provider: BankProvider,
    val accountLast4: String,
    val direction: AccountMovementDirection,
    val eventKind: BankEventKind
)

data class ParsedBankMovement(
    val amountWon: Long,
    val hint: BankAccountHint
)
