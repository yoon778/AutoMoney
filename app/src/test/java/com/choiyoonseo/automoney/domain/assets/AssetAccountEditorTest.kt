package com.choiyoonseo.automoney.domain.assets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssetAccountEditorTest {

    @Test
    fun updateAssetAccountKeepsIdAndAppliesEditedValues() {
        val original = AssetAccount(
            id = 12,
            name = "KB",
            balanceWon = 100_000,
            kind = AssetAccountKind.BANK
        )

        val updated = updateAssetAccount(
            account = original,
            name = "KB 생활비",
            balanceWon = 123_456,
            kind = AssetAccountKind.PAY
        )

        assertThat(updated.id).isEqualTo(12)
        assertThat(updated.name).isEqualTo("KB 생활비")
        assertThat(updated.balanceWon).isEqualTo(123_456)
        assertThat(updated.kind).isEqualTo(AssetAccountKind.PAY)
    }

    @Test
    fun updateAssetAccountRejectsBlankName() {
        val original = AssetAccount(id = 12, name = "KB", balanceWon = 100_000)

        val error = runCatching {
            updateAssetAccount(
                account = original,
                name = " ",
                balanceWon = 123_456,
                kind = AssetAccountKind.BANK
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun updateAssetAccountClearsMetadataWhenChangingToNonBank() {
        val original = AssetAccount(
            id = 12,
            name = "KB",
            balanceWon = 100_000,
            bankProvider = BankProvider.KB,
            accountLast4 = "9012"
        )

        val updated = updateAssetAccount(
            account = original,
            name = "Wallet",
            balanceWon = 123_456,
            kind = AssetAccountKind.PAY
        )

        assertThat(updated.bankProvider).isNull()
        assertThat(updated.accountLast4).isNull()
    }
}
