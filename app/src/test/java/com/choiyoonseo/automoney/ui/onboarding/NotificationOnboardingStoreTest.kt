package com.choiyoonseo.automoney.ui.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationOnboardingStoreTest {
    @Test
    fun notificationOnboardingShowsUntilAccessGrantedOrDismissed() {
        assertThat(
            shouldShowNotificationOnboarding(
                notificationAccessEnabled = false,
                hasDismissed = false
            )
        ).isTrue()

        assertThat(
            shouldShowNotificationOnboarding(
                notificationAccessEnabled = true,
                hasDismissed = false
            )
        ).isFalse()

        assertThat(
            shouldShowNotificationOnboarding(
                notificationAccessEnabled = false,
                hasDismissed = true
            )
        ).isFalse()
    }
}
