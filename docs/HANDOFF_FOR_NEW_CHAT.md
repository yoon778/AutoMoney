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

Current collaboration branch:

```text
main                only local/remote working branch
```

Main-only rules:

- GitHub default branch is `main`.
- Codex and Claude work sequentially; only one agent active at a time.
- Agent/feature branches, separate worktrees, and alternate clones are forbidden.
- Start with `git switch main` and `git pull --ff-only origin main`.
- Ensure `git config --local core.hooksPath .githooks` is configured.
- Commit/push small verified units directly to `main`.
- Repository hooks reject commits and non-deletion pushes outside `main`.

## Agent Roles

Codex:

- Work directly on `main` after confirming a clean, up-to-date tree.
- Own app logic, Android architecture, notification parsing, Room/database, asset balance calculations, report calculations, transaction review logic, rules/automation, tests, and builds.
- Before claiming code work is complete, run relevant unit tests and usually `:app:assembleDebug`.

Claude:

- Pull latest `main` after Codex handoff, then work directly on `main`.
- Own UI layout, copy, visual hierarchy, icons/images, screen composition, and general UX polish.
- Avoid modifying core logic, database, notification parsing, and report calculations unless explicitly assigned.

User:

- Reviews commit diffs already pushed to `main`.
- Uses `git revert` for rollback; no branch merge workflow.

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

Recent logic changes on `main`:

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

### Active handoff: budget-first redesign T1-T2 — 2026-07-13

- Read `docs/superpowers/plans/2026-07-13-budget-first-redesign.md` (사용자 grilling으로 결정 10개 확정됨 — 재논의 불필요)
- Codex task: T1 (MoneyTransaction.budgetPlanId + Room migration + buildCategoryBudgetUsages override + 예산 밖 지출 집계), T2 (저장/수정/검토확정 경로 배선 + 분류 매칭 기본값)
- 계좌 관련 테이블·로직 삭제 금지 — 화면 단절은 Claude T3-T5 몫
- T2 완료 push 후 Claude가 T3-T5 (수정 dialog 예산 선택, 자산→예산 탭 개편, 검토 정리)
- 참고: 최근 parser 수정 3건 — CommonFinanceNotificationParser(단일 금액 ambiguous movement 검토行), GenericFinanceNotificationParser(잔액 라벨 금액 제외), NotificationSnapshot.combinedText(text/bigText 중복 제거). 앱 표시명은 짤랑으로 리브랜딩됨

### (완료 2026-07-13) notification source selection T1-T8 done

Codex 사용량 소진으로 T7·T8은 Claude가 비상 릴레이로 완료. Galaxy SM_S931N에서
`com.android.shell` 실알림으로 차단/허용/검토 전 경로 검증, instrumented tests PASS.
상세: `docs/testing/bank-notification-balance-sync.md`, plan은 Status: complete.
잔여: 실은행 앱(케이뱅크 등) 실알림 확인은 일상 사용 중 수행.

Recommended next logic tasks:

1. Add a rule management screen or settings section so learned rules can be viewed, disabled, or deleted.
2. Improve bank notification account extraction so KB and other bank notifications reliably attach payment method/account.
3. Add stronger tests around wallet topup plus actual wallet spend sequence.
4. Add transaction search/filter by account, category, month, and status.
5. Ensure all user-facing placeholder/dev-only text is removed before distribution.

## Suggested Next Claude Tasks

### (완료 2026-07-13) notification source selection UI — T6 done at `b0995c9`, ownership returned to Codex for T7

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

Codex와 Claude 모두 main에서만 순차 작업해. 다른 branch/worktree/clone은 만들지 마.
시작 전 `git switch main`, `git pull --ff-only origin main`, `git status`로 clean 상태를 확인해.
Codex는 로직/DB/알림 파싱/잔액 계산/보고서 계산/규칙/테스트를 담당하고, 작은 commit을 main에 push해.
Claude는 그다음 latest main을 pull한 뒤 UI/UX를 담당해.

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

Claude와 Codex 모두 main에서만 순차 작업해. 다른 branch/worktree/clone은 만들지 마.
시작 전 `git switch main`, `git pull --ff-only origin main`, `git status`로 clean 상태를 확인해.
Claude는 UI 레이아웃/문구/시각적 정리/사용성 개선을 담당하고, 작은 commit을 main에 push해.
Codex 로직 commit이 필요하면 먼저 main에 반영된 뒤 pull해서 이어가.

먼저 docs/AI_COLLABORATION.md 와 docs/HANDOFF_FOR_NEW_CHAT.md 를 읽고, core logic/database/parser/report calculation은 건드리지 말고 UI 중심으로 진행해줘.
```
