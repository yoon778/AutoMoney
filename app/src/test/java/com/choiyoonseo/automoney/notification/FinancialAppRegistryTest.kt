package com.choiyoonseo.automoney.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAppRegistryTest {
    @Test
    fun supportsTossAndKbStarBanking() {
        assertTrue(FinancialAppRegistry.isSupportedPackage("viva.republica.toss"))
        assertTrue(FinancialAppRegistry.isSupportedPackage("com.kbstar.kbbank"))
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
                badgeText = "T"
            ),
            FinancialAppRegistry.infoForPackage("viva.republica.toss")
        )
        assertEquals(
            FinancialAppInfo(
                packageName = "com.kbstar.kbbank",
                displayName = "\uad6d\ubbfc\uc740\ud589",
                badgeText = "KB"
            ),
            FinancialAppRegistry.infoForPackage("com.kbstar.kbbank")
        )
        assertNull(FinancialAppRegistry.infoForPackage("com.shopping.adapp"))
    }
}
