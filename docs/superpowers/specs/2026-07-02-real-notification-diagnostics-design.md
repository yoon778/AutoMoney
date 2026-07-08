# Real Notification Diagnostics Design

## Goal

Show the latest real Toss notification processing result inside Settings so device testing can answer: did AutoMoney receive the notification, what text did it see, and did ingestion save, ignore, duplicate, or fail?

## Scope

- Add one lightweight diagnostic record for the last Toss notification handled by the listener.
- Store the diagnostic record in `SharedPreferences`.
- Display the diagnostic record in Settings.
- Refresh the diagnostic display when the app returns to the foreground.
- Do not change the Room schema.
- Do not add a full notification history screen yet.
- Do not log non-Toss notifications in the app UI.

## Recommended Approach

Add a `NotificationDiagnosticsStore` that persists one `LastNotificationDiagnostic` record. `MoneyNotificationListenerService` writes to this store after each Toss notification ingestion attempt. `SettingsScreen` reads the latest record through `AppRoot` and renders a compact `실제 알림 점검` card.

This keeps diagnostic data separate from money data. A failed or ignored notification should not create a transaction, but it should leave enough evidence for testing.

## Components

- `LastNotificationDiagnostic`
  - `receivedAt`: when AutoMoney processed the notification.
  - `postedAt`: original notification post time.
  - `packageName`: expected to be Toss.
  - `title`: notification title.
  - `textPreview`: compact combined notification text.
  - `result`: `SAVED`, `DUPLICATE`, `IGNORED`, or `ERROR`.
  - `message`: optional detail.

- `NotificationDiagnosticsStore`
  - Uses `SharedPreferences`.
  - Saves and loads the single latest record.
  - Uses small pure mapping helpers that are unit-tested.

- `MoneyNotificationListenerService`
  - Keeps ignoring non-Toss notifications.
  - Builds a snapshot.
  - Calls `NotificationIngestionUseCase`.
  - Saves diagnostic result even when ingestion throws.

- `SettingsScreen`
  - Adds `실제 알림 점검` card.
  - Shows no-record state before any Toss notification.
  - Shows last time, result, title, and text preview after a Toss notification.

## Data Flow

1. User enables notification access.
2. Toss posts a notification.
3. `MoneyNotificationListenerService` receives it.
4. Listener builds `NotificationSnapshot`.
5. Existing ingestion use case processes the snapshot.
6. Listener saves a diagnostic record with the result.
7. User opens Settings or returns to the app.
8. Settings card shows the latest diagnostic record.

## Error Handling

- Ingestion exceptions are caught in the listener coroutine.
- Error diagnostics store `ERROR` and the exception message when available.
- Corrupt preferences return no diagnostic instead of crashing Settings.
- Long notification text is trimmed to a short preview.

## Testing

- Unit test diagnostic record mapping from `NotificationSnapshot` and `IngestionResult`.
- Unit test preference serialization round-trip without Android framework dependencies.
- Unit test error diagnostic creation.
- Build verification with `:app:testDebugUnitTest :app:assembleDebug`.
- Emulator verification:
  - Settings shows `아직 처리한 토스 알림이 없어요.` initially.
  - Existing sample notification path remains visible.
