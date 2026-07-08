# Manual Income Category Options Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let manual income entries choose an income category instead of always saving as salary.

**Architecture:** Extend the existing manual category option model with a separate income option list. Reuse the manual form dropdown UI for expense and income, while keeping transfer category-free.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit + Truth, Gradle wrapper.

## Global Constraints

- Keep the existing `지출 / 수입 / 이체` selector.
- Expense category options stay unchanged.
- Income options are `월급`, `용돈`, `환급`, `기타`.
- Transfer keeps no visible category selector and saves as `기타`, which the use case ignores.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Income Category Options

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualCategoryOptions.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/ui/transactions/ManualCategoryOptionsTest.kt`

- [x] Add failing tests for `manualIncomeCategoryOptions` and `defaultManualIncomeCategoryOption`.
- [x] Run the focused test and confirm RED.
- [x] Add the income category option list.
- [x] Run the focused test and confirm GREEN.

### Task 2: Manual Form Connection

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualTransactionForm.kt`

- [x] Add separate state for selected income category.
- [x] Show the category dropdown for expense and income.
- [x] Feed expense category for expense, income category for income, and `기타` for transfer.
- [x] Keep transfer category selector hidden.

### Task 3: Verification

- [x] Run full unit tests and debug build.
- [x] Install and launch on emulator.
- [x] Verify income mode shows `월급 / 용돈 / 환급 / 기타`.
- [x] Save income with a non-salary category and verify it appears in transactions.
- [x] Clear emulator verification data.
- [x] Update this checklist.
