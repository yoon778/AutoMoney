# Review Dismiss And Calendar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove handled review cards after user action and show monthly spend calendars on Home and Report screens.

**Architecture:** Add small UI model helpers for review-card removal and calendar summaries. Keep calendar data sample-based for now, then render it through a reusable Compose calendar component used by Home and Report.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit + Truth, Gradle wrapper.

## Global Constraints

- Handled review cards disappear from the visible review list.
- Wallet topup usage still records only the actual used amount as spend.
- Calendar is sample UI data first; DB-driven calendar is a later step.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Model Helpers

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/model/DashboardUiModels.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/ui/model/DashboardUiModelsTest.kt`

**Interfaces:**
- Produces: `ReviewCardUi.id: String`
- Produces: `fun dismissReviewCard(cards: List<ReviewCardUi>, cardId: String): List<ReviewCardUi>`
- Produces: `data class DailySpendUi(day: Int, amountWon: Long, label: String)`
- Produces: `data class MonthlySpendCalendarUi(monthTitle: String, daysInMonth: Int, firstWeekdayOffset: Int, dailySpends: List<DailySpendUi>)`
- Produces: `fun MonthlySpendCalendarUi.spendForDay(day: Int): DailySpendUi?`
- Produces: `fun MonthlySpendCalendarUi.totalSpendWon(): Long`

- [x] Write failing model tests.
- [x] Run model tests and confirm RED.
- [x] Implement minimal model helpers and sample calendar data.
- [x] Run model tests and confirm GREEN.

### Task 2: Review Card Dismissal

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/review/ReviewScreen.kt`

**Interfaces:**
- Consumes: `dismissReviewCard(cards, card.id)`

- [x] Store review cards in screen state.
- [x] Remove wallet topup card after valid usage save.
- [x] Remove wallet topup card after `아직 안 씀`.
- [x] Remove transfer card after current placeholder actions.
- [x] Show empty state only after all cards are handled.

### Task 3: Calendar UI

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/components/SpendingCalendar.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/report/MonthlyReportScreen.kt`

**Interfaces:**
- Consumes: `sampleSpendCalendar`
- Consumes: `MonthlySpendCalendarUi.spendForDay(day)`
- Consumes: `MonthlySpendCalendarUi.totalSpendWon()`

- [x] Add reusable selectable calendar card.
- [x] Add compact calendar to Home.
- [x] Add monthly calendar to Report.

### Task 4: Verification

- [x] Run `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`.
- [x] Install debug APK on emulator.
- [x] Verify review card disappears after `아직 안 씀`.
- [x] Verify review card disappears after usage save.
- [x] Verify Home and Report calendars render.
