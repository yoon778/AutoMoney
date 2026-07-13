package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import java.time.Instant

data class PreparedNotification(
    val snapshot: NotificationSnapshot,
    val sourceAccess: NotificationSourceAccess
)

class NotificationDispatchCoordinator(
    private val accessFor: (String) -> NotificationSourceAccess,
    private val recordObserved: (String, Instant) -> Unit,
    private val snapshotBuilder: NotificationSnapshotBuilder
) {
    fun prepare(
        packageName: String,
        postedAt: Instant,
        readContent: () -> NotificationContentFields
    ): PreparedNotification? {
        recordObserved(packageName, postedAt)
        val sourceAccess = accessFor(packageName)
        if (sourceAccess == NotificationSourceAccess.BLOCKED) return null

        val fields = readContent().copy(
            packageName = packageName,
            postTimeMillis = postedAt.toEpochMilli()
        )
        return PreparedNotification(
            snapshot = snapshotBuilder.build(fields),
            sourceAccess = sourceAccess
        )
    }
}
