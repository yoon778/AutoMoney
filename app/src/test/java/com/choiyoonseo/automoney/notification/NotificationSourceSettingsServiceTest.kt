package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class NotificationSourceSettingsServiceTest {
    @Test
    fun optionsUnionCatalogObservedAndEnabledWithLabelFallbackAndOrdering() {
        val accessStore = FakeAccessStore(
            access = mutableMapOf(
                "known.trusted" to NotificationSourceAccess.TRUSTED,
                "custom.enabled" to NotificationSourceAccess.SELECTED_UNVERIFIED
            ),
            enabled = mutableSetOf("custom.enabled")
        )
        val observedStore = FakeObservedStore(
            listOf(
                ObservedNotificationSource("custom.blocked", Instant.parse("2026-07-13T03:00:00Z"), 3),
                ObservedNotificationSource("custom.enabled", Instant.parse("2026-07-13T02:00:00Z"), 2)
            )
        )
        val service = NotificationSourceSettingsService(
            accessStore = accessStore,
            observedStore = observedStore,
            registryInfos = {
                listOf(
                    FinancialAppInfo("known.trusted", "등록 은행", "K", null, defaultContentAccess = true),
                    FinancialAppInfo("known.off", "기본 차단", "O", null)
                )
            },
            resolveInstalledLabel = { if (it == "custom.enabled") "사용자 은행" else null }
        )

        val options = service.options()

        assertThat(options.map { it.packageName }).containsExactly(
            "custom.enabled",
            "known.trusted",
            "custom.blocked",
            "known.off"
        ).inOrder()
        assertThat(options.first { it.packageName == "known.trusted" }.displayName).isEqualTo("등록 은행")
        assertThat(options.first { it.packageName == "custom.enabled" }.displayName).isEqualTo("사용자 은행")
        assertThat(options.first { it.packageName == "custom.blocked" }.displayName).isEqualTo("custom.blocked")
        assertThat(options.first { it.packageName == "known.off" }.lastSeenAt).isNull()
        assertThat(options.first { it.packageName == "known.off" }.count).isEqualTo(0)
    }

    @Test
    fun setAllowedDelegatesAndClearsOnlyDisabledPackageDiagnostic() {
        val accessStore = FakeAccessStore()
        val cleared = mutableListOf<String>()
        val service = NotificationSourceSettingsService(
            accessStore = accessStore,
            observedStore = FakeObservedStore(emptyList()),
            registryInfos = { emptyList() },
            clearDiagnosticIfPackage = cleared::add
        )

        assertThat(service.setAllowed("custom.bank", true)).isEqualTo(NotificationAccessUpdateResult.UPDATED)
        assertThat(cleared).isEmpty()
        assertThat(service.setAllowed("custom.bank", false)).isEqualTo(NotificationAccessUpdateResult.UPDATED)
        assertThat(cleared).containsExactly("custom.bank")
    }

    @Test
    fun failedDisableDoesNotClearDiagnostic() {
        val accessStore = FakeAccessStore(nextResult = NotificationAccessUpdateResult.DENIED_PACKAGE)
        val cleared = mutableListOf<String>()
        val service = NotificationSourceSettingsService(
            accessStore = accessStore,
            observedStore = FakeObservedStore(emptyList()),
            registryInfos = { emptyList() },
            clearDiagnosticIfPackage = cleared::add
        )

        assertThat(service.setAllowed("android", false)).isEqualTo(NotificationAccessUpdateResult.DENIED_PACKAGE)
        assertThat(cleared).isEmpty()
    }
}

private class FakeAccessStore(
    private val access: MutableMap<String, NotificationSourceAccess> = mutableMapOf(),
    private val enabled: MutableSet<String> = mutableSetOf(),
    private val nextResult: NotificationAccessUpdateResult = NotificationAccessUpdateResult.UPDATED
) : NotificationAppAccessStore {
    override fun accessFor(packageName: String): NotificationSourceAccess =
        access[packageName] ?: NotificationSourceAccess.BLOCKED

    override fun setAllowed(packageName: String, allowed: Boolean): NotificationAccessUpdateResult {
        if (nextResult != NotificationAccessUpdateResult.UPDATED) return nextResult
        access[packageName] = if (allowed) NotificationSourceAccess.SELECTED_UNVERIFIED else NotificationSourceAccess.BLOCKED
        if (allowed) enabled += packageName else enabled -= packageName
        return nextResult
    }

    override fun explicitlyEnabledPackages(): Set<String> = enabled.toSet()
}

private class FakeObservedStore(
    private val sources: List<ObservedNotificationSource>
) : ObservedNotificationSourceStore {
    override fun record(packageName: String, seenAt: Instant) = Unit
    override fun load(): List<ObservedNotificationSource> = sources
}
