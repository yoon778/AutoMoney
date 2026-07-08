# Manual Transaction Save Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Save manual expense entries into the same Room-backed transaction store used by notification ingestion.

**Architecture:** Add a small domain use case that converts form input into a `MoneyTransaction` with `SourceType.MANUAL` and `TransactionStatus.USER_EDITED`, then persists through `MoneyRepository.saveTransaction`. Wire `TransactionsScreen` to call the use case and show save/error feedback while the existing DB Flow updates the list.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room Flow, JUnit + Truth, Gradle wrapper.

## Global Constraints

- Manual entries are expenses in this iteration.
- Amount must be greater than zero.
- Category text should match `Category.displayName` when possible and fall back to `Category.OTHER`.
- Saved transactions must use `SourceType.MANUAL`, `TransactionType.EXPENSE`, `TransactionDirection.EXPENSE`, `TransactionStatus.USER_EDITED`, and confidence `1.0`.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Manual Save Use Case

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/manual/SaveManualTransactionUseCase.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/domain/manual/SaveManualTransactionUseCaseTest.kt`

- [x] Write failing test for saving a manual expense with category, memo, month key, and manual source fields.
- [x] Write failing test for rejecting zero amount.
- [x] Run use case tests and confirm RED.
- [x] Implement `SaveManualTransactionUseCase.saveExpense`.
- [x] Run use case tests and confirm GREEN.

### Task 2: App Wiring

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`

- [x] Create the use case in `AppContainer`.
- [x] Pass it through `MainActivity` and `AppRoot`.
- [x] Keep previews/fallback working when the use case is null.

### Task 3: Transactions Screen Save UX

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualTransactionForm.kt`

- [x] Call `saveExpense` from the form save callback.
- [x] Show a Korean success message.
- [x] Show a Korean validation/error message.
- [x] Disable the save button while saving.

### Task 4: Verification

- [x] Run full unit tests and debug build.
- [x] Install and launch on emulator.
- [x] Save a manual expense.
- [x] Verify the transaction row appears and Home metrics update.
- [x] Update this checklist.
