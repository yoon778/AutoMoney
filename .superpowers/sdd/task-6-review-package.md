# Task 6 Review Package - Updated After Fix

Repository note: no HEAD commit exists; package contains current contents of task-owned files.
Review finding under re-check: diagnostics card must show source/package in non-empty state.

## app\src\main\java\com\choiyoonseo\automoney\notification\NotificationIngestionUseCase.kt
```kotlin
package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.choiyoonseo.automoney.domain.parser.NotificationParser
import com.choiyoonseo.automoney.domain.parser.ParseResult
import com.choiyoonseo.automoney.domain.parser.TransactionDraft
import com.choiyoonseo.automoney.domain.rules.CategorizationEngine
import com.choiyoonseo.automoney.domain.rules.DuplicateDecision
import com.choiyoonseo.automoney.domain.rules.DuplicateDetector

class NotificationIngestionUseCase(
    private val parser: NotificationParser,
    private val categorizationEngine: CategorizationEngine,
    private val duplicateDetector: DuplicateDetector,
    private val repository: MoneyRepository
) {
    suspend fun ingest(snapshot: NotificationSnapshot): IngestionResult {
        val parsed = parser.parse(snapshot)
        if (parsed !is ParseResult.Parsed) {
            val reason = (parsed as? ParseResult.Ignored)?.reason ?: "not parsed"
            return IngestionResult.Ignored(reason)
        }

        val withRules = categorizationEngine.applyRules(parsed.draft, repository.enabledRules())
        val duplicateDecision = duplicateDetector.detect(
            candidate = withRules,
            existing = repository.recentNotificationTransactions(limit = 50)
        )

        if (duplicateDecision == DuplicateDecision.DUPLICATE) {
            return IngestionResult.Duplicate(withRules.type)
        }

        val finalDraft = when (duplicateDecision) {
            DuplicateDecision.SUSPECTED -> withRules.copy(
                status = TransactionStatus.NEEDS_REVIEW,
                reviewReason = ReviewReason.DUPLICATE_SUSPECTED
            )
            DuplicateDecision.UNIQUE,
            DuplicateDecision.DUPLICATE -> withRules
        }

        val id = repository.saveTransaction(finalDraft.toDomain())
        if (finalDraft.status == TransactionStatus.NEEDS_REVIEW && finalDraft.reviewReason != null) {
            repository.createReviewItem(id, finalDraft.reviewReason)
        }

        return IngestionResult.Saved(finalDraft.type, finalDraft.reviewReason)
    }
}

sealed interface IngestionResult {
    data class Saved(
        val transactionType: TransactionType,
        val reviewReason: ReviewReason?
    ) : IngestionResult

    data class Duplicate(
        val transactionType: TransactionType?
    ) : IngestionResult

    data class Ignored(
        val reason: String
    ) : IngestionResult
}

private fun TransactionDraft.toDomain(): MoneyTransaction {
    return MoneyTransaction(
        occurredAt = occurredAt,
        amount = amount,
        direction = direction,
        type = type,
        category = category,
        paymentMethod = paymentMethod,
        merchant = merchant,
        counterparty = counterparty,
        memo = memo,
        sourceApp = sourceApp,
        sourceType = SourceType.NOTIFICATION,
        sourceNotificationHash = sourceNotificationHash,
        status = status,
        confidence = confidence,
        monthKey = monthKey
    )
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
    val message: String?,
    val parsedType: String?
) {
    companion object {
        fun fromIngestionResult(
            snapshot: NotificationSnapshot,
            result: IngestionResult,
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
            val message = when (result) {
                is IngestionResult.Saved -> "??λ맖"
                is IngestionResult.Duplicate -> "以묐났 ?뚮┝"
                is IngestionResult.Ignored -> result.reason
            }
            return fromSnapshot(
                snapshot = snapshot,
                receivedAt = receivedAt,
                result = diagnosticResult,
                message = message,
                parsedType = parsedType
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
                message = throwable.message ?: throwable::class.simpleName ?: "?ㅻ쪟 諛쒖깮",
                parsedType = null
            )

        private fun fromSnapshot(
            snapshot: NotificationSnapshot,
            receivedAt: Instant,
            result: NotificationDiagnosticResult,
            message: String?,
            parsedType: String?
        ): LastNotificationDiagnostic =
            LastNotificationDiagnostic(
                receivedAt = receivedAt,
                postedAt = snapshot.postedAt,
                packageName = snapshot.packageName,
                title = snapshot.title,
                textPreview = snapshot.textPreview(),
                result = result,
                message = message,
                parsedType = parsedType
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
                KEY_MESSAGE to preferences.getString(KEY_MESSAGE, null),
                KEY_PARSED_TYPE to preferences.getString(KEY_PARSED_TYPE, null)
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
        parsedType?.let { put(KEY_PARSED_TYPE, it) }
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
```

## app\src\main\java\com\choiyoonseo\automoney\ui\settings\SettingsScreen.kt
```kotlin
package com.choiyoonseo.automoney.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.notification.IngestionResult
import com.choiyoonseo.automoney.notification.LastNotificationDiagnostic
import com.choiyoonseo.automoney.notification.NotificationDiagnosticResult
import com.choiyoonseo.automoney.notification.SampleNotificationScenario
import com.choiyoonseo.automoney.ui.components.EmptyStateVisual
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    onOpenNotificationSettings: () -> Unit = {},
    notificationAccessEnabled: Boolean? = null,
    lastNotificationDiagnostic: LastNotificationDiagnostic? = null,
    sampleNotifications: List<SampleNotificationScenario> = emptyList(),
    onRunSampleNotification: (suspend (SampleNotificationScenario) -> IngestionResult)? = null
) {
    val scope = rememberCoroutineScope()
    var sampleResultMessage by remember { mutableStateOf<String?>(null) }
    var runningSampleId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "?ㅼ젙",
            subtitle = "?먮룞 湲곕줉怨??뚮┝ 吏꾨떒 ?곹깭瑜??뺤씤?댁슂."
        )

        EmptyStateVisual(
            title = "?뚮┝?쇰줈 ?먮룞 湲곕줉",
            message = "?덉슜??湲덉쑖 ???뚮┝???쎌뼱 嫄곕옒 ?꾨낫濡?諛붽씀怨??뺤씤?댁슂."
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("?뚮┝ ?묎렐 沅뚰븳", fontWeight = FontWeight.Bold)
                Text(
                    text = when (notificationAccessEnabled) {
                        true -> "沅뚰븳 耳쒖쭚"
                        false -> "沅뚰븳 爰쇱쭚"
                        null -> "沅뚰븳 ?곹깭 ?뺤씤 ?꾩슂"
                    },
                    fontWeight = FontWeight.Medium
                )
                Text(
                    when (notificationAccessEnabled) {
                        true -> "?덉슜??湲덉쑖 ???뚮┝???ㅼ뼱?ㅻ㈃ ?먮룞 湲곕줉 ?꾨낫濡?泥섎━?댁슂."
                        false -> "?뚮┝ ?묎렐 沅뚰븳???덉슜?댁빞 寃곗젣/?↔툑 ?뚮┝???쎌쓣 ???덉뼱??"
                        null -> "沅뚰븳 ?ㅼ젙?먯꽌 AutoMoney ?뚮┝ ?묎렐???뺤씤??二쇱꽭??"
                    }
                )
                Button(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("沅뚰븳 ?ㅼ젙 ?닿린")
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("理쒓렐 ?뚮┝ 寃곌낵", fontWeight = FontWeight.Bold)
                if (lastNotificationDiagnostic == null) {
                    Text("?꾩쭅 泥섎━??湲덉쑖 ???뚮┝???놁뼱??")
                    Text("?덉슜??湲덉쑖 ???뚮┝???ㅼ뼱?ㅻ㈃ ?ш린?먯꽌 留덉?留?寃곌낵媛 ?쒖떆?쇱슂.")
                } else {
                    Text(lastNotificationDiagnostic.result.toDisplayText(), fontWeight = FontWeight.Medium)
                    Text("泥섎━ ${lastNotificationDiagnostic.receivedAt.toDisplayTime()}")
                    Text("Source ${lastNotificationDiagnostic.packageName}")
                    Text(lastNotificationDiagnostic.title ?: "?쒕ぉ ?놁쓬")
                    Text(lastNotificationDiagnostic.textPreview)
                    lastNotificationDiagnostic.parsedType?.let {
                        Text("Type $it")
                    }
                    lastNotificationDiagnostic.message?.let { Text(it) }
                }
            }
        }

        if (sampleNotifications.isNotEmpty() && onRunSampleNotification != null) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("?섑뵆 ?뚮┝ ?뚯뒪??, fontWeight = FontWeight.Bold)
                    Text("?ㅼ젣 ?뚮┝ 沅뚰븳??耳쒓린 ?꾩뿉 湲덉쑖 ???뚮┝ ?먮쫫??誘몃━ ?뺤씤?????덉뼱??")
                    sampleResultMessage?.let { Text(it, fontWeight = FontWeight.Medium) }
                    sampleNotifications.forEach { scenario ->
                        Button(
                            enabled = runningSampleId == null,
                            onClick = {
                                scope.launch {
                                    runningSampleId = scenario.id
                                    try {
                                        val result = onRunSampleNotification(scenario)
                                        sampleResultMessage = result.toSampleResultMessage(scenario)
                                    } catch (e: RuntimeException) {
                                        sampleResultMessage = "${scenario.label} ?섑뵆 ?ㅽ뻾 以?臾몄젣媛 ?앷꼈?댁슂."
                                    } finally {
                                        runningSampleId = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val label = if (runningSampleId == scenario.id) {
                                "?쎈뒗 以?.."
                            } else {
                                scenario.label
                            }
                            Text(label)
                        }
                        Text(scenario.description)
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("?욎쑝濡?異붽???寃?, fontWeight = FontWeight.Bold)
                Text("移대뱶?ъ? ????깅퀎 ?뚮┝ 洹쒖튃")
                Text("移댄뀒怨좊━ ?먮룞 遺꾨쪟? 諛섎났 寃곗젣 ?뚮┝")
            }
        }
    }
}

private fun IngestionResult.toSampleResultMessage(scenario: SampleNotificationScenario): String =
    when (this) {
        is IngestionResult.Saved -> "${scenario.label} ?섑뵆???ｌ뿀?댁슂."
        is IngestionResult.Duplicate -> "${scenario.label} ?섑뵆? ?대? ?ㅼ뼱媛 ?덉뼱??"
        is IngestionResult.Ignored -> "${scenario.label} ?섑뵆???쎌? 紐삵뻽?댁슂."
    }

private fun NotificationDiagnosticResult.toDisplayText(): String =
    when (this) {
        NotificationDiagnosticResult.SAVED -> "??λ맖"
        NotificationDiagnosticResult.DUPLICATE -> "以묐났"
        NotificationDiagnosticResult.IGNORED -> "臾댁떆??
        NotificationDiagnosticResult.ERROR -> "?ㅻ쪟"
    }

private fun Instant.toDisplayTime(): String =
    DIAGNOSTIC_TIME_FORMATTER.format(this)

private val DIAGNOSTIC_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M??d??HH:mm").withZone(ZoneId.of("Asia/Seoul"))
```

## app\src\test\java\com\choiyoonseo\automoney\notification\NotificationDiagnosticsStoreTest.kt
```kotlin
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
        assertThat(diagnostic.title).isEqualTo("?좎뒪 寃곗젣")
        assertThat(diagnostic.textPreview).isEqualTo("?좎뒪 寃곗젣\n?ㅽ?踰낆뒪 6,100??寃곗젣")
        assertThat(diagnostic.result).isEqualTo(NotificationDiagnosticResult.SAVED)
        assertThat(diagnostic.message).isEqualTo("??λ맖")
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
        assertThat(duplicate.message).isEqualTo("以묐났 ?뚮┝")
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
            title = "?좎뒪 寃곗젣",
            text = "?ㅽ?踰낆뒪 6,100??寃곗젣",
            bigText = null,
            postedAt = Instant.parse("2026-07-02T03:00:00Z")
        )
}
```

