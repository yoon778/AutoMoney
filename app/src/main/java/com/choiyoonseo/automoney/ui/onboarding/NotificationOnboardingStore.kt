package com.choiyoonseo.automoney.ui.onboarding

import android.content.Context

interface NotificationOnboardingStore {
    fun hasDismissed(): Boolean
    fun markDismissed()
}

class SharedPreferencesNotificationOnboardingStore(context: Context) : NotificationOnboardingStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        NOTIFICATION_ONBOARDING_PREFERENCES,
        Context.MODE_PRIVATE
    )

    override fun hasDismissed(): Boolean =
        preferences.getBoolean(KEY_NOTIFICATION_ONBOARDING_DISMISSED, false)

    override fun markDismissed() {
        preferences.edit().putBoolean(KEY_NOTIFICATION_ONBOARDING_DISMISSED, true).apply()
    }
}

internal fun shouldShowNotificationOnboarding(
    notificationAccessEnabled: Boolean,
    hasDismissed: Boolean
): Boolean =
    !notificationAccessEnabled && !hasDismissed

private const val NOTIFICATION_ONBOARDING_PREFERENCES = "notification_onboarding"
private const val KEY_NOTIFICATION_ONBOARDING_DISMISSED = "notification_onboarding_dismissed"
