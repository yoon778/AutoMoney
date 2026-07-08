package com.choiyoonseo.automoney.notification

import android.content.Context
import android.provider.Settings

class NotificationAccessChecker(
    private val context: Context
) {
    fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        )
        return isListenerEnabledInSetting(
            enabledListeners = enabledListeners,
            packageName = context.packageName,
            listenerClassName = MoneyNotificationListenerService::class.java.name
        )
    }
}

internal fun isListenerEnabledInSetting(
    enabledListeners: String?,
    packageName: String,
    listenerClassName: String
): Boolean {
    if (enabledListeners.isNullOrBlank()) return false

    return enabledListeners.split(':').any { rawComponent ->
        val component = rawComponent.trim()
        val slashIndex = component.indexOf('/')
        if (slashIndex <= 0 || slashIndex == component.lastIndex) {
            return@any false
        }

        val componentPackage = component.substring(0, slashIndex)
        if (componentPackage != packageName) {
            return@any false
        }

        val rawClassName = component.substring(slashIndex + 1)
        val resolvedClassName = if (rawClassName.startsWith(".")) {
            packageName + rawClassName
        } else {
            rawClassName
        }

        resolvedClassName == listenerClassName
    }
}

private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
