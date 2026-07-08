package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationAccessCheckerTest {
    @Test
    fun detectsEnabledListenerFromColonSeparatedSetting() {
        val enabled = listOf(
            "com.other.app/com.other.app.Listener",
            "com.choiyoonseo.automoney/com.choiyoonseo.automoney.notification.MoneyNotificationListenerService"
        ).joinToString(":")

        val result = isListenerEnabledInSetting(
            enabledListeners = enabled,
            packageName = "com.choiyoonseo.automoney",
            listenerClassName = "com.choiyoonseo.automoney.notification.MoneyNotificationListenerService"
        )

        assertThat(result).isTrue()
    }

    @Test
    fun detectsEnabledListenerWhenClassNameIsRelative() {
        val result = isListenerEnabledInSetting(
            enabledListeners = "com.choiyoonseo.automoney/.notification.MoneyNotificationListenerService",
            packageName = "com.choiyoonseo.automoney",
            listenerClassName = "com.choiyoonseo.automoney.notification.MoneyNotificationListenerService"
        )

        assertThat(result).isTrue()
    }

    @Test
    fun returnsFalseForMissingOrOtherListener() {
        assertThat(
            isListenerEnabledInSetting(
                enabledListeners = null,
                packageName = "com.choiyoonseo.automoney",
                listenerClassName = "com.choiyoonseo.automoney.notification.MoneyNotificationListenerService"
            )
        ).isFalse()

        assertThat(
            isListenerEnabledInSetting(
                enabledListeners = "com.other.app/com.other.app.Listener",
                packageName = "com.choiyoonseo.automoney",
                listenerClassName = "com.choiyoonseo.automoney.notification.MoneyNotificationListenerService"
            )
        ).isFalse()
    }
}
