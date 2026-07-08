# Task 1 Report: Parser Interface And Router

## What changed
- Added `NotificationParser`, a small interface with `canParse(snapshot)` and `parse(snapshot)`.
- Added `NotificationParserRouter`, which selects the first parser that can handle a snapshot and falls back to `ParseResult.Ignored("unsupported package")` when none can.
- Updated `TossNotificationParser` to implement `NotificationParser` and expose `canParse` for the Toss package gate.
- Added `NotificationParserRouterTest` to verify first-match routing and unsupported-package fallback.

## Files changed
- `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParser.kt`
- `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouter.kt`
- `app/src/main/java/com/choiyoonseo/automoney/domain/parser/TossNotificationParser.kt`
- `app/src/test/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouterTest.kt`

## TDD evidence
### RED
Command:
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest --no-daemon --console=plain
```
Result:
- Failed to compile because `NotificationParserRouter` and `NotificationParser` were unresolved, which is the expected missing-production-code failure.

### GREEN
Command:
```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest --tests com.choiyoonseo.automoney.domain.parser.TossNotificationParserTest --no-daemon --console=plain
```
Result:
- Passed.

## Self-review
- Scope stayed inside the requested parser files and the new router test.
- Router behavior is minimal and deterministic: first matching parser wins, otherwise unsupported-package ignore.
- Toss parsing logic was left intact except for the interface contract and package gate extraction.

## Concerns
- Gradle still prints existing deprecation warnings from the Android build setup; they are unrelated to this task.
- The repository is still in an uncommitted/untracked state by instruction, so no commit was created.
