package com.choiyoonseo.automoney.ui.assets

import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.AssetAccountKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.choiyoonseo.automoney.domain.assets.maskedAccountLast4
import com.choiyoonseo.automoney.domain.assets.normalizeAccountLast4
import com.choiyoonseo.automoney.domain.assets.validatedForSave

fun createAssetAccountFromForm(
    name: String,
    balanceWon: Long,
    kind: AssetAccountKind,
    bankProvider: BankProvider?,
    accountNumberInput: String,
    providerLabel: String? = null
): AssetAccount {
    val provider = bankProvider.takeIf { kind == AssetAccountKind.BANK }
    val last4 = provider?.let { normalizeAccountLast4(accountNumberInput) }
    return AssetAccount(
        name = name,
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = provider,
        accountLast4 = last4,
        providerLabel = providerLabel
    ).validatedForSave()
}

fun updateAssetAccountFromForm(
    account: AssetAccount,
    name: String,
    balanceWon: Long,
    kind: AssetAccountKind,
    bankProvider: BankProvider?,
    accountNumberInput: String,
    providerLabel: String? = null
): AssetAccount {
    val provider = bankProvider.takeIf { kind == AssetAccountKind.BANK }
    val last4 = when {
        provider == null -> null
        accountNumberInput.isNotBlank() -> normalizeAccountLast4(accountNumberInput)
        provider == account.bankProvider -> account.accountLast4
        else -> null
    }
    return account.copy(
        name = name,
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = provider,
        accountLast4 = last4,
        providerLabel = providerLabel ?: account.providerLabel
    ).validatedForSave()
}

fun assetAccountMetadataLabel(account: AssetAccount): String =
    listOfNotNull(
        account.kind.label,
        account.bankProvider?.displayName ?: account.providerLabel,
        maskedAccountLast4(account.accountLast4)
    ).joinToString(" · ")
