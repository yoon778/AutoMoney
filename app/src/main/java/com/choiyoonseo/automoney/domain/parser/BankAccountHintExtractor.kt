package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.choiyoonseo.automoney.domain.assets.ParsedBankMovement

class BankAccountHintExtractor {
    fun extract(provider: BankProvider, text: String): ParsedBankMovement? {
        val movementLines = text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filter(::containsMovementKeyword)
            .toList()
        if (movementLines.size != 1) return null

        val movementLine = movementLines.single()
        val amountMatch = amountRegex.findAll(movementLine).singleOrNull() ?: return null
        val amountWon = amountMatch.groupValues[1].replace(",", "").toLongOrNull()
            ?.takeIf { it > 0 }
            ?: return null

        val accountText = amountRegex.replace(text, " ")
        val accountToken = accountCandidateRegex.findAll(accountText)
            .map { match -> match.groupValues[1].ifBlank { match.groupValues[2] } }
            .singleOrNull()
            ?: return null
        val accountDigits = accountToken.filter(Char::isDigit)
        if (accountDigits.length < 4) return null

        val hasCreditDirection = containsAny(movementLine, CREDIT_KEYWORDS)
        val hasDebitDirection = containsAny(movementLine, DEBIT_KEYWORDS)
        if (hasCreditDirection == hasDebitDirection) return null

        val direction = if (hasCreditDirection) {
            AccountMovementDirection.CREDIT
        } else {
            AccountMovementDirection.DEBIT
        }
        val eventKind = when {
            containsAny(movementLine, TRANSFER_KEYWORDS) -> BankEventKind.TRANSFER
            hasCreditDirection -> BankEventKind.DEPOSIT
            else -> BankEventKind.WITHDRAWAL
        }

        return ParsedBankMovement(
            amountWon = amountWon,
            hint = BankAccountHint(
                provider = provider,
                accountLast4 = accountDigits.takeLast(4),
                direction = direction,
                eventKind = eventKind
            )
        )
    }

    fun resolveAggregatorProvider(text: String): BankProvider? =
        BankProvider.entries.singleOrNull { provider ->
            text.contains(provider.displayName, ignoreCase = true)
        }

    private fun containsMovementKeyword(value: String): Boolean =
        containsAny(value, MOVEMENT_KEYWORDS)

    private fun containsAny(value: String, keywords: List<String>): Boolean =
        keywords.any { keyword -> value.contains(keyword, ignoreCase = true) }

    private val amountRegex = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
    private val accountCandidateRegex = Regex(
        """(?:계좌|통장)\s*[:：]?\s*([0-9*Xx-]{4,})|(?<![\d,])([0-9*Xx-]{7,})(?![\d,])"""
    )

    companion object {
        private val CREDIT_KEYWORDS = listOf("입금", "받았", "받음")
        private val DEBIT_KEYWORDS = listOf("출금", "보냈", "송금했", "이체완료")
        private val TRANSFER_KEYWORDS = listOf("송금", "이체")
        private val MOVEMENT_KEYWORDS =
            CREDIT_KEYWORDS + DEBIT_KEYWORDS + TRANSFER_KEYWORDS + listOf("ATM")
    }
}
