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
                title = "account 123456-78-901234",
                text = "account 123456-78-901234 10,000 won payment",
                bigText = null,
                postedAt = Instant.parse("2026-07-03T01:00:00Z")
            ),
            result = IngestionResult.Saved(TransactionType.EXPENSE, null),
            receivedAt = Instant.parse("2026-07-03T01:00:05Z")
        )

        assertThat(diagnostic.title).isEqualTo("account ****1234")
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
    fun errorDiagnosticDoesNotPersistRawMessage() {
        val diagnostic = LastNotificationDiagnostic.fromError(
            snapshot = tossSnapshot(),
            throwable = IllegalStateException("database closed"),
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        assertThat(diagnostic.result).isEqualTo(NotificationDiagnosticResult.ERROR)
        assertThat(diagnostic.message).isEqualTo("IllegalStateException")
        assertThat(diagnostic.parsedType).isNull()
    }

    @Test
    fun unverifiedDiagnosticStoresFixedPreviewOnly() {
        val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot().copy(packageName = "com.kbankwith.smartbank"),
            result = IngestionResult.Saved(
                TransactionType.EXPENSE,
                com.choiyoonseo.automoney.domain.model.ReviewReason.LOW_CONFIDENCE_CATEGORY
            ),
            sourceAccess = NotificationSourceAccess.SELECTED_UNVERIFIED,
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        assertThat(diagnostic.packageName).isEqualTo("com.kbankwith.smartbank")
        assertThat(diagnostic.title).isNull()
        assertThat(diagnostic.textPreview).isEqualTo("사용자 선택 앱 · 원문 미저장")
        assertThat(diagnostic.textPreview).doesNotContain("스타벅스")
        assertThat(diagnostic.message).isEqualTo("LOW_CONFIDENCE_CATEGORY")
        assertThat(diagnostic.parsedType).isEqualTo("EXPENSE")
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
    fun legacyUnsupportedDiagnosticMapReturnsNull() {
        val values = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Ignored("not parsed")
        ).toPreferenceMap().toMutableMap().apply {
            put("message", "unsupported package")
        }

        assertThat(lastNotificationDiagnosticFromPreferenceMap(values)).isNull()
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
