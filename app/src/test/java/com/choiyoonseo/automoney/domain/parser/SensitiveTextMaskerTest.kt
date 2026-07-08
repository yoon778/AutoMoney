package com.choiyoonseo.automoney.domain.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SensitiveTextMaskerTest {
    @Test
    fun masksAccountLikeNumbersButKeepsAmounts() {
        val text = "KB 123456-78-901234 account 10,000 won payment"

        val masked = SensitiveTextMasker.mask(text)

        assertThat(masked).contains("****1234")
        assertThat(masked).contains("10,000 won")
        assertThat(masked.contains("123456-78-901234")).isFalse()
    }

    @Test
    fun keepsCommaFreeWonAmountVisible() {
        val masked = SensitiveTextMasker.mask("payment 100000 won completed")

        assertThat(masked).contains("100000 won")
        assertThat(masked.contains("100000")).isTrue()
    }

    @Test
    fun keepsCommaFreeWonCharacterAmountVisible() {
        val masked = SensitiveTextMasker.mask("payment 100000원 completed")

        assertThat(masked).contains("100000원")
        assertThat(masked.contains("100000")).isTrue()
    }

    @Test
    fun masksLongPlainNumbers() {
        val masked = SensitiveTextMasker.mask("sender 110123456789 sent 5,000 won")

        assertThat(masked).contains("****6789")
        assertThat(masked.contains("110123456789")).isFalse()
    }
}
