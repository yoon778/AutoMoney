# Task 2 Review Package

Repository note: no HEAD commit exists; package contains current contents of task-owned files.

## app\src\main\java\com\choiyoonseo\automoney\notification\FinancialAppRegistry.kt
```kotlin
package com.choiyoonseo.automoney.notification

object FinancialAppRegistry {
    const val TOSS_PACKAGE = "viva.republica.toss"
    const val KB_STAR_BANKING_PACKAGE = "com.kbstar.kbbank"

    private val supportedPackages = setOf(
        TOSS_PACKAGE,
        KB_STAR_BANKING_PACKAGE
    )

    fun isSupportedPackage(packageName: String): Boolean =
        packageName in supportedPackages
}
```

## app\src\main\java\com\choiyoonseo\automoney\notification\MoneyNotificationListenerService.kt
```kotlin
package com.choiyoonseo.automoney.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.choiyoonseo.automoney.AutoMoneyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MoneyNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshotBuilder = NotificationSnapshotBuilder()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!FinancialAppRegistry.isSupportedPackage(sbn.packageName)) return

        val extras = sbn.notification.extras
        val snapshot = snapshotBuilder.build(
            NotificationContentFields(
                packageName = sbn.packageName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    ?.map { it.toString() }
                    .orEmpty(),
                postTimeMillis = sbn.postTime
            )
        )

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
}
```

## app\src\test\java\com\choiyoonseo\automoney\notification\FinancialAppRegistryTest.kt
```kotlin
package com.choiyoonseo.automoney.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAppRegistryTest {
    @Test
    fun supportsTossAndKbStarBanking() {
        assertTrue(FinancialAppRegistry.isSupportedPackage("viva.republica.toss"))
        assertTrue(FinancialAppRegistry.isSupportedPackage("com.kbstar.kbbank"))
    }

    @Test
    fun rejectsUnknownAndBlankPackages() {
        assertFalse(FinancialAppRegistry.isSupportedPackage("com.shopping.adapp"))
        assertFalse(FinancialAppRegistry.isSupportedPackage(""))
    }
}
```

