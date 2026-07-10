package com.choiyoonseo.automoney.domain.manual

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

enum class ManualEntryType(val label: String) {
    EXPENSE("지출"),
    INCOME("수입"),
    TRANSFER("이체")
}

class SaveManualTransactionUseCase(
    private val repository: MoneyRepository
) {
    suspend fun save(
        type: ManualEntryType,
        amountWon: Long,
        categoryText: String,
        memo: String,
        occurredAt: Instant = Instant.now(),
        account: AssetAccount? = null,
        paymentMethod: String? = null
    ): Long {
        require(amountWon > 0) { "금액은 0원보다 커야 해요." }

        val cleanMemo = memo.trim()
        val cleanPaymentMethod = paymentMethod?.trim()?.takeIf { it.isNotBlank() } ?: "수동 입력"
        val monthKey = YearMonth.from(occurredAt.atZone(ZoneId.of("Asia/Seoul")))
        val accountId = account?.id?.takeIf { it > 0 }
        val accountPaymentMethod = account
            ?.takeIf { it.id == accountId }
            ?.name
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: cleanPaymentMethod
        val impact = when {
            accountId == null -> BalanceImpact.NONE
            type == ManualEntryType.EXPENSE -> BalanceImpact.DEBIT
            type == ManualEntryType.INCOME -> BalanceImpact.CREDIT
            else -> BalanceImpact.NONE
        }
        val transaction = when (type) {
            ManualEntryType.EXPENSE -> MoneyTransaction(
                occurredAt = occurredAt,
                amount = MoneyAmount(amountWon),
                direction = TransactionDirection.EXPENSE,
                type = TransactionType.EXPENSE,
                category = categoryText.toCategory(),
                paymentMethod = accountPaymentMethod,
                merchant = cleanMemo.ifBlank { "수동 입력" },
                counterparty = null,
                memo = cleanMemo.ifBlank { null },
                sourceApp = null,
                sourceType = SourceType.MANUAL,
                sourceNotificationHash = null,
                status = TransactionStatus.USER_EDITED,
                confidence = 1.0,
                monthKey = monthKey,
                linkedAssetAccountId = accountId,
                balanceImpact = impact
            )

            ManualEntryType.INCOME -> MoneyTransaction(
                occurredAt = occurredAt,
                amount = MoneyAmount(amountWon),
                direction = TransactionDirection.INCOME,
                type = TransactionType.INCOME,
                category = categoryText.toCategory(),
                paymentMethod = accountPaymentMethod,
                merchant = cleanMemo.ifBlank { "수동 수입" },
                counterparty = null,
                memo = cleanMemo.ifBlank { null },
                sourceApp = null,
                sourceType = SourceType.MANUAL,
                sourceNotificationHash = null,
                status = TransactionStatus.USER_EDITED,
                confidence = 1.0,
                monthKey = monthKey,
                linkedAssetAccountId = accountId,
                balanceImpact = impact
            )

            ManualEntryType.TRANSFER -> MoneyTransaction(
                occurredAt = occurredAt,
                amount = MoneyAmount(amountWon),
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.TRANSFER,
                category = null,
                paymentMethod = accountPaymentMethod,
                merchant = null,
                counterparty = cleanMemo.ifBlank { "수동 이체" },
                memo = cleanMemo.ifBlank { null },
                sourceApp = null,
                sourceType = SourceType.MANUAL,
                sourceNotificationHash = null,
                status = TransactionStatus.USER_EDITED,
                confidence = 1.0,
                monthKey = monthKey,
                linkedAssetAccountId = accountId,
                balanceImpact = impact
            )
        }

        return repository.saveTransaction(transaction)
    }

    suspend fun saveExpense(
        amountWon: Long,
        categoryText: String,
        memo: String,
        occurredAt: Instant = Instant.now()
    ): Long = save(
        type = ManualEntryType.EXPENSE,
        amountWon = amountWon,
        categoryText = categoryText,
        memo = memo,
        occurredAt = occurredAt
    )

    private fun String.toCategory(): Category {
        val cleanText = trim()
        return Category.entries.firstOrNull { category ->
            category.displayName == cleanText || category.name.equals(cleanText, ignoreCase = true)
        } ?: Category.OTHER
    }
}
