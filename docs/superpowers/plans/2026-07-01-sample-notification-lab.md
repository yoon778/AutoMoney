# Sample Notification Lab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an in-app way to inject realistic Toss notification samples and see them affect Home, Transactions, Review, and Report screens.

**Architecture:** Define reusable `SampleNotificationScenario` models that convert into `NotificationSnapshot`. Settings screen shows sample buttons and calls the existing `NotificationIngestionUseCase`, so samples use the same parser, duplicate detector, repository, and review-item creation path as real notifications. Transactions screen reads monthly DB transactions instead of fixed sample rows when a repository is injected.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room Flow, JUnit + Truth, Gradle wrapper.

## Global Constraints

- Sample injection must not bypass `NotificationIngestionUseCase`.
- Sample scenarios must include normal payment, wallet topup, transfer, refund/cancel, and payment gateway cases.
- Duplicate sample taps should surface duplicate feedback instead of silently doing nothing.
- Preview/fallback mode must still work without injected repository/use case.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Sample Notification Scenarios

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/SampleNotificationScenarios.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/notification/SampleNotificationScenariosTest.kt`

- [x] Write failing tests for scenario count, ids, labels, and snapshot conversion.
- [x] Run scenario tests and confirm RED.
- [x] Implement `SampleNotificationScenario`, `sampleNotificationScenarios`, and `toSnapshot`.
- [x] Run scenario tests and confirm GREEN.

### Task 2: Transaction Rows From DB

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapper.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapperTest.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`

- [x] Write failing test for `transactionsToRows`.
- [x] Run mapper tests and confirm RED.
- [x] Implement `transactionsToRows` using the existing row mapper.
- [x] Wire `TransactionsScreen` to `MoneyRepository.observeTransactionsForMonth`.
- [x] Pass repository from `AppRoot` into `TransactionsScreen`.
- [x] Run mapper tests and confirm GREEN.

### Task 3: Settings Sample Lab UI

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`

- [x] Add sample notification section to Settings.
- [x] Launch sample ingestion on button click.
- [x] Show Korean result messages for saved, duplicate, and ignored results.
- [x] Keep section hidden in preview/fallback mode when no ingestion callback is supplied.

### Task 4: Verification

- [x] Run full unit tests and debug build.
- [x] Install and launch on emulator.
- [x] Tap sample notification buttons.
- [x] Verify Home metrics, Transactions rows, and Review count/cards react to inserted samples.
- [x] Update this checklist.
