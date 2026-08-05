package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionEditTypeOptionsTest {
    @Test
    fun transactionEditTypeOptionsExposeUserFacingTypesWithoutWalletTopup() {
        assertThat(transactionEditTypeOptions.map { it.type }).containsExactly(
            TransactionType.EXPENSE,
            TransactionType.SPECIAL_EXPENSE,
            TransactionType.INCOME,
            TransactionType.SAVING,
            TransactionType.TRANSFER,
            TransactionType.SETTLEMENT,
            TransactionType.EXCLUDED
        ).inOrder()
    }

    @Test
    fun typeLabelForEditUsesFriendlyLabel() {
        assertThat(typeLabelForEdit(TransactionType.EXPENSE)).isEqualTo("지출")
        assertThat(typeLabelForEdit(TransactionType.SPECIAL_EXPENSE)).isEqualTo("특별지출")
        assertThat(typeLabelForEdit(TransactionType.SAVING)).isEqualTo("저축")
        assertThat(typeLabelForEdit(TransactionType.TRANSFER)).isEqualTo("계좌 이동/송금")
        assertThat(transactionEditTypeOptions.map { it.type }).doesNotContain(TransactionType.WALLET_TOPUP)
    }
}
