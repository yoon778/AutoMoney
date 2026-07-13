package com.choiyoonseo.automoney.notification

import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NotificationSourceStoresInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun clearPreferences() {
        context.getSharedPreferences("notification_sources", 0).edit().clear().commit()
    }

    @Test
    fun sharedPreferencesAdaptersSurviveRecreation() {
        val packageName = "com.kbankwith.smartbank"
        SharedPreferencesNotificationAppAccessStore(context).setAllowed(packageName, true)
        SharedPreferencesObservedNotificationSourceStore(context).record(
            packageName = packageName,
            seenAt = Instant.ofEpochMilli(1000)
        )

        val recreatedAccessStore = SharedPreferencesNotificationAppAccessStore(context)
        val recreatedObservedStore = SharedPreferencesObservedNotificationSourceStore(context)

        assertEquals(
            NotificationSourceAccess.SELECTED_UNVERIFIED,
            recreatedAccessStore.accessFor(packageName)
        )
        assertEquals(
            listOf(ObservedNotificationSource(packageName, Instant.ofEpochMilli(1000), 1)),
            recreatedObservedStore.load()
        )
    }
}
