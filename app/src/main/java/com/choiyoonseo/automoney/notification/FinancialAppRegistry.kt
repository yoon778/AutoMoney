package com.choiyoonseo.automoney.notification

data class FinancialAppInfo(
    val packageName: String,
    val displayName: String,
    val badgeText: String
)

object FinancialAppRegistry {
    const val TOSS_PACKAGE = "viva.republica.toss"
    const val KB_STAR_BANKING_PACKAGE = "com.kbstar.kbbank"

    private val supportedAppInfos = listOf(
        FinancialAppInfo(
            packageName = TOSS_PACKAGE,
            displayName = "\ud1a0\uc2a4",
            badgeText = "T"
        ),
        FinancialAppInfo(
            packageName = KB_STAR_BANKING_PACKAGE,
            displayName = "\uad6d\ubbfc\uc740\ud589",
            badgeText = "KB"
        )
    )
    private val appInfoByPackage = supportedAppInfos.associateBy { it.packageName }
    private val supportedPackages = appInfoByPackage.keys

    fun isSupportedPackage(packageName: String): Boolean =
        packageName in supportedPackages

    fun infoForPackage(packageName: String): FinancialAppInfo? =
        appInfoByPackage[packageName]
}
