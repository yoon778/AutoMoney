package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class ObservedNotificationSourceStoreTest {
    private val preferences = FakeNotificationSourcePreferences()
    private val store = SharedPreferencesObservedNotificationSourceStore(
        preferences = preferences,
        selfPackageName = "com.choiyoonseo.automoney"
    )

    @Test
    fun repeatedPackageUpdatesCountAndLastSeen() {
        store.record("com.example.bank", Instant.ofEpochSecond(10))
        store.record("com.example.bank", Instant.ofEpochSecond(20))

        assertThat(store.load()).containsExactly(
            ObservedNotificationSource(
                packageName = "com.example.bank",
                lastSeenAt = Instant.ofEpochSecond(20),
                count = 2
            )
        )
    }

    @Test
    fun keepsMostRecentFiftyPackages() {
        repeat(51) { index ->
            store.record("com.example.bank$index", Instant.ofEpochSecond(index.toLong()))
        }

        val loaded = store.load()

        assertThat(loaded).hasSize(50)
        assertThat(loaded.first().packageName).isEqualTo("com.example.bank50")
        assertThat(loaded.map { it.packageName }).doesNotContain("com.example.bank0")
    }

    @Test
    fun ignoresInvalidAndExactDeniedPackages() {
        listOf(
            "not a package",
            "android",
            "com.android.systemui",
            "com.choiyoonseo.automoney"
        ).forEach { store.record(it, Instant.EPOCH) }

        assertThat(store.load()).isEmpty()
    }

    @Test
    fun corruptRowsAreSkippedWithoutDroppingValidRows() {
        preferences.putString(
            key = "observed_sources",
            value = "bad-row\ncom.example.bank|1000|2"
        )

        assertThat(store.load()).containsExactly(
            ObservedNotificationSource(
                packageName = "com.example.bank",
                lastSeenAt = Instant.ofEpochMilli(1000),
                count = 2
            )
        )
    }

    @Test
    fun codecContainsMetadataOnly() {
        val encoded = encodeObservedNotificationSources(
            listOf(
                ObservedNotificationSource(
                    packageName = "com.example.bank",
                    lastSeenAt = Instant.ofEpochMilli(1000),
                    count = 1
                )
            )
        )

        assertThat(encoded).isEqualTo("com.example.bank|1000|1")
    }
}
