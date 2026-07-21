package com.choiyoonseo.automoney.domain.manual

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.Rule
import com.choiyoonseo.automoney.domain.model.SourceType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveMissedNotificationTransactionUseCaseTest {
    @Test
    fun savesManualTransactionThenLinksHistory() = runTest {
        val repository = MissedNotificationRepository(mutableSetOf(7))
        val useCase = useCase(repository)

        val id = useCase.save(7, ManualEntryType.EXPENSE, 6_000, "식비", "스타벅스", occurredAt)

        assertThat(id).isEqualTo(42)
        assertThat(repository.manualHistoryLinks).containsExactly(7L to 42L)
        assertThat(repository.savedTransactions.single().sourceType).isEqualTo(SourceType.MANUAL)
    }

    @Test
    fun nonRecordableHistoryDoesNotSaveTransaction() = runTest {
        val repository = MissedNotificationRepository(mutableSetOf())
        val useCase = useCase(repository)

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                useCase.save(7, ManualEntryType.EXPENSE, 6_000, "식비", "스타벅스", occurredAt)
            }
        }

        assertThat(repository.savedTransactions).isEmpty()
    }

    private fun useCase(repository: MoneyRepository) = SaveMissedNotificationTransactionUseCase(
        saveManual = SaveManualTransactionUseCase(repository),
        repository = repository
    )

    private companion object {
        val occurredAt: Instant = Instant.parse("2026-07-21T01:00:00Z")
    }
}

private class MissedNotificationRepository(
    private val recordableHistoryIds: MutableSet<Long>
) : MoneyRepository {
    val savedTransactions = mutableListOf<MoneyTransaction>()
    val manualHistoryLinks = mutableListOf<Pair<Long, Long>>()

    override suspend fun saveManualTransactionFromHistory(
        historyId: Long,
        transaction: MoneyTransaction
    ): Long {
        check(historyId in recordableHistoryIds)
        savedTransactions += transaction
        manualHistoryLinks += historyId to 42L
        return 42
    }
    override suspend fun recentNotificationTransactions(limit: Int) = emptyList<MoneyTransaction>()
    override fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>> = flowOf(emptyList())
    override fun observeOpenReviewCount(): Flow<Int> = flowOf(0)
    override fun observeOpenReviewItems(): Flow<List<OpenReviewItem>> = flowOf(emptyList())
    override suspend fun enabledRules(): List<Rule> = emptyList()
    override suspend fun saveTransaction(transaction: MoneyTransaction): Long = 42
    override suspend fun updateTransaction(transaction: MoneyTransaction) = Unit
    override suspend fun deleteTransaction(transactionId: Long) = Unit
    override suspend fun createReviewItem(transactionId: Long, reason: ReviewReason) = Unit
    override suspend fun resolveReviewItem(reviewItemId: Long) = Unit
    override suspend fun saveRule(rule: Rule): Long = 1
}
