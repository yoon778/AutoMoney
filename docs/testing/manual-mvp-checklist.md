# Manual MVP Checklist

## Build

- [ ] `.\gradlew.bat :app:assembleDebug` succeeds.
- [ ] App launches on emulator or Galaxy device.

## Screens

- [ ] Home tab shows monthly summary.
- [ ] Transactions tab shows transaction list and manual input form.
- [ ] Review tab shows ambiguous transaction actions.
- [ ] Report tab shows income, expense, and saving summary.
- [ ] Settings tab shows notification access entry.

## Parser Scenarios

- [ ] Toss card payment sample becomes auto-confirmed expense.
- [ ] Toss transfer sample becomes review item.
- [ ] Naver Pay top-up sample becomes neutral wallet top-up review item.
- [ ] Refund/cancel sample becomes review item.

## Money Semantics

- [ ] Wallet top-up does not increase monthly expense.
- [ ] Wallet spend increases monthly expense.
- [ ] Settlement money does not increase monthly income.
- [ ] Transfer does not increase monthly expense.

## Device Check

- [ ] Galaxy device is visible in `adb devices`.
- [ ] Notification access permission screen opens.
- [ ] Toss notification produces a transaction or review item.
