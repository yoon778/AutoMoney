package com.choiyoonseo.automoney.domain.manual

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import java.time.Instant

class SaveMissedNotificationTransactionUseCase(
    private val saveManual: SaveManualTransactionUseCase,
    private val repository: MoneyRepository
) {
    suspend fun save(
        historyId: Long,
        type: ManualEntryType,
        amountWon: Long,
        categoryText: String,
        memo: String,
        occurredAt: Instant
    ): Long {
        require(historyId > 0) { "Notification history ID must be positive" }
        val transaction = saveManual.createTransaction(
            type = type,
            amountWon = amountWon,
            categoryText = categoryText,
            memo = memo,
            occurredAt = occurredAt
        )
        return repository.saveManualTransactionFromHistory(historyId, transaction)
    }
}
