# Task 6 Report: Diagnostics And Settings Copy

## Status
- Completed Task 6 implementation in the requested scope.
- No commits created, per instructions.

## Changes

### 1. Detailed ingestion results
- Replaced the `IngestionResult` enum in [NotificationIngestionUseCase.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt) with a sealed interface:
  - `IngestionResult.Saved(transactionType, reviewReason)`
  - `IngestionResult.Duplicate(transactionType)`
  - `IngestionResult.Ignored(reason)`
- Updated `ingest(...)` to return the detailed result shapes required by the brief:
  - ignored parse returns the parse reason or `"not parsed"`
  - duplicates carry the parsed transaction type
  - saved results carry final transaction type and review reason

### 2. Diagnostics parsed type support
- Added `parsedType: String?` to `LastNotificationDiagnostic` in [NotificationDiagnosticsStore.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt).
- Derived `parsedType` from sealed ingestion results:
  - saved -> `transactionType.name`
  - duplicate -> `transactionType?.name`
  - ignored -> `null`
- Added `KEY_PARSED_TYPE = "parsedType"` and round-tripped it through preferences serialization/deserialization.
- Updated ingestion-result-to-diagnostic mapping to use `is IngestionResult.*`.
- Updated ignored diagnostics to store the actual ignore reason as the message.

### 3. Settings copy and diagnostics UI
- Updated [SettingsScreen.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt) copy from Toss-only wording to broader financial-app wording.
- Added parsed type display in the diagnostics card when present:
  - `Type EXPENSE` style output
- Updated sample result messaging to match the sealed `IngestionResult` variants.

### 4. Test updates
- Reworked [NotificationDiagnosticsStoreTest.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt) to use the new sealed ingestion result constructors.
- Added the requested parsed-type regression test:
  - `diagnosticIncludesParsedTypeForSavedFinanceNotification`
- Extended assertions to verify:
  - parsed type on saved/duplicate results
  - ignored reason message
  - `parsedType == null` for ignored/error cases

## TDD Evidence
- Wrote the parsed-type diagnostics test first and updated direct enum-style test call sites to the new sealed constructors.
- Ran the diagnostics test target before production changes and got the expected red state:
  - unresolved references for `IngestionResult.Saved`, `Duplicate`, `Ignored`
  - unresolved `parsedType`
- Implemented the production changes.
- Re-ran the diagnostics test target and got green.

## Verification
- Ran:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest --no-daemon --console=plain
```

- Result: `BUILD SUCCESSFUL`

- Ran the brief's required combined verification:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest :app:assembleDebug --no-daemon --console=plain
```

- Result: `BUILD SUCCESSFUL`

## Files Changed
- [NotificationIngestionUseCase.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt)
- [NotificationDiagnosticsStore.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt)
- [SettingsScreen.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt)
- [NotificationDiagnosticsStoreTest.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt)

## Self-Review
- Kept the result-model change contained to ingestion, diagnostics, settings UI, and the directly affected diagnostics test.
- Avoided unrelated cleanup and did not revert any existing work.
- Verified the new sealed result compiles through the sample notification flow and app debug assembly.

## Concerns
- The repository has no `HEAD` commit and appears entirely untracked, so there is no meaningful git diff baseline beyond the working tree files themselves.
- Existing Gradle/AGP deprecation warnings remain during build, but they did not block Task 6 verification.

## Review Finding Follow-Up
- Fixed the Settings diagnostics card in [SettingsScreen.kt](C:/Users/cys04/OneDrive/Desktop/AutoMoney/app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt) so the non-empty state now shows the notification source package on its own line:
  - `Source com.kbstar.kbbank`
- Preserved the existing parsed type line (`Type EXPENSE`) and the masked preview/result layout.

### Follow-Up Verification
- Ran the required command again:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest :app:assembleDebug --no-daemon --console=plain
```

- Result: `BUILD SUCCESSFUL`
