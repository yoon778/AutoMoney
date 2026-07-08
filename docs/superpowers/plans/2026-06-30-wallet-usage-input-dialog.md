# Wallet Usage Input Dialog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the `사용액 입력` action open a dialog, calculate actual wallet spend, and persist the reviewed topup/spend through the repository path.

**Architecture:** Keep `WalletTopupReviewService` as pure domain logic. Add `RecordWalletTopupUsageUseCase` to persist reviewed topup and created `WALLET_SPEND`. Wire Review UI to a small Compose dialog and pass the use case from `AppContainer` through `MainActivity`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room repository, JUnit + Truth, Gradle wrapper.

## Global Constraints

- Topup amount itself stays excluded from monthly expense.
- User-entered usage creates `WALLET_SPEND`.
- Invalid amount must not save.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Persistence Use Case

**Files:**
- Create: `app/src/test/java/com/choiyoonseo/automoney/domain/review/RecordWalletTopupUsageUseCaseTest.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/review/RecordWalletTopupUsageUseCase.kt`

- [x] Write failing use case test.
- [x] Confirm RED.
- [x] Implement use case.
- [x] Confirm GREEN.

### Task 2: Dialog UI

**Files:**
- Modify: `ui/components/MoneyVisuals.kt`
- Modify: `ui/review/ReviewScreen.kt`
- Modify: `ui/model/DashboardUiModels.kt`

- [x] Add primary/secondary action callbacks to review cards.
- [x] Add wallet usage input dialog.
- [x] Show saved result summary.

### Task 3: App Wiring

**Files:**
- Modify: `di/AppContainer.kt`
- Modify: `MainActivity.kt`
- Modify: `ui/AppRoot.kt`

- [x] Expose use case from container.
- [x] Pass use case to review screen.

### Task 4: Verification

- [x] Run full Gradle verification.
- [x] Install and test on emulator.
