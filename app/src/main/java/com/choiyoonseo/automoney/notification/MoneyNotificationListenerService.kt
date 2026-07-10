package com.choiyoonseo.automoney.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.choiyoonseo.automoney.AutoMoneyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MoneyNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshotBuilder = NotificationSnapshotBuilder()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val snapshot = snapshotBuilder.build(
            NotificationContentFields(
                packageName = sbn.packageName,
                notificationKey = sbn.key,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    ?.map { it.toString() }
                    .orEmpty(),
                postTimeMillis = sbn.postTime
            )
        )

        if (!FinancialAppRegistry.isSupportedPackage(sbn.packageName)) {
            scope.launch {
                val app = applicationContext as AutoMoneyApplication
                app.container.notificationDiagnosticsStore.save(
                    LastNotificationDiagnostic.fromUnsupportedPackage(snapshot)
                )
            }
            return
        }

        scope.launch {
            val app = applicationContext as AutoMoneyApplication
            try {
                val result = app.container.notificationIngestionUseCase.ingest(snapshot)
                app.container.notificationDiagnosticsStore.save(
                    LastNotificationDiagnostic.fromIngestionResult(
                        snapshot = snapshot,
                        result = result
                    )
                )
            } catch (e: RuntimeException) {
                app.container.notificationDiagnosticsStore.save(
                    LastNotificationDiagnostic.fromError(
                        snapshot = snapshot,
                        throwable = e
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
