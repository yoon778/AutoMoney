# AutoMoney New Chat Handoff

Use this document when starting a new Codex or Claude chat for the AutoMoney project.

Encoding note: this file is UTF-8. If Korean text looks garbled in Windows PowerShell, read it with `Get-Content -Encoding UTF8`.

## Project Location

Actual Android app project:

```text
C:\Users\cys04\Desktop\AutoMoney
```

Old planning/mockup folder, not used for active app development:

```text
C:\Users\cys04\OneDrive\문서\Automated Money Management
```

The old folder only contains early planning files, a web mockup, and a reference spreadsheet. It does not affect Android builds.

## GitHub Repository

```text
https://github.com/yoon778/AutoMoney.git
```

Current collaboration branches:

```text
main                stable integration branch, user merges here
codex/app-logic     Codex branch for app logic and tests
claude/ui-polish    Claude branch for UI and UX polish
codex/android-mvp   older branch kept for history
```

Important note:

- GitHub repository default branch may still be `codex/android-mvp`.
- For the intended workflow, the user should eventually change the GitHub default branch to `main`.

## Agent Roles

Codex:

- Work on `codex/app-logic`.
- Own app logic, Android architecture, notification parsing, Room/database, asset balance calculations, report calculations, transaction review logic, rules/automation, tests, and builds.
- Before claiming code work is complete, run relevant unit tests and usually `:app:assembleDebug`.

Claude:

- Work on `claude/ui-polish`.
- Own UI layout, copy, visual hierarchy, icons/images, screen composition, and general UX polish.
- Avoid modifying core logic, database, notification parsing, and report calculations unless explicitly assigned.

User:

- Reviews and merges into `main`.
- Uses GitHub Desktop or GitHub web UI for final merge decisions.

## Product Direction

AutoMoney is an Android/Galaxy automatic money management app.

Core concept:

- Read finance app notifications.
- Parse amount, app/source, account/payment method, merchant/counterparty, and transaction type.
- Automatically create transactions when confidence is high.
- Send ambiguous transactions to review.
- Minimize manual bookkeeping.

UI direction:

- Clean, Toss-like, practical.
- Use simple visual assets where they improve scanning.
- Prioritize functional correctness before final UI polish.

## Key User Requirements Captured So Far

- Read Android/Galaxy notifications instead of relying only on email.
- Toss may not send all bank/payment details, so app should also parse notifications from banks such as KB.
- If a notification has amount, won, account number/account name, merchant, or counterparty, use it for transaction analysis.
- Wallet/point topups such as Naver Pay should not count as real spending until actual use is recorded.
- A topup should go to review so the user can enter why it was charged or how much was actually spent.
- Transfers may mean account movement, split payment, settlement, or not personal spending, so ambiguous transfers need review.
- Review screen should allow editing amount, date, time, account, category, memo, and transaction type.
- Review items should disappear after the user resolves them.
- Transactions needing review should not appear in recent confirmed transactions until resolved.
- Account balances should update when confirmed income/expense/saving/topup movements are recorded.
- Report and home screens should show actual spending separately from savings, transfers, topups, and excluded transactions.
- Monthly reports should support previous months, not only the current month.
- Home/report metrics should open detail dialogs when tapped.
- Notification source app should be visually shown using app/source badge or color.

## Completed Work Summary

Implemented Android app skeleton and multiple feature iterations:

- Notification ingestion flow.
- Common finance notification parser.
- Toss-specific parser.
- Generic bank notification parsing direction.
- Review flow for ambiguous transactions.
- Wallet topup review flow.
- Manual transaction entry.
- Transaction editing with amount, date, time, account, category, memo, and type.
- Transaction deletion.
- Excluded transactions can be visually subdued.
- Home screen with monthly flow, today spend, recent 7-day spend, review shortcut, and recent confirmed transactions.
- Report screen with month navigation, monthly summary, category spends, calendar, and detail dialogs.
- Asset/account tab with account balances.
- Fixed expense tab concept was added earlier.
- Notification source app mapping and UI badges for financial apps.
- Account balance synchronization for income, expense, saving, investment, wallet topup, and wallet spend.
- Report calculation rules that exclude review/excluded/topup/transfer/settlement from actual spending.
- Rule learning foundation: when the user edits a transaction, the app can save category/type rules for similar future notifications.

## Most Recent Code Changes

Recent logic changes on `codex/app-logic`:

- Added shared report classification rules:
  - `isReportableTransaction`
  - `countsAsReportIncome`
  - `countsAsActualExpense`
  - `countsAsSavingMovement`
- Updated monthly summary, calendar, home, and report screens to use these rules.
- Added `savingWon` to domain monthly report calculation.
- Treated `INVESTMENT` as a saving/asset movement bucket for summary purposes.
- Added learned rule creation in `EditTransactionUseCase`.
- Added tests for report classification, calendar filtering, monthly report calculation, and learned edit rules.

Important files:

```text
app/src/main/java/com/choiyoonseo/automoney/domain/report/TransactionReportRules.kt
app/src/main/java/com/choiyoonseo/automoney/domain/report/MonthlyReportCalculator.kt
app/src/main/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapper.kt
app/src/main/java/com/choiyoonseo/automoney/ui/model/CalendarMapper.kt
app/src/main/java/com/choiyoonseo/automoney/domain/transactions/EditTransactionUseCase.kt
docs/AI_COLLABORATION.md
```

## Verification Commands

Use these from:

```text
C:\Users\cys04\Desktop\AutoMoney
```

PowerShell:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
.\gradlew.bat :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest --no-daemon --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.local.AppDatabaseMigrationTest" --no-daemon --console=plain
```

Recent verification passed:

```text
:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest
:app:connectedDebugAndroidTest with AppDatabaseMigrationTest only on Pixel_7 AVD
```

Do not claim the full instrumentation suite passed unless `:app:connectedDebugAndroidTest` is run without a class filter.

## Phone Install Policy

The user prefers not to install/update the phone after every small change.

Use this workflow:

1. Implement a meaningful batch.
2. Run tests and build.
3. Summarize what changed.
4. Install to the connected phone only when the user explicitly asks.

Known ADB path:

```text
C:\Users\cys04\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

Known device from earlier:

```text
SM_S931N / R3KYB05QDFP
```

## Suggested Next Codex Tasks

Recommended next logic tasks:

1. Add a rule management screen or settings section so learned rules can be viewed, disabled, or deleted.
2. Improve bank notification account extraction so KB and other bank notifications reliably attach payment method/account.
3. Add stronger tests around wallet topup plus actual wallet spend sequence.
4. Add transaction search/filter by account, category, month, and status.
5. Ensure all user-facing placeholder/dev-only text is removed before distribution.

## Suggested Next Claude Tasks

Recommended UI tasks:

1. Improve home screen visual hierarchy.
2. Polish report and asset screens so they feel consistent with Toss-like direction.
3. Improve transaction/review cards with clearer source app badges and category colors.
4. Replace awkward placeholder copy with natural Korean UX text.
5. Keep UI changes separate from core logic.

## Prompt To Paste Into A New Codex Chat

```text
AutoMoney 프로젝트를 이어서 작업할 거야.

실제 앱 폴더는:
C:\Users\cys04\Desktop\AutoMoney

GitHub 저장소는:
https://github.com/yoon778/AutoMoney.git

Codex는 codex/app-logic 브랜치에서 작업하고, 로직/DB/알림 파싱/잔액 계산/보고서 계산/규칙/테스트를 담당해.
Claude는 claude/ui-polish 브랜치에서 UI/UX 개선을 담당해.
main은 내가 최종 병합하는 안정 브랜치야.

먼저 docs/AI_COLLABORATION.md 와 docs/HANDOFF_FOR_NEW_CHAT.md 를 읽고 현재 상태를 파악한 다음 진행해줘.
작업 후에는 테스트와 빌드를 확인하고, 핸드폰 설치는 내가 명시적으로 요청할 때만 해줘.
```

## Prompt To Paste Into A New Claude Chat

```text
AutoMoney 프로젝트의 UI/UX를 개선할 거야.

실제 앱 폴더는:
C:\Users\cys04\Desktop\AutoMoney

GitHub 저장소는:
https://github.com/yoon778/AutoMoney.git

Claude는 claude/ui-polish 브랜치에서 작업하고, UI 레이아웃/문구/시각적 정리/사용성 개선을 담당해.
Codex는 codex/app-logic 브랜치에서 로직/DB/테스트를 담당해.
main은 내가 최종 병합하는 안정 브랜치야.

먼저 docs/AI_COLLABORATION.md 와 docs/HANDOFF_FOR_NEW_CHAT.md 를 읽고, core logic/database/parser/report calculation은 건드리지 말고 UI 중심으로 진행해줘.
```
