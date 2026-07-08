# Manual Entry Type Selector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow manual entries to be saved as expense, income, or transfer.

**Architecture:** Extend `SaveManualTransactionUseCase` with a manual entry type enum and a new `save` method. Keep `saveExpense` as a compatibility wrapper. Add a small segmented control to `ManualTransactionForm` and pass the selected type into the existing screen save flow.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room Flow, JUnit + Truth, Gradle wrapper.

## Global Constraints

- Existing `saveExpense` behavior must remain compatible.
- Expense saves as `TransactionType.EXPENSE` and `TransactionDirection.EXPENSE`.
- Income saves as `TransactionType.INCOME` and `TransactionDirection.INCOME`.
- Transfer saves as `TransactionType.TRANSFER` and `TransactionDirection.NEUTRAL`.
- Manual entries keep `SourceType.MANUAL`, `TransactionStatus.USER_EDITED`, and confidence `1.0`.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Domain Type Save

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/manual/SaveManualTransactionUseCase.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/domain/manual/SaveManualTransactionUseCaseTest.kt`

- [x] Write failing tests for income and transfer manual saves.
- [x] Run use case tests and confirm RED.
- [x] Add `ManualEntryType`.
- [x] Implement `save(type, amountWon, categoryText, memo, occurredAt)`.
- [x] Make `saveExpense` delegate to the new method.
- [x] Run use case tests and confirm GREEN.

### Task 2: Form Type Selector

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualTransactionForm.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`

- [x] Add `지출 / 수입 / 이체` selector.
- [x] Pass selected type to `onSave`.
- [x] Call `SaveManualTransactionUseCase.save`.
- [x] Keep category selector, amount validation, and saving disabled state intact.

### Task 3: Verification

- [x] Run full unit tests and debug build.
- [x] Install and launch on emulator.
- [x] Save manual income and verify Home income/net metrics update.
- [x] Save manual transfer and verify it appears in Transactions but does not increase monthly spend.
- [x] Update this checklist.
