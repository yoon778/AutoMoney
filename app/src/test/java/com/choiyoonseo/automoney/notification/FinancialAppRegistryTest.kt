package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.assets.BankProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAppRegistryTest {
    @Test
    fun supportsCurrentMajorBankPackages() {
        val packages = listOf(
            "com.kbstar.kbbank",
            "com.shinhan.sbanking",
            "com.hanabank.oqf",
            "com.wooribank.smart.npib",
            "nh.smart.banking",
            "com.ibk.android.ionebank",
            "com.kakaobank.channel",
            "viva.republica.toss"
        )

        assertTrue(packages.all(FinancialAppRegistry::isSupportedPackage))
    }

    @Test
    fun supportsVerifiedLegacyBankPackages() {
        val packages = listOf(
            "com.shinhan.smartcaremgr",
            "com.kebhana.hanapush"
        )

        assertTrue(packages.all(FinancialAppRegistry::isSupportedPackage))
    }

    @Test
    fun mapsDedicatedAndLegacyPackagesToTheirProviders() {
        val expectedProviders = mapOf(
            "com.kbstar.kbbank" to BankProvider.KB,
            "com.shinhan.sbanking" to BankProvider.SHINHAN,
            "com.shinhan.smartcaremgr" to BankProvider.SHINHAN,
            "com.hanabank.oqf" to BankProvider.HANA,
            "com.kebhana.hanapush" to BankProvider.HANA,
            "com.wooribank.smart.npib" to BankProvider.WOORI,
            "nh.smart.banking" to BankProvider.NH,
            "com.ibk.android.ionebank" to BankProvider.IBK,
            "com.kakaobank.channel" to BankProvider.KAKAO_BANK
        )

        expectedProviders.forEach { (packageName, provider) ->
            assertEquals(provider, FinancialAppRegistry.providerCandidateForPackage(packageName))
            assertEquals(provider, FinancialAppRegistry.infoForPackage(packageName)?.bankProvider)
        }
    }

    @Test
    fun tossDoesNotClaimTossBankProvider() {
        val info = FinancialAppRegistry.infoForPackage("viva.republica.toss")

        assertNull(info?.bankProvider)
        assertTrue(info?.aggregatesMultipleBanks == true)
        assertNull(FinancialAppRegistry.providerCandidateForPackage("viva.republica.toss"))
    }

    @Test
    fun rejectsUnknownAndBlankPackages() {
        assertFalse(FinancialAppRegistry.isSupportedPackage("com.shopping.adapp"))
        assertFalse(FinancialAppRegistry.isSupportedPackage(""))
    }

    @Test
    fun exposesUserFacingSourceInfoForSupportedApps() {
        assertEquals(
            FinancialAppInfo(
                packageName = "viva.republica.toss",
                displayName = "\ud1a0\uc2a4",
                badgeText = "T",
                bankProvider = null,
                aggregatesMultipleBanks = true
            ),
            FinancialAppRegistry.infoForPackage("viva.republica.toss")
        )
        assertEquals(
            FinancialAppInfo(
                packageName = "com.kbstar.kbbank",
                displayName = "\uad6d\ubbfc\uc740\ud589",
                badgeText = "KB",
                bankProvider = BankProvider.KB
            ),
            FinancialAppRegistry.infoForPackage("com.kbstar.kbbank")
        )
        assertNull(FinancialAppRegistry.infoForPackage("com.shopping.adapp"))
    }
}
