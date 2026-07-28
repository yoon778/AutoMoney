package com.choiyoonseo.automoney.di

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.data.repository.RoomAssetRepository
import com.choiyoonseo.automoney.data.repository.RoomMoneyRepository
import com.choiyoonseo.automoney.data.repository.RoomNotificationHistoryRepository
import com.choiyoonseo.automoney.data.repository.RoomUserCategoryRepository
import com.choiyoonseo.automoney.domain.manual.SaveManualTransactionUseCase
import com.choiyoonseo.automoney.domain.manual.SaveMissedNotificationTransactionUseCase
import com.choiyoonseo.automoney.domain.parser.BankAccountHintExtractor
import com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParser
import com.choiyoonseo.automoney.domain.parser.GenericFinanceNotificationParser
import com.choiyoonseo.automoney.domain.parser.NotificationParserRouter
import com.choiyoonseo.automoney.domain.parser.SecuritiesNotificationParser
import com.choiyoonseo.automoney.domain.parser.TossNotificationParser
import com.choiyoonseo.automoney.domain.review.RecordWalletTopupUsageUseCase
import com.choiyoonseo.automoney.domain.review.ResolveAccountTransferUseCase
import com.choiyoonseo.automoney.domain.review.ResolveReviewUseCase
import com.choiyoonseo.automoney.domain.refund.LinkRefundUseCase
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector
import com.choiyoonseo.automoney.domain.settlement.LinkSettlementRepaymentUseCase
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.notification.NotificationDiagnosticsStore
import com.choiyoonseo.automoney.notification.NotificationDispatchCoordinator
import com.choiyoonseo.automoney.notification.NotificationIngestionUseCase
import com.choiyoonseo.automoney.notification.NotificationHistoryRecorder
import com.choiyoonseo.automoney.notification.NotificationIngestionFeedbackNotifier
import com.choiyoonseo.automoney.notification.NotificationSnapshotBuilder
import com.choiyoonseo.automoney.notification.NotificationSourceSettingsService
import com.choiyoonseo.automoney.notification.RunSampleNotificationScenarioUseCase
import com.choiyoonseo.automoney.notification.SharedPreferencesNotificationAppAccessStore
import com.choiyoonseo.automoney.notification.SharedPreferencesObservedNotificationSourceStore
import com.choiyoonseo.automoney.ui.onboarding.SharedPreferencesNotificationOnboardingStore
import com.choiyoonseo.automoney.ui.transactions.SharedPreferencesWalletTopupNoticeStore

class AppContainer(context: Context) {
    val notificationAppAccessStore =
        SharedPreferencesNotificationAppAccessStore(context.applicationContext)
    val observedNotificationSourceStore =
        SharedPreferencesObservedNotificationSourceStore(context.applicationContext)
    val notificationDispatchCoordinator = NotificationDispatchCoordinator(
        accessFor = notificationAppAccessStore::accessFor,
        recordObserved = observedNotificationSourceStore::record,
        snapshotBuilder = NotificationSnapshotBuilder(),
        resolveInstalledLabel = context.applicationContext::installedApplicationLabel
    )
    val notificationDiagnosticsStore = NotificationDiagnosticsStore(context.applicationContext)
    val notificationIngestionFeedbackNotifier =
        NotificationIngestionFeedbackNotifier(context.applicationContext)
    val notificationSourceSettingsService = NotificationSourceSettingsService(
        accessStore = notificationAppAccessStore,
        observedStore = observedNotificationSourceStore,
        resolveInstalledLabel = context.applicationContext::installedApplicationLabel,
        clearDiagnosticIfPackage = notificationDiagnosticsStore::clearIfPackage
    )
    val walletTopupNoticeStore = SharedPreferencesWalletTopupNoticeStore(context.applicationContext)
    val notificationOnboardingStore = SharedPreferencesNotificationOnboardingStore(context.applicationContext)

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "auto_money.db"
    ).addMigrations(*AppDatabase.MIGRATIONS.toTypedArray()).build()

    val repository = RoomMoneyRepository(database)
    val assetRepository = RoomAssetRepository(database)
    val userCategoryRepository = RoomUserCategoryRepository(database)
    val notificationHistoryRepository = RoomNotificationHistoryRepository(database)
    val notificationHistoryRecorder = NotificationHistoryRecorder(notificationHistoryRepository)

    val recordWalletTopupUsageUseCase = RecordWalletTopupUsageUseCase(repository)

    val saveManualTransactionUseCase = SaveManualTransactionUseCase(repository, userCategoryRepository)

    val saveMissedNotificationTransactionUseCase = SaveMissedNotificationTransactionUseCase(
        saveManual = saveManualTransactionUseCase,
        repository = repository
    )

    val editTransactionUseCase = EditTransactionUseCase(repository, userCategoryRepository)

    val resolveReviewUseCase = ResolveReviewUseCase(repository)

    val resolveAccountTransferUseCase = ResolveAccountTransferUseCase(repository)

    val linkSettlementRepaymentUseCase = LinkSettlementRepaymentUseCase(repository)

    val linkRefundUseCase = LinkRefundUseCase(repository)

    private val bankAccountHintExtractor = BankAccountHintExtractor()

    val notificationIngestionUseCase = NotificationIngestionUseCase(
        parser = NotificationParserRouter(
            listOf(
                TossNotificationParser(bankAccountHintExtractor),
                SecuritiesNotificationParser(),
                CommonFinanceNotificationParser(bankAccountHintExtractor)
            )
        ),
        genericParser = GenericFinanceNotificationParser(),
        categorizationEngine = CategorizationEngine(),
        duplicateDetector = DuplicateDetector(),
        repository = repository,
        refundAutoLink = linkRefundUseCase::autoLink
    )

    val runSampleNotificationScenarioUseCase = RunSampleNotificationScenarioUseCase(
        notificationIngestionUseCase = notificationIngestionUseCase,
        notificationDiagnosticsStore = notificationDiagnosticsStore
    )
}

private fun Context.installedApplicationLabel(packageName: String): String? =
    try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
            .toString()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
