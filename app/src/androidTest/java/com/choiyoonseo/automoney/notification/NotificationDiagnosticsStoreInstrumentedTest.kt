package com.choiyoonseo.automoney.notification

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDiagnosticsStoreInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun clearPreferences() {
        context.getSharedPreferences("notification_diagnostics", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun loadingLegacyUnsupportedDiagnosticClearsPreferences() {
        val preferences = context.getSharedPreferences(
            "notification_diagnostics",
            Context.MODE_PRIVATE
        )
        preferences.edit()
            .putString("receivedAt", "2026-07-02T03:00:05Z")
            .putString("postedAt", "2026-07-02T03:00:00Z")
            .putString("packageName", "com.shopping.adapp")
            .putString("textPreview", "masked")
            .putString("result", "IGNORED")
            .putString("message", "unsupported package")
            .commit()

        assertNull(NotificationDiagnosticsStore(context).load())
        assertTrue(preferences.all.isEmpty())
    }
}
