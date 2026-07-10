package com.choiyoonseo.automoney.ui.assets

import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.AssetAccountKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssetAccountFormTest {

    @Test
    fun newAccountStoresOnlySuffix() {
        val account = createAssetAccountFromForm(
            name = "KB savings",
            balanceWon = 100_000,
            kind = AssetAccountKind.BANK,
            bankProvider = BankProvider.KB,
            accountNumberInput = "123-456-789012"
        )

        assertThat(account.bankProvider).isEqualTo(BankProvider.KB)
        assertThat(account.accountLast4).isEqualTo("9012")
        assertThat(account.toString()).doesNotContain("123-456-789012")
    }

    @Test
    fun blankEditInputPreservesSuffix() {
        val original = AssetAccount(
            id = 1,
            name = "KB savings",
            balanceWon = 100_000,
            bankProvider = BankProvider.KB,
            accountLast4 = "9012"
        )

        val updated = updateAssetAccountFromForm(
            account = original,
            name = "KB main",
            balanceWon = 90_000,
            kind = AssetAccountKind.BANK,
            bankProvider = BankProvider.KB,
            accountNumberInput = ""
        )

        assertThat(updated.accountLast4).isEqualTo("9012")
    }

    @Test
    fun bankAccountWithoutMetadataRemainsValid() {
        val account = createAssetAccountFromForm(
            name = "Legacy bank",
            balanceWon = 100_000,
            kind = AssetAccountKind.BANK,
            bankProvider = null,
            accountNumberInput = ""
        )

        assertThat(account.bankProvider).isNull()
        assertThat(account.accountLast4).isNull()
    }
}
