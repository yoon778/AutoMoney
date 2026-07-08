# Task 5 Report

## Changes

- Updated `NotificationIngestionUseCase` to depend on `NotificationParser` instead of `TossNotificationParser`, so ingestion can accept the router without narrowing the parser type.
- Updated `AppContainer` to construct `NotificationParserRouter` with `TossNotificationParser()` and `CommonFinanceNotificationParser()` and pass that router into `NotificationIngestionUseCase`.

## Files Changed

- `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
- `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`

## Tests

- Ran:
  - `$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.TossNotificationParserTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --tests com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest --no-daemon --console=plain`
- Result: passed.

## Self-Review

- Scope stayed limited to the two Task 5 files.
- The parser dependency is now generalized at the ingestion boundary, matching the router-based wiring in the container.
- Imports were adjusted only as needed for the new parser interface and router construction.
- No diagnostics/result-shape changes were made.

## Concerns

- The targeted parser tests passed, but this task did not add a dedicated container wiring test, so the `AppContainer` change is still validated primarily by code review and compilation through the app test task.
- Repository state still has no HEAD commit, so I did not create a commit, per instruction.
