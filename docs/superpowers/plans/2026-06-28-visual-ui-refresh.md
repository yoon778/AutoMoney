# Visual UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the MVP feel like a polished money-management app by adding visual hierarchy, icons, chart-like cards, vector illustrations, and Android Studio previews.

**Architecture:** Keep the existing single-activity Compose app. Add small reusable UI components and sample display models under `ui/components` and `ui/model`, then wire screens to those components. Use Android vector drawables for lightweight illustrations.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android vector drawables, JUnit unit tests, Gradle wrapper.

## Global Constraints

- Keep the app offline-first and local.
- Keep UI dense enough for repeated money review work.
- Do not add a heavy image dependency.
- Use compile/build verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Visual Models And Formatting

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/model/DashboardUiModels.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/ui/model/DashboardUiModelsTest.kt`

**Interfaces:**
- Produces: `formatWon(amount: Long): String`, `HomeSnapshot`, `ReviewCardUi`, `CategorySpendUi`.

- [x] **Step 1: Write failing tests**

Test won formatting and progress clamping.

- [x] **Step 2: Verify test fails before implementation**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "*DashboardUiModelsTest"`

- [x] **Step 3: Implement models and helpers**

Add immutable UI display models used by Compose screens.

- [x] **Step 4: Verify tests pass**

Run the same unit test command.

### Task 2: Shared Components And Vector Illustrations

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/components/MoneyVisuals.kt`
- Create: `app/src/main/res/drawable/illustration_cash_flow.xml`
- Create: `app/src/main/res/drawable/illustration_notification_flow.xml`

**Interfaces:**
- Consumes: models from Task 1.
- Produces: `MoneyHeroCard`, `MetricTile`, `TransactionRow`, `CategoryBar`, `ReviewActionCard`, `EmptyStateVisual`.

- [x] **Step 1: Add reusable Compose components**

Create compact, Material 3-friendly components with stable sizing.

- [x] **Step 2: Add vector drawable assets**

Use simple blue/green finance and notification illustrations.

### Task 3: Screen Refresh

**Files:**
- Modify: `ui/AppRoot.kt`
- Modify: `ui/home/HomeScreen.kt`
- Modify: `ui/transactions/TransactionsScreen.kt`
- Modify: `ui/review/ReviewScreen.kt`
- Modify: `ui/report/MonthlyReportScreen.kt`
- Modify: `ui/settings/SettingsScreen.kt`
- Modify: `ui/theme/Theme.kt`

**Interfaces:**
- Consumes: components and UI models from Tasks 1 and 2.

- [x] **Step 1: Replace text-only cards**

Use hero, metric tiles, category rows, review action cards, and visual empty/permission sections.

- [x] **Step 2: Fix bottom navigation**

Use Material icons so labels do not split awkwardly.

### Task 4: Preview Catalog And Verification

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/PreviewCatalog.kt`

**Interfaces:**
- Produces: Android Studio previews for Home, Transactions, Review, Report, Settings, and AppRoot.

- [x] **Step 1: Add `@Preview` entries**

Add phone-sized previews using `AutoMoneyTheme`.

- [x] **Step 2: Run verification**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.
