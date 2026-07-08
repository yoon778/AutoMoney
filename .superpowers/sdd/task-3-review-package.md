# Task 3 Review Package - Updated After Fix

Repository note: no HEAD commit exists; package contains current contents of task-owned files.
Review finding under re-check: amount-like text without commas should remain visible while account-like numbers are masked.

## app\src\main\java\com\choiyoonseo\automoney\domain\parser\SensitiveTextMasker.kt
```kotlin
package com.choiyoonseo.automoney.domain.parser

object SensitiveTextMasker {
    private val sensitiveNumberPattern = Regex("""\b\d[\d-]{4,}\d\b""")
    private val amountPattern = Regex("""\b\d{6,}\s*(?:won|??""", RegexOption.IGNORE_CASE)

    fun mask(text: String): String {
        val protectedAmounts = mutableListOf<String>()
        val protectedText = amountPattern.replace(text) { match ->
            val token = "__AMOUNT_${protectedAmounts.size}__"
            protectedAmounts += match.value
            token
        }

        val maskedText = sensitiveNumberPattern.replace(protectedText) { match ->
            val digits = match.value.filter(Char::isDigit)
            if (digits.length < 6) {
                match.value
            } else {
                "****" + digits.takeLast(4)
            }
        }

        return protectedAmounts.foldIndexed(maskedText) { index, current, amount ->
            current.replace("__AMOUNT_${index}__", amount)
        }
    }
}
```

## app\src\main\java\com\choiyoonseo\automoney\notification\NotificationDiagnosticsStore.kt
```kotlin
package com.choiyoonseo.automoney.notification

import android.content.Context
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.SensitiveTextMasker
import java.time.Instant

enum class NotificationDiagnosticResult {
    SAVED,
    DUPLICATE,
    IGNORED,
    ERROR
}

data class LastNotificationDiagnostic(
    val receivedAt: Instant,
    val postedAt: Instant,
    val packageName: String,
    val title: String?,
    val textPreview: String,
    val result: NotificationDiagnosticResult,
    val message: String?
) {
    companion object {
        fun fromIngestionResult(
            snapshot: NotificationSnapshot,
            result: IngestionResult,
            receivedAt: Instant = Instant.now()
        ): LastNotificationDiagnostic {
            val diagnosticResult = when (result) {
                IngestionResult.Saved -> NotificationDiagnosticResult.SAVED
                IngestionResult.Duplicate -> NotificationDiagnosticResult.DUPLICATE
                IngestionResult.Ignored -> NotificationDiagnosticResult.IGNORED
            }
            val message = when (result) {
                IngestionResult.Saved -> "???貫留?
                IngestionResult.Duplicate -> "餓λ쵎??
                IngestionResult.Ignored -> "?얜똻???"
            }
            return fromSnapshot(
                snapshot = snapshot,
                receivedAt = receivedAt,
                result = diagnosticResult,
                message = message
            )
        }

        fun fromError(
            snapshot: NotificationSnapshot,
            throwable: Throwable,
            receivedAt: Instant = Instant.now()
        ): LastNotificationDiagnostic =
            fromSnapshot(
                snapshot = snapshot,
                receivedAt = receivedAt,
                result = NotificationDiagnosticResult.ERROR,
                message = throwable.message ?: throwable::class.simpleName ?: "??살첒"
            )

        private fun fromSnapshot(
            snapshot: NotificationSnapshot,
            receivedAt: Instant,
            result: NotificationDiagnosticResult,
            message: String
        ): LastNotificationDiagnostic =
            LastNotificationDiagnostic(
                receivedAt = receivedAt,
                postedAt = snapshot.postedAt,
                packageName = snapshot.packageName,
                title = snapshot.title,
                textPreview = snapshot.textPreview(),
                result = result,
                message = message
            )
    }
}

class NotificationDiagnosticsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(diagnostic: LastNotificationDiagnostic) {
        val values = diagnostic.toPreferenceMap()
        preferences.edit()
            .clear()
            .apply {
                values.forEach { (key, value) -> putString(key, value) }
            }
            .apply()
    }

    fun load(): LastNotificationDiagnostic? =
        lastNotificationDiagnosticFromPreferenceMap(
            mapOf(
                KEY_RECEIVED_AT to preferences.getString(KEY_RECEIVED_AT, null),
                KEY_POSTED_AT to preferences.getString(KEY_POSTED_AT, null),
                KEY_PACKAGE_NAME to preferences.getString(KEY_PACKAGE_NAME, null),
                KEY_TITLE to preferences.getString(KEY_TITLE, null),
                KEY_TEXT_PREVIEW to preferences.getString(KEY_TEXT_PREVIEW, null),
                KEY_RESULT to preferences.getString(KEY_RESULT, null),
                KEY_MESSAGE to preferences.getString(KEY_MESSAGE, null)
            )
        )

    fun clear() {
        preferences.edit().clear().apply()
    }
}

internal fun LastNotificationDiagnostic.toPreferenceMap(): Map<String, String> =
    buildMap {
        put(KEY_RECEIVED_AT, receivedAt.toString())
        put(KEY_POSTED_AT, postedAt.toString())
        put(KEY_PACKAGE_NAME, packageName)
        title?.let { put(KEY_TITLE, it) }
        put(KEY_TEXT_PREVIEW, textPreview)
        put(KEY_RESULT, result.name)
        message?.let { put(KEY_MESSAGE, it) }
    }

internal fun lastNotificationDiagnosticFromPreferenceMap(
    values: Map<String, String?>
): LastNotificationDiagnostic? {
    return try {
        val receivedAt = Instant.parse(values[KEY_RECEIVED_AT] ?: return null)
        val postedAt = Instant.parse(values[KEY_POSTED_AT] ?: return null)
        val packageName = values[KEY_PACKAGE_NAME]?.takeIf { it.isNotBlank() } ?: return null
        val textPreview = values[KEY_TEXT_PREVIEW]?.takeIf { it.isNotBlank() } ?: return null
        val result = NotificationDiagnosticResult.valueOf(values[KEY_RESULT] ?: return null)

        LastNotificationDiagnostic(
            receivedAt = receivedAt,
            postedAt = postedAt,
            packageName = packageName,
            title = values[KEY_TITLE]?.takeIf { it.isNotBlank() },
            textPreview = textPreview,
            result = result,
            message = values[KEY_MESSAGE]?.takeIf { it.isNotBlank() }
        )
    } catch (e: RuntimeException) {
        null
    }
}

private fun NotificationSnapshot.textPreview(): String {
    val rawPreview = listOfNotNull(title, text, bigText)
        .flatMap { it.lineSequence().map(String::trim).filter(String::isNotBlank).toList() }
        .distinct()
        .joinToString("\n")
        .take(MAX_TEXT_PREVIEW_LENGTH)

    return SensitiveTextMasker.mask(rawPreview)
}

private const val PREFERENCES_NAME = "notification_diagnostics"
private const val KEY_RECEIVED_AT = "receivedAt"
private const val KEY_POSTED_AT = "postedAt"
private const val KEY_PACKAGE_NAME = "packageName"
private const val KEY_TITLE = "title"
private const val KEY_TEXT_PREVIEW = "textPreview"
private const val KEY_RESULT = "result"
private const val KEY_MESSAGE = "message"
private const val MAX_TEXT_PREVIEW_LENGTH = 160
```

## app\src\test\java\com\choiyoonseo\automoney\domain\parser\SensitiveTextMaskerTest.kt
```kotlin
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
        val masked = SensitiveTextMasker.mask("payment 100000??completed")

        assertThat(masked).contains("100000??)
        assertThat(masked.contains("100000")).isTrue()
    }

    @Test
    fun masksLongPlainNumbers() {
        val masked = SensitiveTextMasker.mask("sender 110123456789 sent 5,000 won")

        assertThat(masked).contains("****6789")
        assertThat(masked.contains("110123456789")).isFalse()
    }
}
```

## app\src\test\java\com\choiyoonseo\automoney\notification\NotificationDiagnosticsStoreTest.kt
```kotlin
package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class NotificationDiagnosticsStoreTest {
    @Test
    fun createsDiagnosticFromSavedIngestionResult() {
        val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Saved,
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        assertThat(diagnostic.receivedAt).isEqualTo(Instant.parse("2026-07-02T03:00:05Z"))
        assertThat(diagnostic.postedAt).isEqualTo(Instant.parse("2026-07-02T03:00:00Z"))
        assertThat(diagnostic.packageName).isEqualTo("viva.republica.toss")
        assertThat(diagnostic.title).isEqualTo("?醫롫뮞獄?굟寃?筌ｋ똾寃뺟㎉?諭?)
        assertThat(diagnostic.textPreview).isEqualTo("?醫롫뮞獄?굟寃?筌ｋ똾寃뺟㎉?諭?n???甕곕굞???????껊럡 6,100??野껉퀣??)
        assertThat(diagnostic.result).isEqualTo(NotificationDiagnosticResult.SAVED)
        assertThat(diagnostic.message).isEqualTo("???貫留?)
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
            result = IngestionResult.Saved,
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
            result = IngestionResult.Duplicate,
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )
        val ignored = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Ignored,
            receivedAt = Instant.parse("2026-07-02T03:00:05Z")
        )

        assertThat(duplicate.result).isEqualTo(NotificationDiagnosticResult.DUPLICATE)
        assertThat(duplicate.message).isEqualTo("餓λ쵎??)
        assertThat(ignored.result).isEqualTo(NotificationDiagnosticResult.IGNORED)
        assertThat(ignored.message).isEqualTo("?얜똻???")
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
    }

    @Test
    fun preferenceMapRoundTripsDiagnostic() {
        val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
            snapshot = tossSnapshot(),
            result = IngestionResult.Saved,
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

    private fun tossSnapshot(): NotificationSnapshot =
        NotificationSnapshot(
            packageName = "viva.republica.toss",
            title = "?醫롫뮞獄?굟寃?筌ｋ똾寃뺟㎉?諭?,
            text = "???甕곕굞???????껊럡 6,100??野껉퀣??,
            bigText = null,
            postedAt = Instant.parse("2026-07-02T03:00:00Z")
        )
}
```

