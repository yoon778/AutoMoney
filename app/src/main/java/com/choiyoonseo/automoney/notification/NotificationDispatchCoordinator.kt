package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import java.time.Instant

data class PreparedNotification(
    val snapshot: NotificationSnapshot,
    val sourceAccess: NotificationSourceAccess,
    val sourceLabel: String? = null
)

class NotificationDispatchCoordinator(
    private val accessFor: (String) -> NotificationSourceAccess,
    private val recordObserved: (String, Instant) -> Unit,
    private val snapshotBuilder: NotificationSnapshotBuilder,
    private val resolveInstalledLabel: (String) -> String? = { null },
    // 이력은 최근 200건만 유지하므로, 차단 이력은 사용자가 껐을 법한 금융앱만 남긴다.
    // 모든 앱을 남기면 일반 알림이 금융 이력을 밀어낸다.
    private val recordsBlockedHistory: (String) -> Boolean = { packageName ->
        FinancialAppRegistry.infoForPackage(packageName) != null
    }
) {
    fun prepare(
        packageName: String,
        postedAt: Instant,
        readContent: () -> NotificationContentFields
    ): PreparedNotification? {
        recordObserved(packageName, postedAt)
        val sourceAccess = accessFor(packageName)
        if (sourceAccess == NotificationSourceAccess.BLOCKED) {
            if (!recordsBlockedHistory(packageName)) return null
            // 차단된 소스는 본문을 읽지 않는다 — 패키지/시각만 이력에 남긴다.
            return PreparedNotification(
                snapshot = NotificationSnapshot(
                    packageName = packageName,
                    title = null,
                    text = null,
                    bigText = null,
                    postedAt = postedAt
                ),
                sourceAccess = sourceAccess,
                sourceLabel = resolveInstalledLabel(packageName)
            )
        }

        val fields = readContent().copy(
            packageName = packageName,
            postTimeMillis = postedAt.toEpochMilli()
        )
        return PreparedNotification(
            snapshot = snapshotBuilder.build(fields),
            sourceAccess = sourceAccess,
            sourceLabel = resolveInstalledLabel(packageName)
        )
    }
}
