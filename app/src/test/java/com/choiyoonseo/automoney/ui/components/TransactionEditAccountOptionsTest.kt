package com.choiyoonseo.automoney.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionEditAccountOptionsTest {
    @Test
    fun accountOptionsForEditUsesRegisteredAccounts() {
        assertThat(
            accountOptionsForEdit(
                accountNames = listOf("국민은행 통장", "토스뱅크 통장"),
                currentPaymentMethod = null
            )
        ).containsExactly("국민은행 통장", "토스뱅크 통장").inOrder()
    }

    @Test
    fun accountOptionsForEditKeepsCurrentPaymentMethodWhenNotRegistered() {
        assertThat(
            accountOptionsForEdit(
                accountNames = listOf("토스뱅크 통장"),
                currentPaymentMethod = "KB"
            )
        ).containsExactly("KB", "토스뱅크 통장").inOrder()
    }

    @Test
    fun accountLabelForEditSelectsCurrentOrFirstAccount() {
        assertThat(
            accountLabelForEdit(
                paymentMethod = "국민은행 통장",
                accountNames = listOf("국민은행 통장", "토스뱅크 통장")
            )
        ).isEqualTo("국민은행 통장")

        assertThat(
            accountLabelForEdit(
                paymentMethod = null,
                accountNames = listOf("국민은행 통장", "토스뱅크 통장")
            )
        ).isEqualTo("국민은행 통장")
    }
}
