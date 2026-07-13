package com.choiyoonseo.automoney.notification

enum class NotificationSourceAccess {
    BLOCKED,
    TRUSTED,
    SELECTED_UNVERIFIED
}

class NotificationSourceAccessPolicy(
    private val appInfoForPackage: (String) -> FinancialAppInfo? =
        FinancialAppRegistry::infoForPackage
) {
    fun accessFor(
        packageName: String,
        explicitlyEnabledPackages: Set<String> = emptySet(),
        explicitlyDisabledPackages: Set<String> = emptySet()
    ): NotificationSourceAccess {
        if (packageName in explicitlyDisabledPackages) {
            return NotificationSourceAccess.BLOCKED
        }

        val appInfo = appInfoForPackage(packageName)
        if (appInfo?.defaultContentAccess == true) {
            return NotificationSourceAccess.TRUSTED
        }

        return if (packageName in explicitlyEnabledPackages) {
            NotificationSourceAccess.SELECTED_UNVERIFIED
        } else {
            NotificationSourceAccess.BLOCKED
        }
    }
}
