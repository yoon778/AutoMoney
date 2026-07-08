### Task 6: Diagnostics And Settings Copy

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt`

**Interfaces:**
- Produces: detailed ingestion result types.
- Produces: `LastNotificationDiagnostic.parsedType: String?`
- Produces Settings copy that refers to allowed financial apps instead of Toss only.

- [ ] **Step 1: Write failing diagnostics test for parsed type and source package**

```kotlin
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
```

- [ ] **Step 2: Replace enum ingestion result with sealed result**

Replace:

```kotlin
enum class IngestionResult {
    Saved,
    Ignored,
    Duplicate
}
```

With:

```kotlin
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
```

Add imports for `TransactionType` and `ReviewReason`.

- [ ] **Step 3: Update ingestion returns**

Use these exact return shapes:

```kotlin
if (parsed !is ParseResult.Parsed) {
    val reason = (parsed as? ParseResult.Ignored)?.reason ?: "not parsed"
    return IngestionResult.Ignored(reason)
}
```

```kotlin
if (duplicateDecision == DuplicateDecision.DUPLICATE) {
    return IngestionResult.Duplicate(withRules.type)
}
```

```kotlin
return IngestionResult.Saved(finalDraft.type, finalDraft.reviewReason)
```

- [ ] **Step 4: Add parsed type to diagnostics model and serialization**

Add field:

```kotlin
val parsedType: String?
```

Set it from result:

```kotlin
val parsedType = when (result) {
    is IngestionResult.Saved -> result.transactionType.name
    is IngestionResult.Duplicate -> result.transactionType?.name
    is IngestionResult.Ignored -> null
}
```

Add preference key:

```kotlin
private const val KEY_PARSED_TYPE = "parsedType"
```

Round-trip it in `toPreferenceMap()` and `lastNotificationDiagnosticFromPreferenceMap(...)`.

- [ ] **Step 5: Update diagnostic result mapping**

Use:

```kotlin
val diagnosticResult = when (result) {
    is IngestionResult.Saved -> NotificationDiagnosticResult.SAVED
    is IngestionResult.Duplicate -> NotificationDiagnosticResult.DUPLICATE
    is IngestionResult.Ignored -> NotificationDiagnosticResult.IGNORED
}
```

Set message for ignored:

```kotlin
val message = (result as? IngestionResult.Ignored)?.reason
```

- [ ] **Step 6: Update Settings text**

Replace Toss-only user-facing copy in `SettingsScreen.kt` with finance-app copy:

```kotlin
true -> "?덉슜??湲덉쑖 ???뚮┝???ㅼ뼱?ㅻ㈃ ?먮룞 湲곕줉 ?꾨낫濡?泥섎━?댁슂."
false -> "?뚮┝ ?묎렐 沅뚰븳???덉슜?댁빞 寃곗젣/?↔툑 ?뚮┝???쎌쓣 ???덉뼱??"
```

For the empty diagnostics card:

```kotlin
Text("?꾩쭅 泥섎━??湲덉쑖 ???뚮┝???놁뼱??")
Text("?덉슜????? 移대뱶, ?섏씠, ?좎뒪 ?뚮┝???ㅼ뼱?ㅻ㈃ ?ш린??留덉?留?寃곌낵媛 ?쒖떆?쇱슂.")
```

- [ ] **Step 7: Show parsed type when present**

Inside the non-null diagnostic branch:

```kotlin
lastNotificationDiagnostic.parsedType?.let {
    Text("Type $it")
}
```

- [ ] **Step 8: Run diagnostics tests and compile**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest :app:assembleDebug --no-daemon --console=plain
```

Expected: PASS and debug build succeeds.

---

