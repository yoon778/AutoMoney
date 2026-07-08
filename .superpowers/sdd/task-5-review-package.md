# Task 5 Review Package

Repository note: no HEAD commit exists; package contains current contents of task-owned files.

## app\src\main\java\com\choiyoonseo\automoney\notification\NotificationIngestionUseCase.kt
```kotlin
package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.NotificationParser
import com.choiyoonseo.automoney.domain.parser.ParseResult
import com.choiyoonseo.automoney.domain.parser.TransactionDraft
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDecision
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector

class NotificationIngestionUseCase(
    private val parser: NotificationParser,
    private val categorizationEngine: CategorizationEngine,
    private val duplicateDetector: DuplicateDetector,
    private val repository: MoneyRepository
) {
    suspend fun ingest(snapshot: NotificationSnapshot): IngestionResult {
        val parsed = parser.parse(snapshot)
        if (parsed !is ParseResult.Parsed) return IngestionResult.Ignored

        val withRules = categorizationEngine.applyRules(parsed.draft, repository.enabledRules())
        val duplicateDecision = duplicateDetector.detect(
            candidate = withRules,
            existing = repository.recentNotificationTransactions(limit = 50)
        )

        if (duplicateDecision == DuplicateDecision.DUPLICATE) {
            return IngestionResult.Duplicate
        }

        val finalDraft = when (duplicateDecision) {
            DuplicateDecision.SUSPECTED -> withRules.copy(
                status = TransactionStatus.NEEDS_REVIEW,
                reviewReason = ReviewReason.DUPLICATE_SUSPECTED
            )
            DuplicateDecision.UNIQUE,
            DuplicateDecision.DUPLICATE -> withRules
        }

        val id = repository.saveTransaction(finalDraft.toDomain())
        if (finalDraft.status == TransactionStatus.NEEDS_REVIEW && finalDraft.reviewReason != null) {
            repository.createReviewItem(id, finalDraft.reviewReason)
        }

        return IngestionResult.Saved
    }
}

enum class IngestionResult {
    Saved,
    Ignored,
    Duplicate
}

private fun TransactionDraft.toDomain(): MoneyTransaction {
    return MoneyTransaction(
        occurredAt = occurredAt,
        amount = amount,
        direction = direction,
        type = type,
        category = category,
        paymentMethod = paymentMethod,
        merchant = merchant,
        counterparty = counterparty,
        memo = memo,
        sourceApp = sourceApp,
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = sourceNotificationHash,
        status = status,
        confidence = confidence,
        monthKey = monthKey
    )
}
```

## app\src\main\java\com\choiyoonseo\automoney\di\AppContainer.kt
```kotlin
package com.choiyoonseo.automoney.di

import android.content.Context
import androidx.room.Room
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.repository.RoomMoneyRepository
import com.choiyoonseo.automoney.domain.manual.SaveManualTransactionUseCase
import com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParser
import com.choiyoonseo.automoney.domain.parser.NotificationParserRouter
import com.choiyoonseo.automoney.domain.parser.TossNotificationParser
import com.choiyoonseo.automoney.domain.review.RecordWalletTopupUsageUseCase
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.notification.NotificationDiagnosticsStore
import com.choiyoonseo.automoney.notification.NotificationIngestionUseCase

class AppContainer(context: Context) {
    val notificationDiagnosticsStore = NotificationDiagnosticsStore(context.applicationContext)

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "auto_money.db"
    ).build()

    val repository = RoomMoneyRepository(database)

    val recordWalletTopupUsageUseCase = RecordWalletTopupUsageUseCase(repository)

    val saveManualTransactionUseCase = SaveManualTransactionUseCase(repository)

    val editTransactionUseCase = EditTransactionUseCase(repository)

    val notificationIngestionUseCase = NotificationIngestionUseCase(
        parser = NotificationParserRouter(
            listOf(
                TossNotificationParser(),
                CommonFinanceNotificationParser()
            )
        ),
        categorizationEngine = CategorizationEngine(),
        duplicateDetector = DuplicateDetector(),
        repository = repository
    )
}
```

