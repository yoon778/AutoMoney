package com.choiyoonseo.automoney.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.local.entity.AssetAccountEntity
import com.choiyoonseo.automoney.data.local.entity.ReviewItemEntity
import com.choiyoonseo.automoney.data.local.entity.ReviewItemWithTransaction
import com.choiyoonseo.automoney.data.local.entity.RuleEntity
import com.choiyoonseo.automoney.data.local.entity.TransactionEntity
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.assets.applyTransactionBalance
import com.choiyoonseo.automoney.domain.assets.decideAccountBalance
import com.choiyoonseo.automoney.domain.assets.needsAccountMatchReview
import com.choiyoonseo.automoney.domain.assets.removeTransactionBalance
import com.choiyoonseo.automoney.domain.assets.replaceTransactionBalance
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionStatus
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
                val currentAccounts = db.assetDao().accountsOnce().map { it.toDomain() }
                val decision = decideAccountBalance(currentAccounts, accountHint, transaction.amount.won)
                val effectiveReason = decision.reviewReason ?: reviewReason
                val transactionToSave = transaction.copy(
                    linkedAssetAccountId = decision.linkedAssetAccountId,
                    balanceImpact = decision.balanceImpact,
                    status = if (effectiveReason != null) {
                        TransactionStatus.NEEDS_REVIEW
                    } else {
                        transaction.status
                    }
                )
                val id = saveTransactionInternal(
                    transaction = transactionToSave,
                    currentAccounts = currentAccounts,
                    needsAccountReview = false
                ).id
                if (effectiveReason != null) {
                    insertReviewItem(id, effectiveReason)
                }
                NotificationSaveResult(id, effectiveReason)
            }
        } catch (e: SQLiteConstraintException) {
            throw duplicateNotificationExceptionOrOriginal(transaction, e)
        }
    }

    override suspend fun saveTransaction(transaction: MoneyTransaction): Long {
        return try {
            db.withTransaction {
                val currentAccounts = db.assetDao().accountsOnce().map { it.toDomain() }
                val needsAccountReview = transaction.status != TransactionStatus.NEEDS_REVIEW &&
                    needsLegacyAccountReview(currentAccounts, transaction)
                val transactionToSave = transaction.withAccountReviewStatus(needsAccountReview)
                val result = saveTransactionInternal(
                    transaction = transactionToSave,
                    currentAccounts = currentAccounts,
                    needsAccountReview = needsAccountReview
                )
                if (result.needsAccountReview) {
                    insertReviewItem(result.id, ReviewReason.ACCOUNT_UNMATCHED)
                }
                result.id
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
                val currentAccounts = db.assetDao().accountsOnce().map { it.toDomain() }
                val needsAccountReview = needsLegacyAccountReview(currentAccounts, transaction)
                val transactionToSave = transaction.withAccountReviewStatus(needsAccountReview)
                val effectiveReason = reason.withAccountReviewPriority(needsAccountReview)
                val id = saveTransactionInternal(
                    transaction = transactionToSave,
                    currentAccounts = currentAccounts,
                    needsAccountReview = needsAccountReview
                ).id
                insertReviewItem(id, effectiveReason)
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
            val previous = db.transactionDao().transactionById(transactionId)?.toDomain()
            db.transactionDao().deleteById(transactionId)
            if (previous != null) {
                syncAssetAccounts { accounts ->
                    removeTransactionBalance(accounts, previous)
                }
            }
        }
    }

    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) {
        insertReviewItem(transactionId, reason)
    }

    private suspend fun saveTransactionInternal(
        transaction: MoneyTransaction,
        currentAccounts: List<AssetAccount>? = null,
        needsAccountReview: Boolean? = null
    ): SaveTransactionResult {
        val id = db.transactionDao().insert(transaction.toEntity())
        val transactionWithId = transaction.copy(id = id)
        val accounts = currentAccounts ?: db.assetDao().accountsOnce().map { it.toDomain() }
        val shouldReviewAccount = needsAccountReview ?: needsLegacyAccountReview(accounts, transactionWithId)
        syncAssetAccounts(accounts) { accounts ->
            applyTransactionBalance(accounts, transactionWithId)
        }
        return SaveTransactionResult(id = id, needsAccountReview = shouldReviewAccount)
    }

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
            db.assetDao().insertAccount(fromAccount.toEntity())
            db.assetDao().insertAccount(toAccount.toEntity())
            updateTransactionInternal(transaction)
            db.reviewItemDao().resolve(reviewItemId, Instant.now())
            if (pairedReviewItemId != null && pairedTransaction != null) {
                updateTransactionInternal(pairedTransaction)
                db.reviewItemDao().resolve(pairedReviewItemId, Instant.now())
            }
        }
    }

    override suspend fun saveRule(rule: Rule): Long {
        return db.ruleDao().insert(rule.toEntity())
    }

    private suspend fun updateTransactionInternal(transaction: MoneyTransaction) {
        val previous = transaction.id.takeIf { it > 0 }?.let { id ->
            db.transactionDao().transactionById(id)?.toDomain()
        }
        db.transactionDao().update(transaction.toEntity())
        syncAssetAccounts { accounts ->
            replaceTransactionBalance(accounts, oldTransaction = previous, newTransaction = transaction)
        }
    }

    private suspend fun syncAssetAccounts(
        transform: (List<AssetAccount>) -> List<AssetAccount>
    ) {
        val current = db.assetDao().accountsOnce().map { it.toDomain() }
        syncAssetAccounts(current, transform)
    }

    private suspend fun syncAssetAccounts(
        current: List<AssetAccount>,
        transform: (List<AssetAccount>) -> List<AssetAccount>
    ) {
        val updated = transform(current)
        updated.forEachIndexed { index, account ->
            if (account != current[index]) {
                db.assetDao().insertAccount(account.toEntity())
            }
        }
    }
}

private data class SaveTransactionResult(
    val id: Long,
    val needsAccountReview: Boolean
)

private fun needsLegacyAccountReview(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction
): Boolean =
    transaction.balanceImpact == null &&
        transaction.sourceType == SourceType.NOTIFICATION &&
        needsAccountMatchReview(accounts, transaction.asResolvedForAccountReview())

private fun MoneyTransaction.asResolvedForAccountReview(): MoneyTransaction =
    if (status == TransactionStatus.NEEDS_REVIEW) {
        copy(status = TransactionStatus.USER_EDITED)
    } else {
        this
    }

private fun MoneyTransaction.withAccountReviewStatus(needsAccountReview: Boolean): MoneyTransaction =
    if (needsAccountReview) copy(status = TransactionStatus.NEEDS_REVIEW) else this

private fun ReviewReason.withAccountReviewPriority(needsAccountReview: Boolean): ReviewReason =
    if (needsAccountReview && isGenericReviewReason()) ReviewReason.ACCOUNT_UNMATCHED else this

private fun ReviewReason.isGenericReviewReason(): Boolean =
    this == ReviewReason.DUPLICATE_SUSPECTED ||
        this == ReviewReason.LOW_CONFIDENCE_CATEGORY ||
        this == ReviewReason.PAYMENT_GATEWAY

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
        settlementTrackingHidden = settlementTrackingHidden || settlementDetails?.receivableHidden == true
    )
}

private fun AssetAccountEntity.toDomain(): AssetAccount =
    AssetAccount(
        id = id,
        name = name,
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = bankProvider,
        accountLast4 = accountLast4
    )

private fun AssetAccount.toEntity(): AssetAccountEntity =
    AssetAccountEntity(
        id = id,
        name = name,
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = bankProvider,
        accountLast4 = accountLast4
    )

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
