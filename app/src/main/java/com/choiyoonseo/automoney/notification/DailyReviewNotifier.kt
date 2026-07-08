package com.choiyoonseo.automoney.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.choiyoonseo.automoney.R

class DailyReviewNotifier(private val context: Context) {
    fun show(openReviewCount: Int, autoRecordedCount: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "가계부 검토",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("오늘 가계부 확인 필요 ${openReviewCount}건")
            .setContentText("자동 기록 ${autoRecordedCount}건, 검토 필요 ${openReviewCount}건")
            .setAutoCancel(true)
            .build()

        manager.notify(DAILY_REVIEW_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "daily_review"
        private const val DAILY_REVIEW_ID = 1001
    }
}

