# DB Backed Calendar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace sample Home/Report calendar data with monthly spending data derived from saved transactions.

**Architecture:** Add a repository Flow for monthly transactions, map domain transactions into `MonthlySpendCalendarUi`, and let Home/Report collect DB data when a repository is available. Keep sample data as preview/fallback only.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Kotlin Flow, Room, JUnit + Truth, Gradle wrapper.

## Global Constraints

- `WALLET_TOPUP` and other non-expense transaction types stay excluded from the calendar.
- `WALLET_SPEND` and normal expense-counting transactions appear on the calendar.
- Home/Report can still render previews with sample data when no repository is injected.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Calendar Mapper

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/model/CalendarMapper.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/ui/model/CalendarMapperTest.kt`

**Interfaces:**
- Produces: `fun transactionsToSpendCalendar(month: YearMonth, transactions: List<MoneyTransaction>): MonthlySpendCalendarUi`

- [x] Write failing mapper tests for expense inclusion, topup exclusion, and daily grouping.
- [x] Run mapper tests and confirm RED.
- [x] Implement mapper.
- [x] Run mapper tests and confirm GREEN.

### Task 2: Repository Flow

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/MoneyRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/dao/TransactionDao.kt`
- Modify tests that fake `MoneyRepository`.

**Interfaces:**
- Produces: `fun observeTransactionsForMonth(month: YearMonth): Flow<List<MoneyTransaction>>`

- [x] Add repository interface method.
- [x] Add DAO monthly Flow query.
- [x] Implement Room repository mapping.
- [x] Update fake repositories in tests.

### Task 3: Home/Report DB Calendar

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/report/MonthlyReportScreen.kt`

**Interfaces:**
- Consumes: `MoneyRepository.observeTransactionsForMonth(YearMonth.now())`
- Consumes: `transactionsToSpendCalendar(month, transactions)`

- [x] Pass repository from `MainActivity` to `AppRoot`.
- [x] Pass repository to Home and Report screens.
- [x] Collect monthly transactions as state.
- [x] Render mapped DB calendar, sample fallback only when repository is null.

### Task 4: Verification

- [x] Run full Gradle verification.
- [x] Install APK on emulator.
- [x] Save wallet usage from Review and verify date appears in Home calendar.
- [x] Verify Report calendar renders DB-backed data.
