# Transaction Edit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user tap a transaction row, edit amount/category/memo, and exclude a transaction from spending.

**Architecture:** Keep `TransactionsScreen` as the coordinator. Add transaction id to `TransactionRowUi`, add `EditTransactionUseCase` for edit/exclude rules, and show a compact Compose edit dialog from the transaction screen.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Room Flow, JUnit, Truth.

## Global Constraints

- Keep the UI compact and Toss-like.
- Do not change the database schema.
- Do not physically delete transactions in this step.
- Excluding a transaction must mark it as `EXCLUDED`.
- Transaction dates are not edited in this step.

---

### Task 1: Row Identity

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/model/DashboardUiModels.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapper.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapperTest.kt`

**Interfaces:**
- Produces: `TransactionRowUi(id: Long?, merchant: String, category: String, amountWon: Long, method: String, iconText: String)`

- [x] **Step 1: Add failing test for row id mapping**
- [x] **Step 2: Run test and confirm failure**
- [x] **Step 3: Add nullable id to `TransactionRowUi` and mapper**
- [x] **Step 4: Run test and confirm pass**

### Task 2: Edit Rules

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/transactions/EditTransactionUseCase.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/transactions/EditTransactionUseCaseTest.kt`

**Interfaces:**
- Produces: `suspend fun update(transaction: MoneyTransaction, amountWon: Long, categoryText: String, memo: String)`
- Produces: `suspend fun exclude(transaction: MoneyTransaction)`

- [x] **Step 1: Add failing edit/exclude tests**
- [x] **Step 2: Run test and confirm failure**
- [x] **Step 3: Implement update and exclude rules**
- [x] **Step 4: Run test and confirm pass**

### Task 3: Edit Dialog UI

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/components/MoneyVisuals.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`

**Interfaces:**
- Consumes: `EditTransactionUseCase`
- Produces: tappable transaction rows and edit dialog actions

- [x] **Step 1: Add optional `onClick` to `TransactionRow`**
- [x] **Step 2: Add `EditTransactionUseCase` to app container and root**
- [x] **Step 3: Open dialog from real transaction rows**
- [x] **Step 4: Save edits and exclude through the use case**

### Task 4: Verification

**Files:**
- Modify: `docs/superpowers/plans/2026-07-02-transaction-edit.md`

- [x] **Step 1: Run unit tests and debug build**
- [x] **Step 2: Verify edit and exclude on emulator**
- [x] **Step 3: Clear emulator data and temporary files**

**Verification notes:**
- `:app:testDebugUnitTest :app:assembleDebug` passed.
- Emulator verified: tapping a real transaction opens the edit dialog, saving closes the dialog and shows the edit confirmation.
- Emulator verified: excluding a transaction marks it as `EXCLUDED`, changes direction to `NEUTRAL`, and shows it as `지출 제외` instead of monthly spending.
