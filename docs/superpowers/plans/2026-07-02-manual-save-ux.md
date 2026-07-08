# Manual Save UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reset the manual transaction form after successful saves and make the saved result easier to notice.

**Architecture:** Keep the transaction screen as the coordinator. Add a small default-state helper for the form, pass a `resetSignal` from `TransactionsScreen` into `ManualTransactionForm`, and scroll the existing transaction screen after successful saves.

**Tech Stack:** Kotlin, Jetpack Compose, Room Flow, JUnit, Truth.

## Global Constraints

- Keep the Toss-like direction: clean, compact, and low-friction.
- Do not change the database schema.
- Do not change save error behavior.
- Manual dates remain based on `Asia/Seoul`.

---

### Task 1: Manual Form Defaults

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualTransactionFormDefaults.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/ui/transactions/ManualTransactionFormDefaultsTest.kt`

**Interfaces:**
- Produces: `ManualTransactionFormDefaults`
- Produces: `defaultManualTransactionFormValues(today: LocalDate): ManualTransactionFormDefaults`

- [x] **Step 1: Write the failing test**
- [x] **Step 2: Run test to verify it fails**
- [x] **Step 3: Implement the default-state helper**
- [x] **Step 4: Run test to verify it passes**

### Task 2: Wire Reset Into Manual Form

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/ManualTransactionForm.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`

**Interfaces:**
- Consumes: `defaultManualTransactionFormValues(today: LocalDate)`
- Produces: `ManualTransactionForm(resetSignal: Int = 0, ...)`

- [x] **Step 1: Key form state by `resetSignal`**
- [x] **Step 2: Increment `resetSignal` only after successful save**
- [x] **Step 3: Move success message near the top of the screen**
- [x] **Step 4: Scroll to the top after successful save**

### Task 3: Verification

**Files:**
- Modify: `docs/superpowers/plans/2026-07-02-manual-save-ux.md`

- [x] **Step 1: Run unit tests and debug build**
- [x] **Step 2: Verify on emulator**
- [x] **Step 3: Clear verification data and temporary files**

## Verification
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`
- Emulator check: saved a 12,000 won manual expense, confirmed the success chip at the top, confirmed the recent transaction row shows `-12,000원`, and confirmed the manual form reset with an empty amount field.
