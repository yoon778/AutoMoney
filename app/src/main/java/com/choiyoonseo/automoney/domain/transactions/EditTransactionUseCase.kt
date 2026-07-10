package com.choiyoonseo.automoney.domain.transactions

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.RuleAction
import com.choiyoonseo.automoney.domain.model.RuleMatchType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class EditTransactionUseCase(
    private val repository: MoneyRepository
) {
    suspend fun update(
        transaction: MoneyTransaction,
        amountWon: Long,
        categoryText: String,
        memo: String,
        occurredAt: Instant = transaction.occurredAt,
        account: AssetAccount? = null,
        paymentMethod: String? = transaction.paymentMethod,
        transactionType: TransactionType = transaction.type
    ) {
        require(amountWon > 0) { "금액은 0원보다 커야 해요." }

        val cleanMemo = memo.trim()
        val cleanPaymentMethod = paymentMethod?.trim()?.takeIf { it.isNotBlank() }
        val accountId = account?.id?.takeIf { it > 0 }
        val accountPaymentMethod = account
            ?.takeIf { it.id == accountId }
            ?.name
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: cleanPaymentMethod
        val linkedAssetAccountId = if (transactionType == TransactionType.EXCLUDED) {
            transaction.linkedAssetAccountId
        } else {
            accountId
        }
        val nextImpact = when (transactionType) {
            TransactionType.INCOME ->
                accountId?.let { BalanceImpact.CREDIT } ?: BalanceImpact.NONE
            TransactionType.EXPENSE,
            TransactionType.FIXED_EXPENSE,
            TransactionType.SAVING,
            TransactionType.INVESTMENT,
            TransactionType.WALLET_SPEND ->
                accountId?.let { BalanceImpact.DEBIT } ?: BalanceImpact.NONE
            TransactionType.TRANSFER,
            TransactionType.SETTLEMENT,
            TransactionType.REFUND,
            TransactionType.WALLET_TOPUP ->
                transaction.balanceImpact ?: BalanceImpact.NONE
            TransactionType.EXCLUDED -> transaction.balanceImpact ?: BalanceImpact.NONE
        }
        val updatedTransaction = transaction.copy(
            occurredAt = occurredAt,
            amount = MoneyAmount(amountWon),
            direction = transactionType.defaultDirection,
            type = transactionType,
            category = categoryText.toCategoryFor(transactionType),
            paymentMethod = accountPaymentMethod,
            memo = cleanMemo.ifBlank { null },
            status = if (transactionType == TransactionType.EXCLUDED) {
                TransactionStatus.EXCLUDED
            } else {
                TransactionStatus.USER_EDITED
            },
            confidence = 1.0,
            monthKey = YearMonth.from(occurredAt.atZone(koreanZoneId)),
            linkedAssetAccountId = linkedAssetAccountId,
            balanceImpact = nextImpact
        )
        repository.updateTransaction(updatedTransaction)
        saveLearnedRules(transaction, updatedTransaction)
    }

    suspend fun exclude(transaction: MoneyTransaction) {
        repository.updateTransaction(
            transaction.copy(
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.EXCLUDED,
                category = null,
                status = TransactionStatus.EXCLUDED,
                confidence = 1.0,
                memo = appendEditMemo(transaction.memo, "사용자 삭제")
            )
        )
    }

    suspend fun delete(transaction: MoneyTransaction) {
        if (transaction.id > 0L) {
            repository.deleteTransaction(transaction.id)
        }
    }

    private fun String.toCategory(): Category {
        val cleanText = trim()
        return Category.entries.firstOrNull { category ->
            category.displayName == cleanText || category.name.equals(cleanText, ignoreCase = true)
        } ?: Category.OTHER
    }

    private fun String.toCategoryFor(type: TransactionType): Category? =
        when (type) {
            TransactionType.EXPENSE,
            TransactionType.FIXED_EXPENSE,
            TransactionType.WALLET_SPEND,
            TransactionType.SAVING,
            TransactionType.INVESTMENT,
            TransactionType.INCOME -> toCategory()
            else -> null
        }

    private fun appendEditMemo(current: String?, note: String): String =
        current?.takeIf { it.isNotBlank() }?.let { "$it · $note" } ?: note

    private suspend fun saveLearnedRules(original: MoneyTransaction, updated: MoneyTransaction) {
        val match = original.learnedRuleMatch() ?: return
        val candidates = buildList {
            updated.category
                ?.takeIf { updated.type.canLearnCategoryRule() }
                ?.let { category ->
                    add(
                        Rule(
                            matchType = match.type,
                            matchValue = match.value,
                            action = RuleAction.SET_CATEGORY,
                            targetValue = category.name
                        )
                    )
                }

            if (updated.type != original.type || original.status == TransactionStatus.NEEDS_REVIEW) {
                add(updated.toTypeRule(match))
            }
        }
        if (candidates.isEmpty()) return

        val existingRules = repository.enabledRules()
        candidates
            .filterNot { candidate -> existingRules.any { it.isSameRuleAs(candidate) } }
            .forEach { repository.saveRule(it) }
    }

    private fun MoneyTransaction.learnedRuleMatch(): LearnedRuleMatch? =
        merchant.cleanOrNull()?.let { LearnedRuleMatch(RuleMatchType.MERCHANT, it) }
            ?: counterparty.cleanOrNull()?.let { LearnedRuleMatch(RuleMatchType.COUNTERPARTY, it) }

    private fun MoneyTransaction.toTypeRule(match: LearnedRuleMatch): Rule =
        Rule(
            matchType = match.type,
            matchValue = match.value,
            action = type.toRuleAction(),
            targetValue = type.name
        )

    private fun TransactionType.toRuleAction(): RuleAction =
        when (this) {
            TransactionType.EXCLUDED -> RuleAction.EXCLUDE
            TransactionType.WALLET_TOPUP -> RuleAction.MARK_AS_WALLET_TOPUP
            TransactionType.SETTLEMENT -> RuleAction.MARK_AS_SETTLEMENT
            else -> RuleAction.SET_TRANSACTION_TYPE
        }

    private fun TransactionType.canLearnCategoryRule(): Boolean =
        when (this) {
            TransactionType.EXPENSE,
            TransactionType.FIXED_EXPENSE,
            TransactionType.WALLET_SPEND,
            TransactionType.SAVING,
            TransactionType.INVESTMENT,
            TransactionType.INCOME -> true
            else -> false
        }

    private fun Rule.isSameRuleAs(other: Rule): Boolean =
        matchType == other.matchType &&
            matchValue.equals(other.matchValue, ignoreCase = true) &&
            action == other.action &&
            targetValue == other.targetValue

    private fun String?.cleanOrNull(): String? =
        this?.trim()?.takeIf { it.isNotBlank() }

    private data class LearnedRuleMatch(
        val type: RuleMatchType,
        val value: String
    )

    private companion object {
        val koreanZoneId: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
