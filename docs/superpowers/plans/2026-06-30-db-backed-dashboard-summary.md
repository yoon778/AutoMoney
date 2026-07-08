# DB Backed Dashboard Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace sample Home/Report summary numbers and category bars with values calculated from saved transactions.

**Architecture:** Add a UI mapper that converts monthly transactions plus open review count into dashboard/report UI models. Add a repository Flow for open review count. Home and Report reuse the same calculated summary while keeping sample fallback for previews.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Kotlin Flow, Room, JUnit + Truth, Gradle wrapper.

## Global Constraints

- `WALLET_TOPUP`, transfer, saving, investment, refund, and excluded transactions do not count as spending.
- `EXPENSE`, `FIXED_EXPENSE`, and `WALLET_SPEND` count as spending.
- Income uses `TransactionDirection.INCOME`.
- UI preview still works with sample data when repository is null.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Monthly Summary Mapper

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapper.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapperTest.kt`

- [x] Write failing tests for income/expense/net, topup exclusion, category grouping, and recent rows.
- [x] Run mapper tests and confirm RED.
- [x] Implement mapper.
- [x] Run mapper tests and confirm GREEN.

### Task 2: Review Count Flow

**Files:**
- Modify: `data/repository/MoneyRepository.kt`
- Modify: `data/repository/RoomMoneyRepository.kt`
- Modify fake repository tests.

- [x] Add `observeOpenReviewCount(): Flow<Int>`.
- [x] Implement via `ReviewItemDao.observeOpenItems().map { it.size }`.
- [x] Update fake repository.

### Task 3: Home/Report Wiring

**Files:**
- Modify: `ui/home/HomeScreen.kt`
- Modify: `ui/report/MonthlyReportScreen.kt`

- [x] Collect monthly transactions and review count.
- [x] Use summary mapper for Home hero/metrics/recent transactions.
- [x] Use summary mapper for Report hero/metrics/category bars.

### Task 4: Verification

- [x] Run full Gradle verification.
- [x] Install APK on emulator.
- [x] Save wallet usage and verify Home/Report summary changes from DB.
