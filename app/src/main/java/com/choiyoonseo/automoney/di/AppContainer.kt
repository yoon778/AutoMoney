package com.choiyoonseo.automoney.di

import android.content.Context
import androidx.room.Room
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.repository.RoomAssetRepository
import com.choiyoonseo.automoney.data.repository.RoomMoneyRepository
import com.choiyoonseo.automoney.domain.manual.SaveManualTransactionUseCase
import com.choiyoonseo.automoney.domain.parser.BankAccountHintExtractor
import com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParser
import com.choiyoonseo.automoney.domain.parser.NotificationParserRouter
import com.choiyoonseo.automoney.domain.parser.TossNotificationParser
import com.choiyoonseo.automoney.domain.review.RecordWalletTopupUsageUseCase
import com.choiyoonseo.automoney.domain.review.ResolveAccountTransferUseCase
import com.choiyoonseo.automoney.domain.review.ResolveReviewUseCase
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.notification.NotificationDiagnosticsStore
import com.choiyoonseo.automoney.notification.NotificationIngestionUseCase
import com.choiyoonseo.automoney.notification.RunSampleNotificationScenarioUseCase
import com.choiyoonseo.automoney.ui.onboarding.SharedPreferencesNotificationOnboardingStore
import com.choiyoonseo.automoney.ui.transactions.SharedPreferencesWalletTopupNoticeStore

class AppContainer(context: Context) {
    val notificationDiagnosticsStore = NotificationDiagnosticsStore(context.applicationContext)
    val walletTopupNoticeStore = SharedPreferencesWalletTopupNoticeStore(context.applicationContext)
    val notificationOnboardingStore = SharedPreferencesNotificationOnboardingStore(context.applicationContext)

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "auto_money.db"
    ).addMigrations(
        AppDatabase.MIGRATION_1_2,
        AppDatabase.MIGRATION_2_3,
        AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5,
        AppDatabase.MIGRATION_5_6
    ).build()

    val repository = RoomMoneyRepository(database)
    val assetRepository = RoomAssetRepository(database)

    val recordWalletTopupUsageUseCase = RecordWalletTopupUsageUseCase(repository)

    val saveManualTransactionUseCase = SaveManualTransactionUseCase(repository)

    val editTransactionUseCase = EditTransactionUseCase(repository)

    val resolveReviewUseCase = ResolveReviewUseCase(repository)

    val resolveAccountTransferUseCase = ResolveAccountTransferUseCase(repository)

    private val bankAccountHintExtractor = BankAccountHintExtractor()

    val notificationIngestionUseCase = NotificationIngestionUseCase(
        parser = NotificationParserRouter(
            listOf(
                TossNotificationParser(bankAccountHintExtractor),
                CommonFinanceNotificationParser(bankAccountHintExtractor)
            )
        ),
        categorizationEngine = CategorizationEngine(),
        duplicateDetector = DuplicateDetector(),
        repository = repository
    )

    val runSampleNotificationScenarioUseCase = RunSampleNotificationScenarioUseCase(
        notificationIngestionUseCase = notificationIngestionUseCase,
        notificationDiagnosticsStore = notificationDiagnosticsStore
    )
}
