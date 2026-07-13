package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationSourceAccessTest {
    private val policy = NotificationSourceAccessPolicy()

    @Test
    fun defaultTrustedPackageIsTrusted() {
        assertThat(policy.accessFor(FinancialAppRegistry.KAKAO_BANKING_PACKAGE))
            .isEqualTo(NotificationSourceAccess.TRUSTED)
    }

    @Test
    fun futureRegisteredPackageRequiresSelectionAndRemainsUnverified() {
        val futureInfo = FinancialAppInfo(
            packageName = "com.future.bank",
            displayName = "Future Bank",
            badgeText = "F",
            bankProvider = null
        )
        val futurePolicy = NotificationSourceAccessPolicy { packageName ->
            futureInfo.takeIf { it.packageName == packageName }
        }

        assertThat(futurePolicy.accessFor(futureInfo.packageName))
            .isEqualTo(NotificationSourceAccess.BLOCKED)
        assertThat(
            futurePolicy.accessFor(
                packageName = futureInfo.packageName,
                explicitlyEnabledPackages = setOf(futureInfo.packageName)
            )
        ).isEqualTo(NotificationSourceAccess.SELECTED_UNVERIFIED)
    }

    @Test
    fun explicitDisableWinsForTrustedPackage() {
        val packageName = FinancialAppRegistry.KB_STAR_BANKING_PACKAGE

        assertThat(
            policy.accessFor(
                packageName = packageName,
                explicitlyEnabledPackages = setOf(packageName),
                explicitlyDisabledPackages = setOf(packageName)
            )
        ).isEqualTo(NotificationSourceAccess.BLOCKED)
    }

    @Test
    fun unknownPackageIsBlockedUntilExplicitlyEnabled() {
        val packageName = "com.kbankwith.smartbank"

        assertThat(policy.accessFor(packageName)).isEqualTo(NotificationSourceAccess.BLOCKED)
        assertThat(
            policy.accessFor(
                packageName = packageName,
                explicitlyEnabledPackages = setOf(packageName)
            )
        ).isEqualTo(NotificationSourceAccess.SELECTED_UNVERIFIED)
    }

    @Test
    fun exactPackageMatchingDoesNotTrustNearMatch() {
        val trusted = FinancialAppRegistry.KAKAO_BANKING_PACKAGE

        assertThat(policy.accessFor("$trusted.fake"))
            .isEqualTo(NotificationSourceAccess.BLOCKED)
    }
}
