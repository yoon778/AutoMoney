package com.choiyoonseo.automoney.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.choiyoonseo.automoney.AutoMoneyApplication
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MoneyNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = applicationContext as AutoMoneyApplication
        val prepared = app.container.notificationDispatchCoordinator.prepare(
            packageName = sbn.packageName,
            postedAt = Instant.ofEpochMilli(sbn.postTime),
            readContent = {
                val notification = sbn.notification
                val extras = notification.extras
                NotificationContentFields(
                    packageName = sbn.packageName,
                    notificationKey = sbn.key.take(MAX_NOTIFICATION_KEY_READ_LENGTH),
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)
                        .toBoundedString(MAX_NOTIFICATION_TITLE_LENGTH),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)
                        .toBoundedString(MAX_NOTIFICATION_TEXT_LENGTH),
                    bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                        .toBoundedString(MAX_NOTIFICATION_EXPANDED_TEXT_LENGTH),
                    textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                        .orEmpty()
                        .asSequence()
                        .take(MAX_NOTIFICATION_EXPANDED_LINES)
                        .mapNotNull { it.toBoundedString(MAX_NOTIFICATION_TEXT_LENGTH) }
                        .toList(),
                    postTimeMillis = sbn.postTime
                )
            }
        )
        prepared ?: return

        scope.launch {
            try {
                val result = app.container.notificationIngestionUseCase.ingest(
                    prepared.snapshot,
                    prepared.sourceAccess
                )
                app.container.notificationDiagnosticsStore.save(
                    LastNotificationDiagnostic.fromIngestionResult(
                        snapshot = prepared.snapshot,
                        result = result,
                        sourceAccess = prepared.sourceAccess
                    )
                )
                app.container.notificationIngestionFeedbackNotifier.notify(result)
            } catch (e: RuntimeException) {
                app.container.notificationDiagnosticsStore.save(
                    LastNotificationDiagnostic.fromError(
                        snapshot = prepared.snapshot,
                        throwable = e,
                        sourceAccess = prepared.sourceAccess
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

private fun CharSequence?.toBoundedString(maxLength: Int): String? {
    if (this == null) return null
    val endIndex = minOf(length, maxLength)
    return subSequence(0, endIndex).toString()
}

private const val MAX_NOTIFICATION_KEY_READ_LENGTH = 256
