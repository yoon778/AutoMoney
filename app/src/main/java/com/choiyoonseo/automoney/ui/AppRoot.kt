package com.choiyoonseo.automoney.ui

import android.content.Intent
import android.provider.Settings as AndroidSettings
import com.choiyoonseo.automoney.BuildConfig
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.choiyoonseo.automoney.data.repository.AssetRepository
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.manual.SaveManualTransactionUseCase
import com.choiyoonseo.automoney.domain.review.RecordWalletTopupUsageUseCase
import com.choiyoonseo.automoney.domain.review.ResolveAccountTransferUseCase
import com.choiyoonseo.automoney.domain.review.ResolveReviewUseCase
import com.choiyoonseo.automoney.domain.settlement.LinkSettlementRepaymentUseCase
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.notification.LastNotificationDiagnostic
import com.choiyoonseo.automoney.notification.NotificationAccessChecker
import com.choiyoonseo.automoney.notification.NotificationDiagnosticsStore
import com.choiyoonseo.automoney.notification.NotificationIngestionUseCase
import com.choiyoonseo.automoney.notification.RunSampleNotificationScenarioUseCase
import com.choiyoonseo.automoney.notification.sampleNotificationScenarios
import com.choiyoonseo.automoney.ui.assets.AssetsScreen
import com.choiyoonseo.automoney.ui.home.HomeScreen
import com.choiyoonseo.automoney.ui.onboarding.NotificationOnboardingStore
import com.choiyoonseo.automoney.ui.onboarding.shouldShowNotificationOnboarding
import com.choiyoonseo.automoney.ui.report.MonthlyReportScreen
import com.choiyoonseo.automoney.ui.review.ReviewScreen
import com.choiyoonseo.automoney.ui.settings.SettingsScreen
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import com.choiyoonseo.automoney.ui.transactions.TransactionsScreen
import com.choiyoonseo.automoney.ui.transactions.WalletTopupNoticeStore
import kotlinx.coroutines.launch

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Filled.Home),
    TRANSACTIONS("거래", Icons.AutoMirrored.Filled.List),
    REVIEW("검토", Icons.Filled.CheckCircle),
    ASSETS("자산", Icons.Filled.AccountBalance),
    REPORT("보고서", Icons.Filled.BarChart),
    SETTINGS("설정", Icons.Filled.Settings)
}

@Composable
fun AppRoot(
    recordWalletTopupUsageUseCase: RecordWalletTopupUsageUseCase? = null,
    moneyRepository: MoneyRepository? = null,
    assetRepository: AssetRepository? = null,
    notificationIngestionUseCase: NotificationIngestionUseCase? = null,
    saveManualTransactionUseCase: SaveManualTransactionUseCase? = null,
    editTransactionUseCase: EditTransactionUseCase? = null,
    notificationDiagnosticsStore: NotificationDiagnosticsStore? = null,
    walletTopupNoticeStore: WalletTopupNoticeStore? = null,
    notificationOnboardingStore: NotificationOnboardingStore? = null,
    runSampleNotificationScenarioUseCase: RunSampleNotificationScenarioUseCase? = null,
    resolveReviewUseCase: ResolveReviewUseCase? = null,
    resolveAccountTransferUseCase: ResolveAccountTransferUseCase? = null,
    linkSettlementRepaymentUseCase: LinkSettlementRepaymentUseCase? = null,
    userCategoryRepository: UserCategoryRepository? = null
) {
    val colors = MoneyTheme.colors
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val openNotificationSettings = {
        context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    val notificationAccessChecker = remember(context) {
        NotificationAccessChecker(context.applicationContext)
    }
    var notificationAccessEnabled by remember {
        mutableStateOf(notificationAccessChecker.isNotificationAccessEnabled())
    }
    var lastNotificationDiagnostic by remember {
        mutableStateOf<LastNotificationDiagnostic?>(notificationDiagnosticsStore?.load())
    }
    var notificationOnboardingDismissed by remember(notificationOnboardingStore) {
        mutableStateOf(notificationOnboardingStore?.hasDismissed() == true)
    }
    val showNotificationOnboarding =
        notificationOnboardingStore != null &&
            shouldShowNotificationOnboarding(
                notificationAccessEnabled = notificationAccessEnabled,
                hasDismissed = notificationOnboardingDismissed
            )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationAccessEnabled = notificationAccessChecker.isNotificationAccessEnabled()
        lastNotificationDiagnostic = notificationDiagnosticsStore?.load()
    }

    Scaffold(
        containerColor = colors.canvas,
        bottomBar = {
            NavigationBar(containerColor = colors.surface) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.primary,
                            selectedTextColor = colors.primary,
                            indicatorColor = colors.primary.copy(alpha = 0.12f),
                            unselectedIconColor = colors.muted,
                            unselectedTextColor = colors.muted
                        ),
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.HOME -> HomeScreen(
                padding = padding,
                moneyRepository = moneyRepository,
                onReviewClick = { selectedTab = AppTab.REVIEW },
                editTransactionUseCase = editTransactionUseCase,
                assetRepository = assetRepository,
                notificationAccessEnabled = notificationAccessEnabled,
                showNotificationOnboarding = showNotificationOnboarding,
                onDismissNotificationOnboarding = {
                    notificationOnboardingStore?.markDismissed()
                    notificationOnboardingDismissed = true
                },
                onOpenNotificationSettings = openNotificationSettings
            )
            AppTab.TRANSACTIONS -> TransactionsScreen(
                padding = padding,
                moneyRepository = moneyRepository,
                saveManualTransactionUseCase = saveManualTransactionUseCase,
                editTransactionUseCase = editTransactionUseCase,
                assetRepository = assetRepository,
                walletTopupNoticeStore = walletTopupNoticeStore,
                notificationAccessEnabled = notificationAccessEnabled,
                onOpenNotificationSettings = openNotificationSettings
            )
            AppTab.REVIEW -> ReviewScreen(
                padding = padding,
                recordWalletTopupUsageUseCase = recordWalletTopupUsageUseCase,
                moneyRepository = moneyRepository,
                editTransactionUseCase = editTransactionUseCase,
                assetRepository = assetRepository,
                resolveReviewUseCase = resolveReviewUseCase,
                resolveAccountTransferUseCase = resolveAccountTransferUseCase,
                linkSettlementRepaymentUseCase = linkSettlementRepaymentUseCase
            )
            AppTab.ASSETS -> AssetsScreen(
                padding = padding,
                assetRepository = assetRepository
            )
            AppTab.REPORT -> MonthlyReportScreen(
                padding = padding,
                moneyRepository = moneyRepository
            )
            AppTab.SETTINGS -> SettingsScreen(
                padding = padding,
                onOpenNotificationSettings = openNotificationSettings,
                notificationAccessEnabled = notificationAccessEnabled,
                lastNotificationDiagnostic = lastNotificationDiagnostic,
                userCategoryRepository = userCategoryRepository,
                onRunSampleNotificationScenario = if (BuildConfig.DEBUG && runSampleNotificationScenarioUseCase != null) {
                    {
                        scope.launch {
                            runSampleNotificationScenarioUseCase.run(sampleNotificationScenarios.first())
                            lastNotificationDiagnostic = notificationDiagnosticsStore?.load()
                        }
                    }
                } else {
                    null
                }
            )
        }
    }
}
