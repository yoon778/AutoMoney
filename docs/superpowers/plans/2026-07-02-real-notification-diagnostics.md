# Real Notification Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Display the latest real Toss notification processing result in Settings.

**Architecture:** Add a lightweight SharedPreferences diagnostics store for one latest notification record. The listener writes diagnostics after ingestion, and AppRoot refreshes the record for Settings on foreground resume.

**Tech Stack:** Kotlin, Android SharedPreferences, NotificationListenerService, Jetpack Compose Material3, JUnit, Truth.

## Global Constraints

- Do not change the Room schema.
- Do not add a notification history screen.
- Keep UI diagnostics limited to Toss notifications.
- Keep sample notification testing intact.

---

### Task 1: Diagnostic Model And Serialization

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt`

**Interfaces:**
- Produces: `enum class NotificationDiagnosticResult`
- Produces: `data class LastNotificationDiagnostic(...)`
- Produces: `internal fun LastNotificationDiagnostic.toPreferenceMap(): Map<String, String>`
- Produces: `internal fun lastNotificationDiagnosticFromPreferenceMap(values: Map<String, String?>): LastNotificationDiagnostic?`

- [x] **Step 1: Add failing serialization and mapping tests**
- [x] **Step 2: Run tests and confirm failure**
- [x] **Step 3: Implement diagnostic model and serialization helpers**
- [x] **Step 4: Run tests and confirm pass**

### Task 2: Diagnostics Store And Listener Writes

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`

**Interfaces:**
- Consumes: `NotificationDiagnosticsStore`
- Produces: listener writes last notification diagnostic for saved, duplicate, ignored, and error results

- [x] **Step 1: Add `NotificationDiagnosticsStore` to AppContainer**
- [x] **Step 2: Write diagnostic after successful ingestion result**
- [x] **Step 3: Catch runtime errors and write `ERROR` diagnostic**

### Task 3: Settings Diagnostics Card

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/PreviewCatalog.kt`

**Interfaces:**
- Consumes: `lastNotificationDiagnostic: LastNotificationDiagnostic?`
- Produces: Settings card titled `실제 알림 점검`

- [x] **Step 1: Pass diagnostics store into AppRoot**
- [x] **Step 2: Refresh latest diagnostic on resume**
- [x] **Step 3: Render no-record Settings card**
- [x] **Step 4: Render saved diagnostic details**

### Task 4: Verification

**Files:**
- Modify: `docs/superpowers/plans/2026-07-02-real-notification-diagnostics.md`

- [x] **Step 1: Run unit tests and debug build**
- [x] **Step 2: Install on emulator and check Settings diagnostics card**
- [x] **Step 3: Clear emulator data and temporary files**

## Verification Notes

- Confirmed RED with targeted `NotificationDiagnosticsStoreTest` before implementation.
- Confirmed GREEN with targeted `NotificationDiagnosticsStoreTest`.
- Confirmed full verification with `:app:testDebugUnitTest :app:assembleDebug`.
- Installed the debug APK on the emulator and verified the Settings screen shows `실제 알림 점검` with the no-record state.
- Real Toss notification capture still needs a physical Galaxy phone test with notification access enabled.
