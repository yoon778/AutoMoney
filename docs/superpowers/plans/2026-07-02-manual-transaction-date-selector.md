# Manual Transaction Date Selector

## Goal
Let the user choose the actual transaction date while adding a manual transaction, then save that date as `occurredAt`.

## Scope
- Add a small date selector to the manual transaction form.
- Support quick choices for today and yesterday.
- Support choosing another date with the Material date picker.
- Convert the selected local date to the start of that day in `Asia/Seoul`.
- Pass the selected instant into `SaveManualTransactionUseCase.save`.

## Plan
- [x] Confirm the manual save path already accepts `occurredAt`.
- [x] Add tests for manual transaction date formatting and conversion.
- [x] Add date helper functions for UI-safe date labels and picker conversion.
- [x] Add date controls to `ManualTransactionForm`.
- [x] Pass selected `occurredAt` through `TransactionsScreen`.
- [x] Run unit tests and debug build.
- [x] Verify the flow in the emulator.

## Verification
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`
- Emulator check: opened the transaction screen, selected yesterday, opened the date picker, saved a 12,000 won manual expense, and confirmed the DB stored `2026-06-30T15:00:00Z` for July 1, 2026 in Korea time.

## Notes
- Keep the form compact so the transaction screen still feels like Toss: quiet, simple, and low-friction.
- Manual dates should not require the user to type text.
