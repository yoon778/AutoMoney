# Manual Category Selector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the free-text manual transaction category field with a controlled category menu.

**Architecture:** Add a small UI model for manual expense category options backed by the domain `Category` enum. `ManualTransactionForm` keeps selected option state and sends the selected display name to the existing save callback.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit + Truth, Gradle wrapper.

## Global Constraints

- DB schema and save use case must not change.
- Default category remains `기타`.
- Options must include common expense categories first: `식비`, `카페/간식`, `교통비`, `쇼핑`, `생활`, then `기타`.
- The form must still work in preview/fallback mode.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Category Option Model

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualCategoryOptions.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/ui/transactions/ManualCategoryOptionsTest.kt`

- [x] Write failing tests for option labels and default option.
- [x] Run tests and confirm RED.
- [x] Implement `ManualCategoryOption`, `manualExpenseCategoryOptions`, and `defaultManualCategoryOption`.
- [x] Run tests and confirm GREEN.

### Task 2: Form Dropdown UI

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualTransactionForm.kt`

- [x] Replace category text field with a menu button.
- [x] Show the selected category.
- [x] Send selected category display name to `onSave`.
- [x] Keep amount validation and saving disabled state intact.

### Task 3: Verification

- [x] Run full unit tests and debug build.
- [x] Install and launch on emulator.
- [x] Open transaction screen and verify category selector appears.
- [x] Save a manual expense and verify the row uses selected/default category.
- [x] Update this checklist.
