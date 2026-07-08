# Notification Access MVP Design

## Goal

Make the real notification ingestion path usable enough for device testing: the user can see whether notification access is enabled, open the Android permission screen, and the listener builds a reliable notification snapshot from common Android notification text fields.

## Scope

- Show notification listener permission state in Settings.
- Refresh permission state when the app returns to the foreground.
- Keep the existing Android notification listener service.
- Improve notification text extraction by including title, text, big text, and text lines.
- Keep ingestion limited to Toss package notifications for this step.
- Do not add a full notification history or debug log screen yet.

## Recommended Approach

Use a small Android-facing permission checker in the UI layer and a pure Kotlin snapshot builder for notification text extraction. The service should delegate snapshot creation to the builder, then pass the snapshot to the existing `NotificationIngestionUseCase`.

This keeps Android framework code thin and leaves parsing/ingestion testable with ordinary unit tests.

## Components

- `NotificationAccessChecker`
  - Reads `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS`.
  - Returns whether this app's notification listener component is enabled.

- `NotificationSnapshotBuilder`
  - Consumes primitive notification fields: package name, post time, title, text, big text, text lines.
  - Produces `NotificationSnapshot`.
  - Deduplicates repeated text lines so parser input stays clean.

- `MoneyNotificationListenerService`
  - Filters to Toss notifications.
  - Extracts notification extras.
  - Uses `NotificationSnapshotBuilder`.
  - Calls `NotificationIngestionUseCase`.

- `SettingsScreen`
  - Displays `권한 켜짐` or `권한 꺼짐`.
  - Shows a short next action.
  - Keeps the `권한 설정 열기` button.

## Data Flow

1. User opens Settings tab.
2. App checks whether notification listener access is enabled.
3. If disabled, user taps `권한 설정 열기`.
4. Android opens the notification access settings screen.
5. User grants access and returns.
6. App refreshes the state and displays `권한 켜짐`.
7. When Toss posts a notification, `MoneyNotificationListenerService` builds a `NotificationSnapshot`.
8. Existing parser and ingestion logic save or review the transaction.

## Error Handling

- Permission state failures default to disabled.
- Non-Toss notifications are ignored.
- Missing notification text still builds a snapshot; parser can ignore it as `amount not found`.
- Ingestion errors stay isolated inside the listener coroutine and should not crash the app.

## Testing

- Unit test `NotificationSnapshotBuilder`:
  - Builds snapshot from title/text/bigText.
  - Includes text lines.
  - Deduplicates repeated values.
- Unit test `NotificationAccessChecker` through a small pure parser function for enabled listener strings.
- Build verification with `:app:testDebugUnitTest :app:assembleDebug`.
- Emulator check:
  - Settings tab opens.
  - Permission card shows a status line.
  - Permission settings button still opens Android settings.
