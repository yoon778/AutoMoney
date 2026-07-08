package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class NotificationDiagnosticsStoreTest {
    @Test
    fun createsDiagnosticFromSavedIngestionResult() {
        val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Saved(TransactionType.EXPENSE, null),
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        assertThat(diagnostic.receivedAt).isEqualTo(Instant.parse("2026-07-02T03:00:05Z"))
        assertThat(diagnostic.postedAt).isEqualTo(Instant.parse("2026-07-02T03:00:00Z"))
        assertThat(diagnostic.packageName).isEqualTo("viva.republica.toss")
        assertThat(diagnostic.title).isEqualTo("토스 결제")
        assertThat(diagnostic.textPreview).isEqualTo("토스 결제\n스타벅스 6,100원 결제")
        assertThat(diagnostic.result).isEqualTo(NotificationDiagnosticResult.SAVED)
        assertThat(diagnostic.message).isEqualTo("저장됨")
        assertThat(diagnostic.parsedType).isEqualTo("EXPENSE")
    }

    @Test
    fun masksSensitiveNumbersInDiagnosticPreview() {
        val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = NotificationSnapshot(
                packageName = "com.kbstar.kbbank",
                title = "KB",
                text = "account 123456-78-901234 10,000 won payment",
                bigText = null,
                postedAt = Instant.parse("2026-07-03T01:00:00Z")
            ),
            result = IngestionResult.Saved(TransactionType.EXPENSE, null),
            receivedAt = Instant.parse("2026-07-03T01:00:05Z")
        )

        assertThat(diagnostic.textPreview).contains("****1234")
        assertThat(diagnostic.textPreview).contains("10,000 won")
        assertThat(diagnostic.textPreview.contains("123456-78-901234")).isFalse()
    }

    @Test
    fun mapsDuplicateAndIgnoredIngestionResults() {
        val duplicate = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Duplicate(TransactionType.EXPENSE),
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )
        val ignored = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Ignored("not parsed"),
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        assertThat(duplicate.result).isEqualTo(NotificationDiagnosticResult.DUPLICATE)
        assertThat(duplicate.message).isEqualTo("중복 알림")
        assertThat(duplicate.parsedType).isEqualTo("EXPENSE")
        assertThat(ignored.result).isEqualTo(NotificationDiagnosticResult.IGNORED)
        assertThat(ignored.message).isEqualTo("not parsed")
        assertThat(ignored.parsedType).isNull()
    }

    @Test
    fun createsErrorDiagnosticWithMessage() {
        val diagnostic = LastNotificationDiagnostic.fromError(
            snapshot = tossSnapshot(),
            throwable = IllegalStateException("database closed"),
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        assertThat(diagnostic.result).isEqualTo(NotificationDiagnosticResult.ERROR)
        assertThat(diagnostic.message).isEqualTo("database closed")
        assertThat(diagnostic.parsedType).isNull()
    }

    @Test
    fun preferenceMapRoundTripsDiagnostic() {
        val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Saved(TransactionType.EXPENSE, null),
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        val restored = lastNotificationDiagnosticFromPreferenceMap(diagnostic.toPreferenceMap())

        assertThat(restored).isEqualTo(diagnostic)
    }

    @Test
    fun corruptPreferenceMapReturnsNull() {
        val restored = lastNotificationDiagnosticFromPreferenceMap(
            mapOf(
                "receivedAt" to "not an instant",
                "postedAt" to "2026-07-02T03:00:00Z",
                "packageName" to "viva.republica.toss",
                "textPreview" to "text",
                "result" to "SAVED"
            )
        )

        assertThat(restored).isNull()
    }

    @Test
    fun diagnosticIncludesParsedTypeForSavedFinanceNotification() {
        val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = NotificationSnapshot(
                packageName = "com.kbstar.kbbank",
                title = "KB",
                text = "STARBUCKS 6,100 won payment",
                bigText = null,
                postedAt = Instant.parse("2026-07-03T01:00:00Z")
            ),
            result = IngestionResult.Saved(TransactionType.EXPENSE, null),
            receivedAt = Instant.parse("2026-07-03T01:00:05Z")
        )

        assertThat(diagnostic.packageName).isEqualTo("com.kbstar.kbbank")
        assertThat(diagnostic.parsedType).isEqualTo("EXPENSE")
    }

    private fun tossSnapshot(): NotificationSnapshot =
        NotificationSnapshot(
            packageName = "viva.republica.toss",
            title = "토스 결제",
            text = "스타벅스 6,100원 결제",
            bigText = null,
            postedAt = Instant.parse("2026-07-02T03:00:00Z")
        )
}
