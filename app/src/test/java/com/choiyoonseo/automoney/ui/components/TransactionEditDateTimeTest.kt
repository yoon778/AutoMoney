package com.choiyoonseo.automoney.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class TransactionEditDateTimeTest {
    @Test
    fun instantSplitsToSeoulLocalDateAndTime() {
        val instant = Instant.parse("2026-07-01T15:30:00Z")

        assertThat(instant.toTransactionEditLocalDate()).isEqualTo(LocalDate.of(2026, 7, 2))
        assertThat(instant.toTransactionEditLocalTime()).isEqualTo(LocalTime.of(0, 30))
    }

    @Test
    fun localDateAndTimeMergeToInstantInSeoulZone() {
        val occurredAt = LocalDate.of(2026, 7, 2)
            .toTransactionEditInstant(LocalTime.of(14, 30, 45))

        assertThat(occurredAt).isEqualTo(Instant.parse("2026-07-02T05:30:00Z"))
    }

    @Test
    fun datePickerMillisRoundTripsAsLocalDate() {
        val selectedDate = LocalDate.of(2026, 7, 2)

        assertThat(selectedDate.toTransactionEditDatePickerMillis().toTransactionEditDatePickerLocalDate())
            .isEqualTo(selectedDate)
    }
}
