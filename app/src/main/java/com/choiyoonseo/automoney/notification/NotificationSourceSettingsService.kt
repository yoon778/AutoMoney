package com.choiyoonseo.automoney.notification

import java.time.Instant

data class NotificationSourceOption(
    val packageName: String,
    val displayName: String,
    val isRegistered: Boolean,
    val isDefaultTrusted: Boolean,
    val access: NotificationSourceAccess,
    val lastSeenAt: Instant?,
    val count: Int
)

class NotificationSourceSettingsService(
    private val accessStore: NotificationAppAccessStore,
    private val observedStore: ObservedNotificationSourceStore,
    private val registryInfos: () -> List<FinancialAppInfo> = FinancialAppRegistry::allAppInfos,
    private val resolveInstalledLabel: (String) -> String? = { null },
    private val clearDiagnosticIfPackage: (String) -> Unit = {}
) {
    fun options(): List<NotificationSourceOption> {
        val registryByPackage = registryInfos().associateBy(FinancialAppInfo::packageName)
        val observedByPackage = observedStore.load().associateBy(ObservedNotificationSource::packageName)
        val packages = registryByPackage.keys + observedByPackage.keys + accessStore.explicitlyEnabledPackages()

        return packages.map { packageName ->
            val appInfo = registryByPackage[packageName]
            val observed = observedByPackage[packageName]
            NotificationSourceOption(
                packageName = packageName,
                displayName = appInfo?.displayName
                    ?: resolveInstalledLabel(packageName)?.trim()?.takeIf(String::isNotBlank)
                    ?: packageName,
                isRegistered = appInfo != null,
                isDefaultTrusted = appInfo?.defaultContentAccess == true,
                access = accessStore.accessFor(packageName),
                lastSeenAt = observed?.lastSeenAt,
                count = observed?.count ?: 0
            )
        }.sortedWith(
            compareByDescending<NotificationSourceOption> { it.access != NotificationSourceAccess.BLOCKED }
                .thenByDescending { it.lastSeenAt }
                .thenBy { it.displayName }
                .thenBy { it.packageName }
        )
    }

    fun setAllowed(
        packageName: String,
        allowed: Boolean
    ): NotificationAccessUpdateResult {
        val result = accessStore.setAllowed(packageName, allowed)
        if (!allowed && result == NotificationAccessUpdateResult.UPDATED) {
            clearDiagnosticIfPackage(packageName)
        }
        return result
    }
}
