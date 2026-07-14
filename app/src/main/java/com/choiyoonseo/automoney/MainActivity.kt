package com.choiyoonseo.automoney

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import com.choiyoonseo.automoney.ui.AppRoot
import com.choiyoonseo.automoney.ui.theme.AutoMoneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as AutoMoneyApplication).container
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
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
                    linkSettlementRepaymentUseCase = appContainer.linkSettlementRepaymentUseCase,
                    userCategoryRepository = appContainer.userCategoryRepository,
                    notificationSourceSettingsService = appContainer.notificationSourceSettingsService
                )
            }
        }
    }
}

private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
