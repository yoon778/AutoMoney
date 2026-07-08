# Task 4 Report: Common Korean Finance Parser

## Status

BLOCKED

## What Changed

Implemented the Task 4 parser work within the allowed file scope:

1. Added `ReviewReason.INCOME_UNKNOWN` immediately after `TRANSFER_UNKNOWN` in `app/src/main/java/com/choiyoonseo/automoney/domain/model/MoneyModels.kt`.
2. Added `app/src/test/java/com/choiyoonseo/automoney/domain/parser/CommonFinanceNotificationParserTest.kt` with coverage for:
   - KB card payment parsing as auto-confirmed expense
   - transfer parsing as review-needed transfer
   - deposit parsing as review-needed income
   - wallet top-up parsing as review-needed wallet top-up
   - cancel/refund parsing as review-needed refund
   - promotional notification ignore behavior
   - unsupported package ignore behavior
3. Added `app/src/main/java/com/choiyoonseo/automoney/domain/parser/CommonFinanceNotificationParser.kt` implementing:
   - `NotificationParser`
   - KB package gating through `FinancialAppRegistry.KB_STAR_BANKING_PACKAGE`
   - amount extraction from won-formatted text
   - promotion filtering
   - routing for refund, top-up, deposit, transfer, and payment cases
   - `SensitiveTextMasker` usage for memo masking
   - `TransactionDraft` construction with hash, month key, status, confidence, and review reason

## Tests Run

### RED run

Command:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --no-daemon --console=plain
```

Observed result:

- Build failed during `:app:compileDebugKotlin`
- Failure was **not** due to missing `CommonFinanceNotificationParser`
- Actual failure:

```text
ReviewItemMapper.kt:9:9 'when' expression must be exhaustive. Add the 'INCOME_UNKNOWN' branch or an 'else' branch.
ReviewItemMapper.kt:9:9 Return type mismatch: expected 'ReviewCardUi', actual 'Unit'.
```

### Post-implementation run

Ran the same command again after adding the parser implementation.

Observed result:

- Build failed with the same out-of-scope `ReviewItemMapper.kt` compile error
- This prevented reaching parser test execution, so GREEN could not be completed within the requested edit boundary

## TDD Evidence

- RED completed: added tests first, then ran the targeted Gradle test command before implementing the parser
- RED result was a real compile failure caused by introducing `ReviewReason.INCOME_UNKNOWN`
- GREEN blocked: after implementation, the same targeted test command still cannot reach the new parser tests because `app/src/main/java/com/choiyoonseo/automoney/ui/model/ReviewItemMapper.kt` does not handle the new enum value

## Files Changed

- `C:\Users\cys04\OneDrive\Desktop\AutoMoney\app\src\main\java\com\choiyoonseo\automoney\domain\model\MoneyModels.kt`
- `C:\Users\cys04\OneDrive\Desktop\AutoMoney\app\src\main\java\com\choiyoonseo\automoney\domain\parser\CommonFinanceNotificationParser.kt`
- `C:\Users\cys04\OneDrive\Desktop\AutoMoney\app\src\test\java\com\choiyoonseo\automoney\domain\parser\CommonFinanceNotificationParserTest.kt`

## Self-Review

- Kept edits inside the exact Task 4 file allowlist
- Followed the requested TDD order: test first, run, implement, rerun
- Used Unicode escapes for Korean keyword constants in the new test and parser keyword constants
- Verified that `ReviewReason` is already used as a typed Room/entity field in existing code; no schema file or converter changes were made
- Parser logic is intentionally narrow and matches existing parser patterns:
  - ignores unsupported package
  - ignores promotions with money-like amounts
  - parses won amounts
  - maps payment to expense, transfer to transfer review, deposit to income review, top-up to wallet top-up review, cancel/refund to refund review

## Concerns

1. The task brief expected the initial RED failure to be `unresolved reference CommonFinanceNotificationParser`, but the real build fails earlier because adding `INCOME_UNKNOWN` breaks an existing exhaustive `when` in `ReviewItemMapper.kt`.
2. Because the user restricted edits to the three Task 4 files, I did not modify `ReviewItemMapper.kt`; this leaves the build blocked.
3. The new parser code could not be fully validated by Gradle in this turn because Kotlin compilation stops before the parser test class can run.

## Scope Expansion Fix

Approved additional write scope used:

- `C:\Users\cys04\OneDrive\Desktop\AutoMoney\app\src\main\java\com\choiyoonseo\automoney\ui\model\ReviewItemMapper.kt`

Applied conservative handling for `ReviewReason.INCOME_UNKNOWN` in `openReviewItemsToCards`:

- used `ReviewCardKind.OTHER`
- preserved the existing branch shape and actions style
- avoided adding any new UI model types
- used simple quoted strings for the new branch to avoid mojibake-related syntax issues

## Final Verification Run

Command:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --no-daemon --console=plain
```

Observed result:

- `BUILD SUCCESSFUL`
- targeted test task `:app:testDebugUnitTest` completed successfully

## Updated Status

DONE

## Review Fix: Comma-Free Won Amount Parsing

Addressed review findings in the Task 4 parser/test scope:

1. Added a focused regression test for `STARBUCKS 6100원 승인` to verify:
   - parsed amount remains `6100`
   - parsed merchant is exactly `STARBUCKS`
2. Updated `CommonFinanceNotificationParser` so amount extraction returns the actual regex match text together with the parsed `MoneyAmount`.
3. Updated merchant extraction and memo line selection to use the matched amount text directly rather than only the reconstructed `%,d원` string.
4. Preserved the existing comma-formatted positive test coverage.

### Review-Fix RED Run

Command:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --no-daemon --console=plain
```

Observed result:

- `CommonFinanceNotificationParserTest > parsesKbCardPaymentWithoutCommaAsAutoConfirmedExpense FAILED`
- test suite summary: `8 tests completed, 1 failed`

### Review-Fix GREEN Run

Ran the same command after updating the parser.

Observed result:

- `BUILD SUCCESSFUL`
- targeted parser suite passed
