package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionEditAccountOptionsTest {
    @Test
    fun accountOptionsForEditUsesStableAccountIds() {
        assertThat(
            accountOptionsForEdit(
                accounts = listOf(
                    AssetAccount(id = 7, name = "Primary", balanceWon = 0),
                    AssetAccount(id = 9, name = "Savings", balanceWon = 0)
                ),
                linkedAssetAccountId = null,
                currentPaymentMethod = null
            )
        ).containsExactly(
            TransactionAccountOption(accountId = 7, label = "Primary"),
            TransactionAccountOption(accountId = 9, label = "Savings")
        ).inOrder()
    }

    @Test
    fun accountOptionsForEditKeepsLegacyNameWhenStableIdIsMissingOrInvalid() {
        assertThat(
            accountOptionsForEdit(
                accounts = listOf(
                    AssetAccount(id = 0, name = "Unsaved", balanceWon = 0),
                    AssetAccount(id = 9, name = "Savings", balanceWon = 0)
                ),
                linkedAssetAccountId = 7,
                currentPaymentMethod = "Legacy account"
            )
        ).containsExactly(
            TransactionAccountOption(accountId = null, label = "Legacy account"),
            TransactionAccountOption(accountId = 9, label = "Savings")
        ).inOrder()
    }
}
