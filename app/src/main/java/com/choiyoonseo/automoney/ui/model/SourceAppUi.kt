package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.notification.FinancialAppRegistry

data class SourceAppUi(
    val packageName: String,
    val displayName: String,
    val badgeText: String
)

fun sourceAppUiForPackage(packageName: String?): SourceAppUi? {
    val normalized = packageName?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val registered = FinancialAppRegistry.infoForPackage(normalized)
    return if (registered != null) {
        SourceAppUi(
            packageName = registered.packageName,
            displayName = registered.displayName,
            badgeText = registered.badgeText
        )
    } else {
        SourceAppUi(
            packageName = normalized,
            displayName = normalized.substringAfterLast('.').ifBlank { normalized },
            badgeText = "APP"
        )
    }
}
