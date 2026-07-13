package com.choiyoonseo.automoney.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.choiyoonseo.automoney.MainActivity
import com.choiyoonseo.automoney.R
import java.util.concurrent.atomic.AtomicInteger

class NotificationIngestionFeedbackNotifier(
    private val context: Context
) {
    fun notify(result: IngestionResult) {
        val feedback = notificationFeedbackFor(result) ?: return
        if (!canPostNotifications()) return

        ensureChannel(feedback.kind)
        NotificationManagerCompat.from(context).notify(
            nextNotificationId.incrementAndGet(),
            NotificationCompat.Builder(context, channelIdFor(feedback.kind))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(feedback.title)
                .setContentText(feedback.text)
                .setContentIntent(contentIntent())
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(kind: NotificationIngestionFeedbackKind) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            channelIdFor(kind),
            if (kind == NotificationIngestionFeedbackKind.NEEDS_REVIEW) {
                "거래 검토 알림"
            } else {
                "거래 자동 입력 알림"
            },
            if (kind == NotificationIngestionFeedbackKind.NEEDS_REVIEW) {
                NotificationManager.IMPORTANCE_DEFAULT
            } else {
                NotificationManager.IMPORTANCE_LOW
            }
        ).apply {
            description = "AutoMoney 거래 입력 결과 알림"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun channelIdFor(kind: NotificationIngestionFeedbackKind): String = when (kind) {
    NotificationIngestionFeedbackKind.AUTO_RECORDED -> "transaction_auto_recorded"
    NotificationIngestionFeedbackKind.NEEDS_REVIEW -> "transaction_needs_review"
}

private val nextNotificationId = AtomicInteger(20_000)
