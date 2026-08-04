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
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.refund.RefundLinkMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth

class RoomMoneyRepository(
    private val db: AppDatabase
) : MoneyRepository {
    private val refundLinkMatcher = RefundLinkMatcher()

    override suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction> {
        return db.transactionDao().recentNotificationTransactions(limit).map { it.toDomain() }
    }

    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> {
        return db.transactionDao()
            .observeTransactionsForMonth(month.toString())
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeSettlementTrackingTransactions(): Flow<List<MoneyTransaction>> =
        db.transactionDao().observeSettlementTrackingTransactions()
            .map { entities -> entities.map { it.toDomain() } }

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
                saveTransactionInternal(transaction)
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
                val id = saveTransactionInternal(transaction)
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
            val linkedRefunds = db.transactionDao().refundsForParent(transactionId)
            db.transactionDao().deleteById(transactionId)
            linkedRefunds.forEach { refund ->
                db.transactionDao().updateRefundLink(
                    refundId = refund.id,
                    parentId = null,
                    status = TransactionStatus.NEEDS_REVIEW
                )
                insertReviewItem(refund.id, ReviewReason.REFUND_OR_CANCEL)
            }
        }
    }

    override suspend fun saveManualTransactionFromHistory(
        historyId: Long,
        transaction: MoneyTransaction
    ): Long {
        require(historyId > 0)
        return db.withTransaction {
            val transactionId = saveTransactionInternal(transaction)
            check(
                db.notificationHistoryDao().markResolvedManually(historyId, transactionId) == 1
            ) { "Notification history is not recordable: $historyId" }
            transactionId
        }
    }

    override suspend fun findTransaction(id: Long): MoneyTransaction? =
        db.transactionDao().byId(id)?.toDomain()

    override suspend fun refundMatchWindow(
        sourceApp: String,
        from: Instant,
        to: Instant
    ): List<MoneyTransaction> =
        db.transactionDao().refundMatchWindow(sourceApp, from, to).map { it.toDomain() }

    override suspend fun linkRefundAndResolve(
        refundId: Long,
        paymentId: Long,
        userConfirmed: Boolean
    ) {
        require(refundId > 0 && paymentId > 0 && refundId != paymentId)
        db.withTransaction {
            val refund = requireNotNull(db.transactionDao().byId(refundId)) {
                "Refund transaction not found: $refundId"
            }.toDomain()
            val payment = requireNotNull(db.transactionDao().byId(paymentId)) {
                "Payment transaction not found: $paymentId"
            }.toDomain()
            require(refund.type == TransactionType.REFUND) { "Refund transaction required" }
            val sourceApp = requireNotNull(refund.sourceApp) { "Refund source app required" }
            val window = db.transactionDao().refundMatchWindow(
                sourceApp = sourceApp,
                from = refund.occurredAt.minusSeconds(REFUND_MATCH_WINDOW_SECONDS),
                to = refund.occurredAt
            ).map { it.toDomain() }
            val linkedRefunds = db.transactionDao().refundsForParent(paymentId)
                .filter { it.id != refundId }
                .map { it.toDomain() }
            val eligibleIds = refundLinkMatcher.eligibleCandidates(
                refund = refund,
                candidates = window.filter { it.type != TransactionType.REFUND },
                linkedRefunds = linkedRefunds
            ).mapTo(mutableSetOf(), MoneyTransaction::id)
            require(payment.id in eligibleIds) { "Payment is not eligible for this refund" }

            db.transactionDao().updateRefundLink(
                refundId = refundId,
                parentId = paymentId,
                status = if (userConfirmed) {
                    TransactionStatus.USER_EDITED
                } else {
                    TransactionStatus.AUTO_CONFIRMED
                }
            )
            db.reviewItemDao().resolveByTransactionId(refundId, Instant.now())
        }
    }

    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) {
        insertReviewItem(transactionId, reason)
    }

    private suspend fun saveTransactionInternal(transaction: MoneyTransaction): Long =
        db.transactionDao().insert(transaction.toEntity())

    private suspend fun insertReviewItem(transactionId: Long, reason: ReviewReason) {
        val createdAt = Instant.now()
        if (
            db.reviewItemDao().reopenByTransactionId(
                transactionId = transactionId,
                reason = reason,
                createdAt = createdAt
            ) == 0
        ) {
            db.reviewItemDao().insert(
                ReviewItemEntity(
                    transactionId = transactionId,
                    reason = reason,
                    createdAt = createdAt,
                    resolvedAt = null
                )
            )
        }
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
        db.transactionDao().update(transaction.toEntity())
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
        settlementPartyCount = settlementPartyCount,
        settlementMyShareWon = settlementMyShareWon,
        settlementParentId = settlementParentId,
        settlementTrackingHidden = settlementTrackingHidden,
        budgetPlanId = budgetPlanId,
        fixedExpensePlanId = fixedExpensePlanId,
        refundParentTransactionId = refundParentTransactionId
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
        refundParentTransactionId = refundParentTransactionId
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

private const val REFUND_MATCH_WINDOW_SECONDS = 30L * 24 * 60 * 60
