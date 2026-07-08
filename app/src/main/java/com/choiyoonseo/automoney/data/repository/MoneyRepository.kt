package com.choiyoonseo.automoney.data.repository

import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface MoneyRepository {
    suspend fun recentNotificationTransactions(limit: Int): List<MoneyTransaction>
    fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>>
    fun observeOpenReviewCount(): Flow<Int>
    fun observeOpenReviewItems(): Flow<List<OpenReviewItem>>
    suspend fun enabledRules(): List<Rule>
    suspend fun saveTransaction(transaction: MoneyTransaction): Long
    suspend fun saveTransactionWithReview(transaction: MoneyTransaction, reason: ReviewReason): Long {
        val transactionId = saveTransaction(transaction)
        createReviewItem(transactionId, reason)
        return transactionId
    }
    suspend fun updateTransaction(transaction: MoneyTransaction)
    suspend fun deleteTransaction(transactionId: Long)
    suspend fun createReviewItem(transactionId: Long, reason: ReviewReason)
    suspend fun resolveReviewItem(reviewItemId: Long)
    suspend fun saveRule(rule: Rule): Long
}
