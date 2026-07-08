# Task 2 Report: Financial App Registry And Listener Filtering

## What changed
- Added `FinancialAppRegistry` in `app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt`.
- Defined `TOSS_PACKAGE` and `KB_STAR_BANKING_PACKAGE` constants plus `isSupportedPackage(packageName: String): Boolean`.
- Updated `MoneyNotificationListenerService` to use `FinancialAppRegistry.isSupportedPackage(...)` instead of a hardcoded Toss-only package check.
- Added `FinancialAppRegistryTest` in `app/src/test/java/com/choiyoonseo/automoney/notification/FinancialAppRegistryTest.kt`.

## Tests run
- `\$env:JAVA_HOME='D:\Android Studio\jbr'; \$env:Path="\$env:JAVA_HOME\bin;\$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.FinancialAppRegistryTest --no-daemon --console=plain`
- `\$env:JAVA_HOME='D:\Android Studio\jbr'; \$env:Path="\$env:JAVA_HOME\bin;\$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.FinancialAppRegistryTest :app:assembleDebug --no-daemon --console=plain`

## TDD evidence
### RED
- Initial registry test run failed as expected with unresolved reference errors for `FinancialAppRegistry`.
- Final red output before implementation:
  - `Unresolved reference 'FinancialAppRegistry'` in `FinancialAppRegistryTest.kt`
- This confirmed the test was exercising the missing production symbol.

### GREEN
- After adding the registry and listener filter, the combined test/build command passed.
- Result: `BUILD SUCCESSFUL`

## Files changed
- `app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt`
- `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`
- `app/src/test/java/com/choiyoonseo/automoney/notification/FinancialAppRegistryTest.kt`

## Self-review
- The registry is intentionally small and deterministic.
- Listener filtering now uses one shared source of truth for supported financial app packages.
- The test covers supported Toss and KB Star Banking packages, plus unknown and blank inputs.

## Concerns
- KB Star Banking is now allowed through the listener, but this task does not add a KB parser. If KB notifications arrive, downstream ingestion may still ignore them until a parser is introduced.
