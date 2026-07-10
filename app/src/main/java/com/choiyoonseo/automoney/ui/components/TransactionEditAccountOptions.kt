package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.assets.AssetAccount

data class TransactionAccountOption(
    val accountId: Long?,
    val label: String
)

fun accountOptionsForEdit(
    accounts: List<AssetAccount>,
    linkedAssetAccountId: Long?,
    currentPaymentMethod: String?
): List<TransactionAccountOption> {
    val registered = accounts
        .filter { it.id > 0 }
        .map { account -> TransactionAccountOption(account.id, account.name) }
    val current = currentPaymentMethod?.trim()?.takeIf { it.isNotBlank() }
    val linkedAccountExists = linkedAssetAccountId != null &&
        registered.any { it.accountId == linkedAssetAccountId }

    return if (linkedAccountExists || current == null) {
        registered
    } else {
        listOf(TransactionAccountOption(accountId = null, label = current)) + registered
    }
}

fun selectedAccountOptionForEdit(
    options: List<TransactionAccountOption>,
    linkedAssetAccountId: Long?
): TransactionAccountOption? =
    options.firstOrNull { it.accountId == linkedAssetAccountId && linkedAssetAccountId != null }
        ?: options.firstOrNull { it.accountId == null }
        ?: options.firstOrNull()
