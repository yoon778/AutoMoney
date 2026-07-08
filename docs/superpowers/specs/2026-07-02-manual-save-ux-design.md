# Manual Save UX Design

## Goal
After a manual transaction is saved, the app should feel finished: clear the form, show the saved result near the recent transaction list, and avoid making the user manually hunt for confirmation.

## Behavior
- On successful manual save, reset the manual form to its default state.
- Default form state means expense type, today as the selected date, empty amount, default expense category, default income category, empty memo, no input error, and closed popups.
- Move the success message near the top of the transaction screen so it is visible after saving.
- Scroll the transaction screen back to the top after saving so the recent transaction list is visible.
- Keep error behavior unchanged: validation and save errors stay close to the manual form.

## Data Flow
- `ManualTransactionForm` receives a `resetSignal`.
- `TransactionsScreen` increments `resetSignal` only after `SaveManualTransactionUseCase.save` succeeds.
- The transaction list continues to refresh through the existing repository Flow.

## Testing
- Unit test the default manual form values.
- Keep the previous date conversion tests.
- Verify the full save flow on the emulator.
