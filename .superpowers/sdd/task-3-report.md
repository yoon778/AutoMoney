# Task 3: Sensitive Text Masking Report

## What changed

- Added `SensitiveTextMasker` in `app/src/main/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMasker.kt`.
- Updated `LastNotificationDiagnostic.fromIngestionResult(...)` in `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt` so the diagnostic preview is built from the notification snapshot, trimmed/deduplicated, limited to the preview length, and then masked.
- Added red/green coverage in `app/src/test/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMaskerTest.kt`.
- Added masking coverage for diagnostics preview in `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt`.

## Tests run

- `JAVA_HOME=D:\Android Studio\jbr; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.SensitiveTextMaskerTest --no-daemon --console=plain`
- `JAVA_HOME=D:\Android Studio\jbr; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.SensitiveTextMaskerTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest --no-daemon --console=plain`

## TDD evidence

### RED

- First targeted test run failed during compilation with `Unresolved reference 'SensitiveTextMasker'` from `SensitiveTextMaskerTest.kt`.
- The initial test draft also used a Truth matcher not available in this project, so I switched those assertions to `contains(...).isFalse()` before continuing.

### GREEN

- After adding the masker and wiring masking into diagnostics previews, the targeted test run completed successfully with `BUILD SUCCESSFUL`.

## Files changed

- `app/src/main/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMasker.kt`
- `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
- `app/src/test/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMaskerTest.kt`
- `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt`

## Self-review

- Scope stayed inside the four Task 3 files.
- The masking logic is narrow: it targets 6+ digit runs with optional hyphens and leaves amount-like strings such as `10,000 won` alone.
- Diagnostics persistence behavior was left intact except for the masked preview text.
- I did not change the `IngestionResult` shape.

## Concerns

- The masking rule is intentionally conservative; unusual sensitive formats that do not look like long numeric runs may still leak through.
- Gradle still prints existing deprecation warnings from the Android build setup, but the targeted tests pass.

## Review fix

### What changed

- Updated `SensitiveTextMasker.mask(...)` so amount-like strings with 6+ digits followed by `won` or `원` are protected before long numeric masking runs.
- Added regression coverage for comma-free `won` and `원` amounts in `SensitiveTextMaskerTest`.

### Tests run

- `JAVA_HOME=D:\Android Studio\jbr; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.SensitiveTextMaskerTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest --no-daemon --console=plain`

### Result

- `BUILD SUCCESSFUL`
