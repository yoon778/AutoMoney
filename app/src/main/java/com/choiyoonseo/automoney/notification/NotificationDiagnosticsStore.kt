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
    val message: String?,
    val parsedType: String?
) {
    companion object {
        fun fromIngestionResult(
            snapshot: NotificationSnapshot,
            result: IngestionResult,
            sourceAccess: NotificationSourceAccess = NotificationSourceAccess.TRUSTED,
            receivedAt: Instant = Instant.now()
        ): LastNotificationDiagnostic {
            val diagnosticResult = when (result) {
                is IngestionResult.Saved -> NotificationDiagnosticResult.SAVED
                is IngestionResult.Duplicate -> NotificationDiagnosticResult.DUPLICATE
                is IngestionResult.Ignored -> NotificationDiagnosticResult.IGNORED
            }
            val parsedType = when (result) {
                is IngestionResult.Saved -> result.transactionType.name
                is IngestionResult.Duplicate -> result.transactionType?.name
                is IngestionResult.Ignored -> null
            }
            val message = if (sourceAccess == NotificationSourceAccess.SELECTED_UNVERIFIED) {
                when (result) {
                    is IngestionResult.Saved -> result.reviewReason?.name ?: "REVIEW_REQUIRED"
                    is IngestionResult.Duplicate -> "DUPLICATE"
                    is IngestionResult.Ignored -> "IGNORED"
                }
            } else {
                when (result) {
                    is IngestionResult.Saved -> "저장됨"
                    is IngestionResult.Duplicate -> "중복 알림"
                    is IngestionResult.Ignored -> result.reason
                }
            }
            return fromSnapshot(
                snapshot = snapshot,
                receivedAt = receivedAt,
                result = diagnosticResult,
                message = message,
                parsedType = parsedType,
                sourceAccess = sourceAccess
            )
        }

        fun fromError(
            snapshot: NotificationSnapshot,
            throwable: Throwable,
            sourceAccess: NotificationSourceAccess = NotificationSourceAccess.TRUSTED,
            receivedAt: Instant = Instant.now()
        ): LastNotificationDiagnostic =
            fromSnapshot(
                snapshot = snapshot,
                receivedAt = receivedAt,
                result = NotificationDiagnosticResult.ERROR,
                message = throwable::class.simpleName ?: "RUNTIME_ERROR",
                parsedType = null,
                sourceAccess = sourceAccess
            )

        private fun fromSnapshot(
            snapshot: NotificationSnapshot,
            receivedAt: Instant,
            result: NotificationDiagnosticResult,
            message: String?,
            parsedType: String?,
            sourceAccess: NotificationSourceAccess
        ): LastNotificationDiagnostic {
            val unverified = sourceAccess == NotificationSourceAccess.SELECTED_UNVERIFIED
            return LastNotificationDiagnostic(
                receivedAt = receivedAt,
                postedAt = snapshot.postedAt,
                packageName = snapshot.packageName,
                title = if (unverified) null else snapshot.title?.let(SensitiveTextMasker::mask),
                textPreview = if (unverified) UNVERIFIED_TEXT_PREVIEW else snapshot.textPreview(),
                result = result,
                message = message,
                parsedType = parsedType
            )
        }
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

    fun load(): LastNotificationDiagnostic? {
        val values = mapOf(
                KEY_RECEIVED_AT to preferences.getString(KEY_RECEIVED_AT, null),
                KEY_POSTED_AT to preferences.getString(KEY_POSTED_AT, null),
                KEY_PACKAGE_NAME to preferences.getString(KEY_PACKAGE_NAME, null),
                KEY_TITLE to preferences.getString(KEY_TITLE, null),
                KEY_TEXT_PREVIEW to preferences.getString(KEY_TEXT_PREVIEW, null),
                KEY_RESULT to preferences.getString(KEY_RESULT, null),
                KEY_MESSAGE to preferences.getString(KEY_MESSAGE, null),
                KEY_PARSED_TYPE to preferences.getString(KEY_PARSED_TYPE, null)
            )
        if (values[KEY_MESSAGE] == LEGACY_UNSUPPORTED_MESSAGE) {
            clear()
            return null
        }
        return lastNotificationDiagnosticFromPreferenceMap(values)
    }

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
        parsedType?.let { put(KEY_PARSED_TYPE, it) }
    }

internal fun lastNotificationDiagnosticFromPreferenceMap(
    values: Map<String, String?>
): LastNotificationDiagnostic? {
    return try {
        if (values[KEY_MESSAGE] == LEGACY_UNSUPPORTED_MESSAGE) return null
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
            message = values[KEY_MESSAGE]?.takeIf { it.isNotBlank() },
            parsedType = values[KEY_PARSED_TYPE]?.takeIf { it.isNotBlank() }
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
private const val KEY_PARSED_TYPE = "parsedType"
private const val MAX_TEXT_PREVIEW_LENGTH = 160
private const val LEGACY_UNSUPPORTED_MESSAGE = "unsupported package"
private const val UNVERIFIED_TEXT_PREVIEW = "사용자 선택 앱 · 원문 미저장"
