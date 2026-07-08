# AutoMoney Visual Refresh Progress

## Goal

Make the app feel like one coherent Korean fintech product: clean white surfaces, pale blue canvas, mint asset accents, coral review accents, small friendly illustrations, and clear money-flow hierarchy.

## Current Rule

Do not install every small change to the phone. Build and verify locally. Install APK only when user asks: "업데이트 설치해줘".

## Approved Direction

- Home should act as the visual face of the app.
- Overall feeling: Toss-like clean structure, but not copied.
- Use small visual accents, not heavy illustration.
- Keep 8dp card radius.
- Avoid purple-dominant palette, heavy gradients, or decorative blobs.

## Phase 1 Done

- Added shared visual tokens in `app/src/main/java/com/choiyoonseo/automoney/ui/components/MoneyVisuals.kt`.
- Added `MoneyFlowHeroCard`.
- Added `FinanceSectionCard`.
- Updated home screen to use:
  - pale app canvas
  - "이번 달 돈 흐름" hero card
  - flow pills for 지출/저축/검토
  - unified section cards for review and recent transactions
- Updated app theme background to `#F7F9FC`.

## Phase 2 Done

- Refreshed `AssetsScreen`.
  - Added pale canvas background.
  - Added total asset summary section.
  - Added mint account balance bars.
  - Wrapped account, fixed expense, monthly plan lists in `FinanceSectionCard`.
  - Kept account edit, fixed expense account dropdown, and save flows.
- Refreshed `ReviewScreen`.
  - Added pale canvas background.
  - Added review summary section.
  - Changed review cards to soft coral background.
  - Kept memo, edit, wallet topup, and account transfer actions.
- Refreshed `TransactionsScreen`.
  - Added pale canvas background.
  - Replaced large elevated blocks with `FinanceSectionCard`.
  - Kept manual expense account picker.
- Refreshed `MonthlyReportScreen`.
  - Added pale canvas background.
  - Switched report hero to `MoneyFlowHeroCard`.
  - Replaced category block with `FinanceSectionCard`.
  - Removed purple chart color from report palette.
- Refreshed `SettingsScreen`.
  - Added pale canvas background.
  - Replaced settings blocks with `FinanceSectionCard`.
- Refreshed bottom navigation colors in `AppRoot`.
- Refreshed `SpendingCalendarCard` to white card with shared soft blue cells.

## Phase 3 Done

- Added optional Material icon support to `FinanceSectionCard`.
- Added section icons across Home, Assets, Review, Transactions, Report, and Settings.
- Set `Scaffold` container color to shared pale canvas.
- Set Assets tab row to white/blue styling.

## Phase 4 Done

- Added richer hero illustrations:
  - `illustration_wallet_coins.xml`
  - `illustration_bank_mint.xml`
  - `illustration_review_magnifier.xml`
- Changed Home hero image from chart-style art to wallet/coins art.
- Added `IllustratedSummaryCard` shared component.
- Changed Assets top summary into bank illustration hero card.
- Changed Review top summary into magnifier/warning illustration hero card.

## Phase 5 Done

- Added richer `MonthlyFlowCard` for Home.
  - Uses wallet illustration.
  - Shows period, remaining money, income, spending, savings/transfer flow.
- Added circular badges to asset/fixed/monthly rows.
- Added review filter chips below review hero.
- Added notification bell visual to `ScreenTitle`.

## Phase 6 Done

- Added deterministic color rules in `FinancePalette`.
  - Account colors now stay stable by brand/name.
  - Category colors now stay stable across transactions, report bars, asset plans, and calendar days.
  - Review colors now separate topup, transfer, refund, duplicate/review states.
- Updated transaction rows, review cards, review chips, asset rows, report category bars, and calendar cells to use the shared palette.
- Added unit tests for the color rules.

## Phase 7 Done

- Added small home flow illustrations:
  - `illustration_flow_income.xml`
  - `illustration_flow_expense.xml`
  - `illustration_flow_saving.xml`
- Changed Home monthly flow steps from text-only badges to illustrated income, expense, and savings/transfer badges.
- Added short flow connectors between the three steps.
- Added unit tests for home flow step labels, values, colors, and image resources.

## Functional Fixes Done

- Added asset balance sync rules.
  - Confirmed expenses subtract from the matched account.
  - Confirmed income adds to the matched account.
  - Review-pending transactions do not affect balances until resolved.
  - Transaction edits and deletes reverse the previous account-balance effect.
- Changed monthly summary math.
  - `savingWon` now means actual `SAVING` transactions only.
  - `netWon` now means income minus expense minus saving.
  - Expense-only months no longer show the expense amount as saving.
- Added month navigation to the monthly report.
- Added Home and Report detail dialogs for amount/category drill-down.
- Removed release-inappropriate cards from Transactions and Settings.
- Removed the unused top-right notification icon from screen titles.
- Improved review and transaction readability.
  - Review cards now show the source payment account when known.
  - Transfer reviews fall back to memo text when counterparty/title fields are blank.
  - Transaction rows now use merchant, counterparty, memo, income category, then transaction type fallback so resolved reviews do not appear titleless.
- Simplified Home monthly flow card by removing the nested blue inner panel.

## Next Phase

1. Optional visual QA on phone or emulator.
2. Tighten spacing/text after seeing real screen.
3. Replace vector illustrations with generated PNG/WEBP assets if a more 3D look is needed.

## Verification Commands

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```

## Install Command

Run only when user explicitly asks to install/update:

```powershell
& 'C:\Users\cys04\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r 'C:\Users\cys04\OneDrive\Desktop\AutoMoney\app\build\outputs\apk\debug\app-debug.apk'
```

## Phase 8 Done (Toss token restyle — foundation + Home)

- Added `ui/theme` token layer: `MoneyColors` (light+dark), Pretendard `Typography`, `Shapes` (18-20dp), `Spacing`.
- `AutoMoneyTheme` now supports `darkTheme`; live app still defaults to light until all screens convert.
- Added `SoftShadowCard`; migrated Home components (ScreenTitle, FinanceSectionCard, MetricTile, TransactionRow, MonthlyFlowCard) to tokens + rounder cards.
- Reworked Home canvas and bottom nav to tokens; added light+dark Home previews.
- Supersedes the earlier "keep 8dp radius" rule.
