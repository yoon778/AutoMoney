# Notification Access MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show notification access state in Settings and make real notification snapshot extraction more reliable.

**Architecture:** Keep the existing listener and ingestion use case. Add a pure Kotlin snapshot builder for testable notification text extraction, plus a small Android permission checker for Settings.

**Tech Stack:** Kotlin, Android NotificationListenerService, Jetpack Compose Material3, JUnit, Truth.

## Global Constraints

- Keep ingestion limited to Toss package notifications in this step.
- Do not add a full notification history screen.
- Do not change the Room schema.
- Preserve the existing sample notification test flow.

---

### Task 1: Snapshot Builder

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationSnapshotBuilder.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationSnapshotBuilderTest.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`

**Interfaces:**
- Produces: `data class NotificationContentFields(...)`
- Produces: `class NotificationSnapshotBuilder { fun build(fields: NotificationContentFields): NotificationSnapshot }`

- [x] **Step 1: Add failing tests for snapshot builder**
- [x] **Step 2: Run tests and confirm failure**
- [x] **Step 3: Implement `NotificationSnapshotBuilder`**
- [x] **Step 4: Run tests and confirm pass**
- [x] **Step 5: Use builder from listener service**

### Task 2: Permission Checker

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationAccessChecker.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationAccessCheckerTest.kt`

**Interfaces:**
- Produces: `class NotificationAccessChecker(private val context: Context) { fun isNotificationAccessEnabled(): Boolean }`
- Produces: `internal fun isListenerEnabledInSetting(enabledListeners: String?, packageName: String, listenerClassName: String): Boolean`

- [x] **Step 1: Add failing parser tests for enabled listener settings**
- [x] **Step 2: Run tests and confirm failure**
- [x] **Step 3: Implement checker and parser helper**
- [x] **Step 4: Run tests and confirm pass**

### Task 3: Settings UI State

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `notificationAccessEnabled: Boolean?`
- Produces: Settings card that displays `권한 켜짐` or `권한 꺼짐`

- [x] **Step 1: Pass permission state from AppRoot to SettingsScreen**
- [x] **Step 2: Refresh permission state on app resume**
- [x] **Step 3: Render permission status in SettingsScreen**
- [x] **Step 4: Verify preview/default call remains valid**

### Task 4: Verification

**Files:**
- Modify: `docs/superpowers/plans/2026-07-02-notification-access.md`

- [x] **Step 1: Run unit tests and debug build**
- [x] **Step 2: Install on emulator and check Settings tab**
- [x] **Step 3: Clear emulator data and temporary files**

**Verification notes:**
- RED/GREEN verified for `NotificationSnapshotBuilderTest`.
- RED/GREEN verified for `NotificationAccessCheckerTest`.
- `:app:testDebugUnitTest :app:assembleDebug` passed.
- Emulator verified Settings tab shows `권한 꺼짐` and `권한 설정 열기`.
- Emulator verified `권한 설정 열기` opens Android notification access settings and lists `AutoMoney`.
