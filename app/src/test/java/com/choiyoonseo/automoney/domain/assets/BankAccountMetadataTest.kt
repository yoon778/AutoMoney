package com.choiyoonseo.automoney.domain.assets

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BankAccountMetadataTest {
    @Test
    fun keepsOnlyLastFourDigits() {
        assertThat(normalizeAccountLast4("123-456-789012")).isEqualTo("9012")
    }

    @Test
    fun rejectsShortInput() {
        assertThrows(IllegalArgumentException::class.java) {
            normalizeAccountLast4("123")
        }
    }

    @Test
    fun masksStoredSuffix() {
        assertThat(maskedAccountLast4("9012")).isEqualTo("****9012")
    }
}
