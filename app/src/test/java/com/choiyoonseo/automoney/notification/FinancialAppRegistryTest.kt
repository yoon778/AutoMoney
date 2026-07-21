package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.assets.BankProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
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
            "com.kbankwith.smartbank",
            "com.scbank.ma30",
            "kr.co.dgb.dgbm",
            "kr.co.busanbank.mbp",
            "kr.co.bnkbank.push.customer",
            "viva.republica.toss"
        )

        assertTrue(packages.all { FinancialAppRegistry.infoForPackage(it) != null })
    }

    @Test
    fun supportsVerifiedLegacyBankPackages() {
        val packages = listOf(
            "com.shinhan.smartcaremgr",
            "com.kebhana.hanapush"
        )

        assertTrue(packages.all { FinancialAppRegistry.infoForPackage(it) != null })
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
            "com.kakaobank.channel" to BankProvider.KAKAO_BANK,
            "com.kbankwith.smartbank" to BankProvider.K_BANK,
            "com.scbank.ma30" to BankProvider.SC,
            "kr.co.dgb.dgbm" to BankProvider.IM_BANK,
            "kr.co.busanbank.mbp" to BankProvider.BNK_BUSAN,
            "kr.co.bnkbank.push.customer" to BankProvider.BNK_BUSAN
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
        assertEquals(FinancialAppKind.AGGREGATOR, info?.kind)
        assertNull(FinancialAppRegistry.providerCandidateForPackage("viva.republica.toss"))
    }

    @Test
    fun rejectsUnknownAndBlankPackages() {
        assertNull(FinancialAppRegistry.infoForPackage("com.shopping.adapp"))
        assertNull(FinancialAppRegistry.infoForPackage(""))
    }

    @Test
    fun exposesUserFacingSourceInfoForSupportedApps() {
        assertEquals(
            FinancialAppInfo(
                packageName = "viva.republica.toss",
                displayName = "\ud1a0\uc2a4",
                badgeText = "T",
                bankProvider = null,
                aggregatesMultipleBanks = true,
                defaultContentAccess = true,
                kind = FinancialAppKind.AGGREGATOR
            ),
            FinancialAppRegistry.infoForPackage("viva.republica.toss")
        )
        assertEquals(
            FinancialAppInfo(
                packageName = "com.kbstar.kbbank",
                displayName = "\uad6d\ubbfc\uc740\ud589",
                badgeText = "KB",
                bankProvider = BankProvider.KB,
                defaultContentAccess = true
            ),
            FinancialAppRegistry.infoForPackage("com.kbstar.kbbank")
        )
        assertNull(FinancialAppRegistry.infoForPackage("com.shopping.adapp"))
    }

    @Test
    fun supportsMajorSecuritiesPackages() {
        val packages = listOf(
            "com.kiwoom.heromts",
            "com.miraeasset.trade",
            "com.samsungpop.android.mpop",
            "com.shinhaninvest.nsmts",
            "com.kbsec.mts.iplustarngm2",
            "com.wooriwm.txsmart",
            "com.truefriend.neosmartarenewal",
            "com.hanasec.stock"
        )

        packages.forEach { packageName ->
            val info = FinancialAppRegistry.infoForPackage(packageName)
            assertEquals(FinancialAppKind.SECURITIES, info?.kind)
            assertNull(info?.bankProvider)
        }
    }

    @Test
    fun exposesImmutableSnapshotOfAllRegisteredApps() {
        val first = FinancialAppRegistry.allAppInfos()
        val second = FinancialAppRegistry.allAppInfos()

        assertEquals(23, first.size)
        assertEquals(23, first.map { it.packageName }.distinct().size)
        assertTrue(first.all { it.defaultContentAccess })
        assertNotSame(first, second)
    }
}
