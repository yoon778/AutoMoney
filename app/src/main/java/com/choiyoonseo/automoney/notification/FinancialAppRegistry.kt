package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.assets.BankProvider

data class FinancialAppInfo(
    val packageName: String,
    val displayName: String,
    val badgeText: String,
    val bankProvider: BankProvider?,
    val aggregatesMultipleBanks: Boolean = false,
    val defaultContentAccess: Boolean = false
)

object FinancialAppRegistry {
    const val TOSS_PACKAGE = "viva.republica.toss"
    const val KB_STAR_BANKING_PACKAGE = "com.kbstar.kbbank"
    const val SHINHAN_BANKING_PACKAGE = "com.shinhan.sbanking"
    const val SHINHAN_LEGACY_PACKAGE = "com.shinhan.smartcaremgr"
    const val HANA_BANKING_PACKAGE = "com.hanabank.oqf"
    const val HANA_LEGACY_PACKAGE = "com.kebhana.hanapush"
    const val WOORI_BANKING_PACKAGE = "com.wooribank.smart.npib"
    const val NH_BANKING_PACKAGE = "nh.smart.banking"
    const val IBK_BANKING_PACKAGE = "com.ibk.android.ionebank"
    const val KAKAO_BANKING_PACKAGE = "com.kakaobank.channel"

    private val supportedAppInfos = listOf(
        FinancialAppInfo(
            packageName = TOSS_PACKAGE,
            displayName = "\ud1a0\uc2a4",
            badgeText = "T",
            bankProvider = null,
            aggregatesMultipleBanks = true,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = KB_STAR_BANKING_PACKAGE,
            displayName = "\uad6d\ubbfc\uc740\ud589",
            badgeText = "KB",
            bankProvider = BankProvider.KB,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = SHINHAN_BANKING_PACKAGE,
            displayName = BankProvider.SHINHAN.displayName,
            badgeText = BankProvider.SHINHAN.badgeText,
            bankProvider = BankProvider.SHINHAN,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = SHINHAN_LEGACY_PACKAGE,
            displayName = BankProvider.SHINHAN.displayName,
            badgeText = BankProvider.SHINHAN.badgeText,
            bankProvider = BankProvider.SHINHAN,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = HANA_BANKING_PACKAGE,
            displayName = BankProvider.HANA.displayName,
            badgeText = BankProvider.HANA.badgeText,
            bankProvider = BankProvider.HANA,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = HANA_LEGACY_PACKAGE,
            displayName = BankProvider.HANA.displayName,
            badgeText = BankProvider.HANA.badgeText,
            bankProvider = BankProvider.HANA,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = WOORI_BANKING_PACKAGE,
            displayName = BankProvider.WOORI.displayName,
            badgeText = BankProvider.WOORI.badgeText,
            bankProvider = BankProvider.WOORI,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = NH_BANKING_PACKAGE,
            displayName = BankProvider.NH.displayName,
            badgeText = BankProvider.NH.badgeText,
            bankProvider = BankProvider.NH,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = IBK_BANKING_PACKAGE,
            displayName = BankProvider.IBK.displayName,
            badgeText = BankProvider.IBK.badgeText,
            bankProvider = BankProvider.IBK,
            defaultContentAccess = true
        ),
        FinancialAppInfo(
            packageName = KAKAO_BANKING_PACKAGE,
            displayName = BankProvider.KAKAO_BANK.displayName,
            badgeText = BankProvider.KAKAO_BANK.badgeText,
            bankProvider = BankProvider.KAKAO_BANK,
            defaultContentAccess = true
        )
    )
    private val appInfoByPackage = supportedAppInfos.associateBy { it.packageName }

    fun infoForPackage(packageName: String): FinancialAppInfo? =
        appInfoByPackage[packageName]

    fun allAppInfos(): List<FinancialAppInfo> =
        supportedAppInfos.toList()

    fun providerCandidateForPackage(packageName: String): BankProvider? =
        appInfoByPackage[packageName]?.bankProvider
}
