# Transaction Edit Design

## Goal
Let the user correct saved transactions without leaving the transaction screen.

## Behavior
- Tapping a real transaction row opens a transaction edit dialog.
- The first editable fields are amount, category, and memo.
- Saving updates the existing transaction and marks it as `USER_EDITED`.
- Deleting does not physically remove the transaction. It marks the transaction as `EXCLUDED`, clears category, and removes it from spend totals.
- Sample preview rows remain read-only.

## Data Flow
- `TransactionRowUi` carries the original transaction id.
- `TransactionsScreen` maps the tapped row id back to the current `MoneyTransaction`.
- `EditTransactionUseCase` applies edit and exclude rules, then calls `MoneyRepository.updateTransaction`.
- Existing Room Flow refreshes the recent transaction list, home summary, report, and calendar.

## Initial Scope
- No date editing in this step.
- No merchant editing in this step.
- No physical delete in this step.
- Excluded transactions stay in the database so future recovery can be added.

## Testing
- Unit test row id mapping.
- Unit test edit and exclude rules.
- Build and emulator-check the tap, edit, save, and exclude flow.
