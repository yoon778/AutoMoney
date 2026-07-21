package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.assets.BankProvider

enum class FinancialAppKind {
    BANK,
    AGGREGATOR,
    SECURITIES
}

data class FinancialAppInfo(
    val packageName: String,
    val displayName: String,
    val badgeText: String,
    val bankProvider: BankProvider?,
    val aggregatesMultipleBanks: Boolean = false,
    val defaultContentAccess: Boolean = false,
    val kind: FinancialAppKind = FinancialAppKind.BANK
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
    const val K_BANKING_PACKAGE = "com.kbankwith.smartbank"
    const val SC_BANKING_PACKAGE = "com.scbank.ma30"
    const val IM_BANKING_PACKAGE = "kr.co.dgb.dgbm"
    const val BNK_BUSAN_BANKING_PACKAGE = "kr.co.busanbank.mbp"
    const val BNK_BUSAN_PUSH_PACKAGE = "kr.co.bnkbank.push.customer"

    const val KIWOOM_SECURITIES_PACKAGE = "com.kiwoom.heromts"
    const val MIRAE_SECURITIES_PACKAGE = "com.miraeasset.trade"
    const val SAMSUNG_SECURITIES_PACKAGE = "com.samsungpop.android.mpop"
    const val SHINHAN_SECURITIES_PACKAGE = "com.shinhaninvest.nsmts"
    const val KB_SECURITIES_PACKAGE = "com.kbsec.mts.iplustarngm2"
    const val NH_SECURITIES_PACKAGE = "com.wooriwm.txsmart"
    const val KOREA_INVESTMENT_PACKAGE = "com.truefriend.neosmartarenewal"
    const val HANA_SECURITIES_PACKAGE = "com.hanasec.stock"

    private val supportedAppInfos = listOf(
        FinancialAppInfo(
            packageName = TOSS_PACKAGE,
            displayName = "\ud1a0\uc2a4",
            badgeText = "T",
            bankProvider = null,
            aggregatesMultipleBanks = true,
            defaultContentAccess = true,
            kind = FinancialAppKind.AGGREGATOR
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
        ),
        bankApp(K_BANKING_PACKAGE, BankProvider.K_BANK),
        bankApp(SC_BANKING_PACKAGE, BankProvider.SC),
        bankApp(IM_BANKING_PACKAGE, BankProvider.IM_BANK),
        bankApp(BNK_BUSAN_BANKING_PACKAGE, BankProvider.BNK_BUSAN),
        bankApp(BNK_BUSAN_PUSH_PACKAGE, BankProvider.BNK_BUSAN),
        securitiesApp(KIWOOM_SECURITIES_PACKAGE, "키움증권", "키움"),
        securitiesApp(MIRAE_SECURITIES_PACKAGE, "미래에셋증권", "미래"),
        securitiesApp(SAMSUNG_SECURITIES_PACKAGE, "삼성증권", "삼성"),
        securitiesApp(SHINHAN_SECURITIES_PACKAGE, "신한투자증권", "신한"),
        securitiesApp(KB_SECURITIES_PACKAGE, "KB증권", "KB"),
        securitiesApp(NH_SECURITIES_PACKAGE, "NH투자증권", "NH"),
        securitiesApp(KOREA_INVESTMENT_PACKAGE, "한국투자증권", "한투"),
        securitiesApp(HANA_SECURITIES_PACKAGE, "하나증권", "하나")
    )

    private fun bankApp(packageName: String, provider: BankProvider) =
        FinancialAppInfo(
            packageName = packageName,
            displayName = provider.displayName,
            badgeText = provider.badgeText,
            bankProvider = provider,
            defaultContentAccess = true
        )

    private fun securitiesApp(packageName: String, displayName: String, badgeText: String) =
        FinancialAppInfo(
            packageName = packageName,
            displayName = displayName,
            badgeText = badgeText,
            bankProvider = null,
            defaultContentAccess = true,
            kind = FinancialAppKind.SECURITIES
        )

    private val appInfoByPackage = supportedAppInfos.associateBy { it.packageName }

    fun infoForPackage(packageName: String): FinancialAppInfo? =
        appInfoByPackage[packageName]

    fun allAppInfos(): List<FinancialAppInfo> =
        supportedAppInfos.toList()

    fun providerCandidateForPackage(packageName: String): BankProvider? =
        appInfoByPackage[packageName]?.bankProvider
}
