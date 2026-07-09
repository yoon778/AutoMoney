# App Review & Fix List

- Date: 2026-07-09
- Author: Claude (UI agent), reviewing as both a senior app developer and an end user
- Audience: user, Claude, Codex
- Scope: whole-app evaluation of AutoMoney after the Toss-style restyle of Home + Transactions

## Overall Assessment

**What works well (keep):**
- The core loop is solid and fully wired: read finance notifications → auto-create transactions → ambiguous ones go to Review → balances and monthly reports update. This is the heart of the app and it works.
- Money rules are careful: wallet topups/points excluded from real spending, balance sync on confirm/edit/delete, report classification rules with tests.
- Home + Transactions now follow the new token design (Pretendard, 18–20dp cards, light/dark tokens).

**Main weakness, in one line:** *the app is smart, but unfriendly to a first-time user.* A new user who has not granted notification access sees an empty home screen with no explanation and no path forward.

---

## Fix List (prioritized)

Ownership legend: **[UI]** = Claude, **[LOGIC]** = Codex, **[SHARED]** = contract change needed (claim first per `docs/AI_COLLABORATION.md`).

### A. Critical — first-run experience (do first)

| # | Item | Detail | Owner |
|---|------|--------|-------|
| A1 | First-run onboarding for notification access | If notification access is off, nothing gets recorded, but the only hint lives inside the Settings screen. Add a first-run notice (dialog or home banner) explaining "allow notification access → transactions record themselves", with a button that opens the system settings screen (`onOpenNotificationSettings` already exists in `AppRoot.kt`). Show until granted or explicitly dismissed. | [UI], may need a small "seen" store like `WalletTopupNoticeStore` [LOGIC] |
| A2 | Actionable empty states | Empty lists currently say only "아직 이번 달 기록이 없어요." When the DB is empty AND access is off, the empty state should say automatic recording needs permission and offer the same settings button. `notificationAccessEnabled` is already computed in `AppRoot.kt` — needs passing down to Home/Transactions. | [UI] |
| A3 | ~~Settings is unreachable from the bottom nav~~ **CORRECTED 2026-07-09:** wrong — the live app has 6 tabs including 설정. Verified on emulator. No action needed. | — | — |

### B. Trust — "is the automatic record correct?"

| # | Item | Detail | Owner |
|---|------|--------|-------|
| B1 | Distinguish auto vs manual transactions | Rows look identical whether parsed from a notification or entered by hand. Add a small "자동" badge for notification-sourced rows. `TransactionRowUi` likely needs a source flag — that is a UI-model contract change: claim it, Codex updates the mapper, Claude renders it. | [SHARED] |
| B2 | Status messages never disappear | Save/edit confirmations render as an `AssistChip` that stays forever and is not dismissible (`TransactionsScreen.kt`, `AssetsScreen.kt`, `ReviewScreen.kt`). Auto-dismiss after ~3s (or switch to Snackbar). | [UI] |
| B4 | Timezone consistency between manual-entry date and list/home grouping | Observed on a UTC-timezone emulator (2026-07-09): a manual transaction saved with the "오늘" chip landed in the "7월 8일" date section, and Home's "오늘 사용" stayed 0원 while "최근 7일" showed the amount. Suggests the manual form's zone (`manualTransactionZoneId`) and the grouping zone (`ZoneId.systemDefault()` in `transactionsToDateSections` / home calculators) can disagree. Invisible on a KST device, but worth unifying on one zone source. | [LOGIC] |
| B3 | Apply the pending review-flow corrections | The four items Codex documented in `docs/AI_COLLABORATION.md` → "Claude UI Notes": (1) `ACCOUNT_UNMATCHED` cards must route to an account-focused edit, not the generic memo confirm; (2) review actions should call the atomic review use cases from `AppContainer` instead of `updateTransaction()` + `resolveReviewItem()` separately; (3) account-transfer review should call the atomic transfer use case; (4) visual polish stays in `ReviewScreen.kt`. These are behavior-correctness issues. | [UI] consuming [LOGIC] use cases already exposed |

### C. Consistency — half the app wears the new design

| # | Item | Detail | Owner |
|---|------|--------|-------|
| C1 | Restyle remaining screens to tokens | `AssetsScreen`, `ReviewScreen`, `MonthlyReportScreen`, `SettingsScreen` still use legacy literals (`MoneyCanvas`, 8dp corners, light-only colors). Migrate them to `MoneyTheme.colors` + 18–20dp like Home/Transactions. Until then, tab switches feel like two different apps and dark mode cannot ship. | [UI] |
| C2 | Replace legacy illustrations | `EmptyStateVisual` / `IllustratedSummaryCard` still use the old 3D-ish vector drawables (`illustration_*`), which clash with the new flat icon-chip style. Replace with colored icon chips; then delete unused drawables and legacy composables (`MonthlyFlowCardOld`, `MoneyHeroCard`, `MoneyFlowHeroCard`, `IllustratedSummaryCard` if unreferenced). | [UI] |
| C3 | Enable live dark mode | `AutoMoneyTheme(darkTheme=)` already supports dark; it is intentionally not wired to the system setting until every screen is token-based. After C1/C2, flip `MainActivity` to `isSystemInDarkTheme()`. `MainActivity.kt` is a boundary zone — claim first. | [UI] |

### D. Later — housekeeping and scale

| # | Item | Detail | Owner |
|---|------|--------|-------|
| D1 | List performance | Screens render transaction lists with `Column` + `forEach` inside `verticalScroll`. Fine now; will degrade with hundreds of rows. Consider `LazyColumn` when C1 touches these screens. | [UI] |
| D2 | Split `ReviewScreen.kt` (831 lines) | Extract the dialogs (wallet usage, memo, transfer) into their own files for maintainability. | [UI] |
| D3 | Build-config deprecation warnings | Gradle warns about `android.builtInKotlin=false`, `android.newDsl=false`, and the legacy `applicationVariants` API (AGP 10 removal). Not urgent; app-level build config. | [LOGIC] |

---

## Verified Working (emulator run, 2026-07-09, Pixel_7 AVD)

Manual end-to-end check on the debug build (commit `e69a112` era):
- Install, launch, and full tab navigation (홈/거래/검토/자산/보고서/설정) — no crashes, empty crash buffer, process stable throughout.
- Transactions: restyled header + blue `+` renders; one-time 충전/포인트 notice appeared exactly once on first visit; `+` opens the manual form; type chips are 지출/수입/이체 only (no WALLET_TOPUP).
- Saved a manual 5,000원 expense end-to-end: success message shown, row appears in a date section (red negative amount), Home hero updates (지출 5,000원 / 남은 돈 -5,000원 / 최근 7일 5,000원), Report updates (지출 5,000원, calendar day badge "5천").
- Settings correctly reports notification access off and offers the settings button.
- B4 (timezone) was discovered during this run.

## Suggested Order

**A1 → A2 → A3** (first impression) → **B3** (review-flow correctness) → **C1 → C2 → C3** (full visual consistency + dark) → **B1 → B2** → **D**.

## Notes for Codex

- B1 needs a mapper/UI-model change (source flag on `TransactionRowUi`) — Claude will claim the contract and ask before touching anything in `ui/model/**`.
- A1 may want a tiny persistent "onboarding seen" store analogous to `SharedPreferencesWalletTopupNoticeStore`; if you prefer, expose it from `AppContainer` and Claude will consume it.
- Everything in C/D-UI stays inside `ui/**` and will not touch logic, mappers, or the DB.
