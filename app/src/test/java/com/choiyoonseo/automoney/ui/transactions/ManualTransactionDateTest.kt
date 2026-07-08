package com.choiyoonseo.automoney.ui.transactions

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ManualTransactionDateTest {
    @Test
    fun selectedDateConvertsToSeoulStartOfDayInstant() {
        val occurredAt = LocalDate.of(2026, 7, 2).toManualTransactionInstant()

        assertThat(occurredAt).isEqualTo(Instant.parse("2026-07-01T15:00:00Z"))
    }

    @Test
    fun selectedDateFormatsAsShortKoreanLabel() {
        val label = LocalDate.of(2026, 7, 2).toManualTransactionDateLabel()

        assertThat(label).isEqualTo("7월 2일")
    }

    @Test
    fun datePickerMillisRoundTripsAsLocalDate() {
        val selectedDate = LocalDate.of(2026, 7, 2)

        assertThat(selectedDate.toDatePickerMillis().toDatePickerLocalDate()).isEqualTo(selectedDate)
    }
}
