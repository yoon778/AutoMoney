package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationAppAccessStoreTest {
    private val preferences = FakeNotificationSourcePreferences()
    private val store = SharedPreferencesNotificationAppAccessStore(
        preferences = preferences,
        selfPackageName = "com.choiyoonseo.automoney"
    )

    @Test
    fun knownPackageCanBeDisabledAndReenabled() {
        val packageName = FinancialAppRegistry.KAKAO_BANKING_PACKAGE

        assertThat(store.accessFor(packageName)).isEqualTo(NotificationSourceAccess.TRUSTED)
        assertThat(store.setAllowed(packageName, false))
            .isEqualTo(NotificationAccessUpdateResult.UPDATED)
        assertThat(store.accessFor(packageName)).isEqualTo(NotificationSourceAccess.BLOCKED)

        assertThat(store.setAllowed(packageName, true))
            .isEqualTo(NotificationAccessUpdateResult.UPDATED)
        assertThat(store.accessFor(packageName)).isEqualTo(NotificationSourceAccess.TRUSTED)
    }

    @Test
    fun unknownPackageCanBeEnabledAndDisabled() {
        val packageName = "com.kbankwith.smartbank"

        assertThat(store.setAllowed(packageName, true))
            .isEqualTo(NotificationAccessUpdateResult.UPDATED)
        assertThat(store.accessFor(packageName))
            .isEqualTo(NotificationSourceAccess.SELECTED_UNVERIFIED)
        assertThat(store.explicitlyEnabledPackages()).containsExactly(packageName)

        assertThat(store.setAllowed(packageName, false))
            .isEqualTo(NotificationAccessUpdateResult.UPDATED)
        assertThat(store.accessFor(packageName)).isEqualTo(NotificationSourceAccess.BLOCKED)
        assertThat(store.explicitlyEnabledPackages()).isEmpty()
    }

    @Test
    fun enabledStateSurvivesStoreRecreation() {
        val packageName = "com.example.bank"
        store.setAllowed(packageName, true)

        val recreated = SharedPreferencesNotificationAppAccessStore(
            preferences = preferences,
            selfPackageName = "com.choiyoonseo.automoney"
        )

        assertThat(recreated.accessFor(packageName))
            .isEqualTo(NotificationSourceAccess.SELECTED_UNVERIFIED)
        assertThat(recreated.explicitlyEnabledPackages()).containsExactly(packageName)
    }

    @Test
    fun enabledPackageRemainsAvailableWhenObservedLruEvictsIt() {
        val packageName = "com.example.keep"
        store.setAllowed(packageName, true)
        val observedStore = SharedPreferencesObservedNotificationSourceStore(
            preferences = preferences,
            selfPackageName = "com.choiyoonseo.automoney"
        )
        observedStore.record(packageName, java.time.Instant.EPOCH)
        repeat(51) { index ->
            observedStore.record(
                "com.example.recent$index",
                java.time.Instant.ofEpochSecond(index.toLong() + 1)
            )
        }

        assertThat(observedStore.load().map { it.packageName }).doesNotContain(packageName)
        assertThat(store.explicitlyEnabledPackages()).containsExactly(packageName)
    }

    @Test
    fun enabledPackageLimitDoesNotMutateState() {
        repeat(50) { index ->
            assertThat(store.setAllowed("com.example.bank$index", true))
                .isEqualTo(NotificationAccessUpdateResult.UPDATED)
        }

        assertThat(store.setAllowed("com.example.overflow", true))
            .isEqualTo(NotificationAccessUpdateResult.LIMIT_REACHED)
        assertThat(store.explicitlyEnabledPackages()).hasSize(50)
        assertThat(store.accessFor("com.example.overflow"))
            .isEqualTo(NotificationSourceAccess.BLOCKED)
    }

    @Test
    fun rejectsInvalidAndExactDeniedPackages() {
        assertThat(store.setAllowed("not a package", true))
            .isEqualTo(NotificationAccessUpdateResult.INVALID_PACKAGE)
        assertThat(store.setAllowed("android", true))
            .isEqualTo(NotificationAccessUpdateResult.DENIED_PACKAGE)
        assertThat(store.setAllowed("com.android.systemui", true))
            .isEqualTo(NotificationAccessUpdateResult.DENIED_PACKAGE)
        assertThat(store.setAllowed("com.choiyoonseo.automoney", true))
            .isEqualTo(NotificationAccessUpdateResult.DENIED_PACKAGE)
    }

    @Test
    fun returnedEnabledSetIsDefensiveCopy() {
        store.setAllowed("com.example.bank", true)
        val returned = store.explicitlyEnabledPackages().toMutableSet()

        returned.clear()

        assertThat(store.explicitlyEnabledPackages()).containsExactly("com.example.bank")
    }
}

internal class FakeNotificationSourcePreferences : NotificationSourcePreferences {
    private val stringSets = mutableMapOf<String, Set<String>>()
    private val strings = mutableMapOf<String, String>()

    override fun getStringSet(key: String): Set<String> =
        stringSets[key]?.toSet().orEmpty()

    override fun putStringSets(values: Map<String, Set<String>>) {
        values.forEach { (key, value) -> stringSets[key] = value.toSet() }
    }

    override fun getString(key: String): String? = strings[key]

    override fun putString(key: String, value: String) {
        strings[key] = value
    }
}
