package com.choiyoonseo.automoney.ui.components

import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionEditTypeOptionsTest {
    @Test
    fun transactionEditTypeOptionsExposeReviewFriendlyTypes() {
        assertThat(transactionEditTypeOptions.map { it.type }).containsExactly(
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            TransactionType.TRANSFER,
            TransactionType.WALLET_TOPUP,
            TransactionType.SETTLEMENT,
            TransactionType.EXCLUDED
        ).inOrder()
    }

    @Test
    fun typeLabelForEditUsesFriendlyLabel() {
        assertThat(typeLabelForEdit(TransactionType.EXPENSE)).isEqualTo("지출")
        assertThat(typeLabelForEdit(TransactionType.TRANSFER)).isEqualTo("계좌 이동/송금")
        assertThat(typeLabelForEdit(TransactionType.WALLET_TOPUP)).isEqualTo("충전/포인트")
    }
}
