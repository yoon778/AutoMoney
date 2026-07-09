package com.choiyoonseo.automoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.choiyoonseo.automoney.ui.AppRoot
import com.choiyoonseo.automoney.ui.theme.AutoMoneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as AutoMoneyApplication).container
        setContent {
            AutoMoneyTheme(darkTheme = isSystemInDarkTheme()) {
                AppRoot(
                    recordWalletTopupUsageUseCase = appContainer.recordWalletTopupUsageUseCase,
                    moneyRepository = appContainer.repository,
                    assetRepository = appContainer.assetRepository,
                    notificationIngestionUseCase = appContainer.notificationIngestionUseCase,
                    saveManualTransactionUseCase = appContainer.saveManualTransactionUseCase,
                    editTransactionUseCase = appContainer.editTransactionUseCase,
                    notificationDiagnosticsStore = appContainer.notificationDiagnosticsStore,
                    walletTopupNoticeStore = appContainer.walletTopupNoticeStore,
                    notificationOnboardingStore = appContainer.notificationOnboardingStore,
                    runSampleNotificationScenarioUseCase = appContainer.runSampleNotificationScenarioUseCase,
                    resolveReviewUseCase = appContainer.resolveReviewUseCase,
                    resolveAccountTransferUseCase = appContainer.resolveAccountTransferUseCase
                )
            }
        }
    }
}
