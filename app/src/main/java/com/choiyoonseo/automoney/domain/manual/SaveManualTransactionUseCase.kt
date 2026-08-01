package com.choiyoonseo.automoney.domain.manual

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.category.TransactionCategoryResolver
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
    SPECIAL_EXPENSE("특별지출"),
    INCOME("수입"),
    TRANSFER("이체")
}

class SaveManualTransactionUseCase(
    private val repository: MoneyRepository,
    userCategoryRepository: UserCategoryRepository? = null
) {
    private val categoryResolver = TransactionCategoryResolver(userCategoryRepository)

    suspend fun save(
        type: ManualEntryType,
        amountWon: Long,
        categoryText: String,
        memo: String,
        occurredAt: Instant = Instant.now(),
        account: AssetAccount? = null,
        paymentMethod: String? = null,
        budgetPlanId: Long? = null,
        fixedExpensePlanId: Long? = null
    ): Long = repository.saveTransaction(
        createTransaction(
            type = type,
            amountWon = amountWon,
            categoryText = categoryText,
            memo = memo,
            occurredAt = occurredAt,
            account = account,
            paymentMethod = paymentMethod,
            budgetPlanId = budgetPlanId,
            fixedExpensePlanId = fixedExpensePlanId
        )
    )

    suspend fun createTransaction(
        type: ManualEntryType,
        amountWon: Long,
        categoryText: String,
        memo: String,
        occurredAt: Instant = Instant.now(),
        account: AssetAccount? = null,
        paymentMethod: String? = null,
        budgetPlanId: Long? = null,
        fixedExpensePlanId: Long? = null
    ): MoneyTransaction {
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
            type == ManualEntryType.EXPENSE || type == ManualEntryType.SPECIAL_EXPENSE ->
                BalanceImpact.DEBIT
            type == ManualEntryType.INCOME -> BalanceImpact.CREDIT
            else -> BalanceImpact.NONE
        }
        val selectedFixedExpensePlanId = fixedExpensePlanId?.takeIf { it > 0 }
        val transactionType = when (type) {
            ManualEntryType.EXPENSE -> if (selectedFixedExpensePlanId != null) {
                TransactionType.FIXED_EXPENSE
            } else {
                TransactionType.EXPENSE
            }
            ManualEntryType.SPECIAL_EXPENSE -> TransactionType.SPECIAL_EXPENSE
            ManualEntryType.INCOME -> TransactionType.INCOME
            ManualEntryType.TRANSFER -> TransactionType.TRANSFER
        }
        val categoryAssignment = categoryResolver.resolve(categoryText, transactionType)
        val transaction = when (type) {
            ManualEntryType.EXPENSE,
            ManualEntryType.SPECIAL_EXPENSE -> MoneyTransaction(
                occurredAt = occurredAt,
                amount = MoneyAmount(amountWon),
                direction = TransactionDirection.EXPENSE,
                type = transactionType,
                category = categoryAssignment.category,
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
                balanceImpact = impact,
                customCategoryId = categoryAssignment.customCategoryId,
                customCategoryName = categoryAssignment.customCategoryName,
                budgetPlanId = budgetPlanId.takeIf {
                    type == ManualEntryType.EXPENSE && selectedFixedExpensePlanId == null
                },
                fixedExpensePlanId = selectedFixedExpensePlanId.takeIf {
                    type == ManualEntryType.EXPENSE
                }
            )

            ManualEntryType.INCOME -> MoneyTransaction(
                occurredAt = occurredAt,
                amount = MoneyAmount(amountWon),
                direction = TransactionDirection.INCOME,
                type = TransactionType.INCOME,
                category = categoryAssignment.category,
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
                balanceImpact = impact,
                customCategoryId = categoryAssignment.customCategoryId,
                customCategoryName = categoryAssignment.customCategoryName
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
                linkedAssetAccountId = null,
                balanceImpact = BalanceImpact.NONE
            )
        }

        return transaction
    }
}
