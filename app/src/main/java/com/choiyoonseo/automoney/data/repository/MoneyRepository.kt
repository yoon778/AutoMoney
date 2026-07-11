package com.choiyoonseo.automoney.data.repository

import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.YearMonth

data class NotificationSaveResult(
    val transactionId: Long,
    val reviewReason: ReviewReason?
)

interface MoneyRepository {
    suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction>
    fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>>
    fun observeAllTransactions(): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    fun observeOpenReviewCount(): Flow<Int>
    fun observeOpenReviewItems(): Flow<List<OpenReviewItem>>
    suspend fun enabledRules(): List<Rule>
    suspend fun saveTransaction(transaction: MoneyTransaction): Long
    suspend fun saveTransactionWithReview(transaction: MoneyTransaction, reason: ReviewReason): Long {
        val transactionId = saveTransaction(transaction)
        createReviewItem(transactionId, reason)
        return transactionId
    }
    suspend fun saveNotificationTransaction(
        transaction: MoneyTransaction,
        accountHint: BankAccountHint?,
        reviewReason: ReviewReason?
    ): NotificationSaveResult {
        val explicitNone = transaction.copy(
            linkedAssetAccountId = null,
            balanceImpact = BalanceImpact.NONE
        )
        val transactionId = if (reviewReason == null) {
            saveTransaction(explicitNone)
        } else {
            saveTransactionWithReview(explicitNone, reviewReason)
        }
        return NotificationSaveResult(transactionId, reviewReason)
    }
    suspend fun updateTransaction(transaction: MoneyTransaction)
    suspend fun deleteTransaction(transactionId: Long)
    suspend fun createReviewItem(transactionId: Long, reason: ReviewReason)
    suspend fun resolveReviewItem(reviewItemId: Long)
    suspend fun resolveReviewItemWithTransaction(reviewItemId: Long, transaction: MoneyTransaction) {
        updateTransaction(transaction)
        resolveReviewItem(reviewItemId)
    }
    suspend fun resolveAccountTransferReview(
        reviewItemId: Long,
        transaction: MoneyTransaction,
        fromAccount: AssetAccount,
        toAccount: AssetAccount,
        pairedReviewItemId: Long? = null,
        pairedTransaction: MoneyTransaction? = null
    ) {
        resolveReviewItemWithTransaction(reviewItemId, transaction)
        if (pairedReviewItemId != null && pairedTransaction != null) {
            resolveReviewItemWithTransaction(pairedReviewItemId, pairedTransaction)
        }
    }
    suspend fun saveRule(rule: Rule): Long
}
