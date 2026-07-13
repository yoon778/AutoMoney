package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class MoneyNotificationListenerServiceTest {
    @Test
    fun listenerCancelsCoroutineScopeOnDestroy() {
        val service = File("src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt")
            .readText()

        assertThat(service).contains("override fun onDestroy()")
        assertThat(service).contains("scope.cancel()")
    }

    @Test
    fun listenerGatesContentBeforeStartingIngestionCoroutine() {
        val service = File("src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt")
            .readText()

        assertThat(service).doesNotContain("LastNotificationDiagnostic.fromUnsupportedPackage")
        assertThat(service).contains("notificationDispatchCoordinator.prepare")
        assertThat(service.indexOf("readContent = {")).isLessThan(service.indexOf("sbn.notification"))
        assertThat(service.indexOf("prepared ?: return")).isLessThan(service.indexOf("scope.launch"))
    }
}
