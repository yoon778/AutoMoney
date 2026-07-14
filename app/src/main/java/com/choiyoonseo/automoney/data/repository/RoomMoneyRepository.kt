package com.choiyoonseo.automoney.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.local.entity.ReviewItemEntity
import com.choiyoonseo.automoney.data.local.entity.ReviewItemWithTransaction
import com.choiyoonseo.automoney.data.local.entity.RuleEntity
import com.choiyoonseo.automoney.data.local.entity.TransactionEntity
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth

class RoomMoneyRepository(
    private val db: AppDatabase
) : MoneyRepository {
    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> {
        return db.transactionDao().recentNotificationTransactions(limit).map { it.toDomain() }
    }

    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> {
        return db.transactionDao()
            .observeTransactionsForMonth(month.toString())
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeAllTransactions(): Flow<List<MoneyTransaction>> =
        db.transactionDao().observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeOpenReviewCount(): Flow<Int> {
        return db.reviewItemDao().observeOpenItemCount()
    }

    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> {
        return db.reviewItemDao()
            .observeOpenItemsWithTransactions()
            .map { items -> items.map { it.toDomain() } }
    }

    override suspend fun enabledRules(): List<Rule> {
        return db.ruleDao().enabledRules().map { it.toDomain() }
    }

    override suspend fun saveNotificationTransaction(
        transaction: MoneyTransaction,
        accountHint: BankAccountHint?,
        reviewReason: ReviewReason?
    ): NotificationSaveResult {
        return try {
            db.withTransaction {
                val id = saveTransactionInternal(transaction.withoutAccountLink())
                if (reviewReason != null) {
                    insertReviewItem(id, reviewReason)
                }
                NotificationSaveResult(id, reviewReason)
            }
        } catch (e: SQLiteConstraintException) {
            throw duplicateNotificationExceptionOrOriginal(transaction, e)
        }
    }

    override suspend fun saveTransaction(transaction: MoneyTransaction): Long {
        return try {
            db.withTransaction {
                saveTransactionInternal(transaction.withoutAccountLink())
            }
        } catch (e: SQLiteConstraintException) {
            throw duplicateNotificationExceptionOrOriginal(transaction, e)
        }
    }

    override suspend fun saveTransactionWithReview(
        transaction: MoneyTransaction,
        reason: ReviewReason
    ): Long {
        return try {
            db.withTransaction {
                val id = saveTransactionInternal(transaction.withoutAccountLink())
                insertReviewItem(id, reason)
                id
            }
        } catch (e: SQLiteConstraintException) {
            throw duplicateNotificationExceptionOrOriginal(transaction, e)
        }
    }

    override suspend fun updateTransaction(transaction: MoneyTransaction) {
        db.withTransaction {
            updateTransactionInternal(transaction)
        }
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        db.withTransaction {
            db.transactionDao().deleteById(transactionId)
        }
    }

    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) {
        insertReviewItem(transactionId, reason)
    }

    private suspend fun saveTransactionInternal(transaction: MoneyTransaction): Long =
        db.transactionDao().insert(transaction.toEntity())

    private suspend fun insertReviewItem(transactionId: Long, reason: ReviewReason) {
        db.reviewItemDao().insert(
            ReviewItemEntity(
                transactionId = transactionId,
                reason = reason,
                createdAt = Instant.now(),
                resolvedAt = null
            )
        )
    }

    private suspend fun duplicateNotificationExceptionOrOriginal(
        transaction: MoneyTransaction,
        exception: SQLiteConstraintException
    ): Throwable {
        val hash = transaction.sourceNotificationHash?.takeIf { it.isNotBlank() }
            ?: return exception
        return if (db.transactionDao().countBySourceNotificationHash(hash) > 0) {
            DuplicateNotificationException(hash, exception)
        } else {
            exception
        }
    }

    override suspend fun resolveReviewItem(reviewItemId: Long) {
        db.reviewItemDao().resolve(reviewItemId, Instant.now())
    }

    override suspend fun resolveReviewItemWithTransaction(
        reviewItemId: Long,
        transaction: MoneyTransaction
    ) {
        db.withTransaction {
            updateTransactionInternal(transaction)
            db.reviewItemDao().resolve(reviewItemId, Instant.now())
        }
    }

    override suspend fun resolveAccountTransferReview(
        reviewItemId: Long,
        transaction: MoneyTransaction,
        fromAccount: AssetAccount,
        toAccount: AssetAccount,
        pairedReviewItemId: Long?,
        pairedTransaction: MoneyTransaction?
    ) {
        db.withTransaction {
            updateTransactionInternal(transaction.withoutAccountLink())
            db.reviewItemDao().resolve(reviewItemId, Instant.now())
            if (pairedReviewItemId != null && pairedTransaction != null) {
                updateTransactionInternal(pairedTransaction.withoutAccountLink())
                db.reviewItemDao().resolve(pairedReviewItemId, Instant.now())
            }
        }
    }

    override suspend fun saveRule(rule: Rule): Long {
        return db.ruleDao().insert(rule.toEntity())
    }

    private suspend fun updateTransactionInternal(transaction: MoneyTransaction) {
        db.transactionDao().update(transaction.withoutAccountLink().toEntity())
    }
}

private fun MoneyTransaction.withoutAccountLink(): MoneyTransaction = copy(
    linkedAssetAccountId = null,
    balanceImpact = BalanceImpact.NONE
)

private fun MoneyTransaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        occurredAt = occurredAt,
        amountWon = amount.won,
        direction = direction,
        type = type,
        category = category,
        paymentMethod = paymentMethod,
        merchant = merchant,
        counterparty = counterparty,
        memo = memo,
        sourceApp = sourceApp,
        sourceType = sourceType.name,
        sourceNotificationHash = sourceNotificationHash,
        status = status,
        confidence = confidence,
        monthKey = monthKey.toString(),
        linkedAssetAccountId = linkedAssetAccountId,
        balanceImpact = balanceImpact,
        customCategoryId = customCategoryId,
        customCategoryName = customCategoryName,
        settlementPartyCount = settlementPartyCount ?: settlementDetails?.peopleCount,
        settlementMyShareWon = settlementMyShareWon ?: settlementDetails?.myShareWon,
        settlementParentId = settlementParentId ?: recoveryOfSettlementTransactionId,
        settlementTrackingHidden = settlementTrackingHidden || settlementDetails?.receivableHidden == true,
        budgetPlanId = budgetPlanId,
        fixedExpensePlanId = fixedExpensePlanId
    )
}

private fun TransactionEntity.toDomain(): MoneyTransaction {
    return MoneyTransaction(
        id = id,
        occurredAt = occurredAt,
        amount = MoneyAmount(amountWon),
        direction = direction,
        type = type,
        category = category,
        paymentMethod = paymentMethod,
        merchant = merchant,
        counterparty = counterparty,
        memo = memo,
        sourceApp = sourceApp,
        sourceType = SourceType.valueOf(sourceType),
        sourceNotificationHash = sourceNotificationHash,
        status = status,
        confidence = confidence,
        monthKey = YearMonth.parse(monthKey),
        linkedAssetAccountId = linkedAssetAccountId,
        balanceImpact = balanceImpact,
        customCategoryId = customCategoryId,
        customCategoryName = customCategoryName,
        settlementPartyCount = settlementPartyCount,
        settlementMyShareWon = settlementMyShareWon,
        settlementParentId = settlementParentId,
        settlementTrackingHidden = settlementTrackingHidden,
        budgetPlanId = budgetPlanId,
        fixedExpensePlanId = fixedExpensePlanId,
        settlementDetails = settlementPartyCount?.let { count ->
            settlementMyShareWon?.let { share ->
                com.choiyoonseo.automoney.domain.model.SettlementDetails(
                    peopleCount = count,
                    myShareWon = share,
                    receivableHidden = settlementTrackingHidden
                )
            }
        },
        recoveryOfSettlementTransactionId = settlementParentId
    )
}

private fun ReviewItemWithTransaction.toDomain(): OpenReviewItem {
    return OpenReviewItem(
        id = reviewItem.id,
        transaction = transaction.toDomain(),
        reason = reviewItem.reason,
        createdAt = reviewItem.createdAt
    )
}

private fun RuleEntity.toDomain(): Rule {
    return Rule(
        id = id,
        matchType = matchType,
        matchValue = matchValue,
        action = action,
        targetValue = targetValue,
        enabled = enabled
    )
}

private fun Rule.toEntity(): RuleEntity {
    return RuleEntity(
        id = id,
        matchType = matchType,
        matchValue = matchValue,
        action = action,
        targetValue = targetValue,
        enabled = enabled
    )
}
