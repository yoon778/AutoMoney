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
| B3 | ~~Apply the pending review-flow corrections~~ **DONE 2026-07-09:** (1) `ACCOUNT_UNMATCHED` "계좌 확인" now opens the transaction edit dialog (account field included) — reason looked up from `openReviewItems`, no mapper change; (2) memo resolutions now call `ResolveReviewUseCase.resolve` atomically; (3) account transfer now calls `ResolveAccountTransferUseCase.resolve` atomically (paired incoming item included); sample-mode fallbacks kept for previews. Wired through `AppRoot`/`MainActivity`. Verified live on emulator via debug 테스트 알림 (ACCOUNT_UNMATCHED card → edit dialog → save → review count 0). | done |

### C. Consistency — half the app wears the new design

| # | Item | Detail | Owner |
|---|------|--------|-------|
| C1 | ~~Restyle remaining screens to tokens~~ **DONE 2026-07-09:** Assets/Review/Report/Settings canvases, TabRow, InputCard, AssetRow, SpendingCalendar, ReviewActionCard, CategoryBar, MoneyFlowHeroCard/FlowPill all migrated to `MoneyTheme.colors` + 18–20dp. Dark previews added for all screens. | done |
| C2 | ~~Replace legacy illustrations~~ **DONE 2026-07-09:** `EmptyStateVisual`/`IllustratedSummaryCard` now take an `icon: ImageVector` and render colored icon chips. Deleted unused `MoneyHeroCard`, `MonthlyFlowCardOld`, and drawables `illustration_{cash_flow,bank_mint,review_magnifier,notification_flow}`. Home hero keeps `illustration_wallet_coins` + flow images (approved look). | done |
| C3 | ~~Enable live dark mode~~ **DONE 2026-07-09:** `MainActivity` now passes `isSystemInDarkTheme()`. Verified live on emulator (`cmd uimode night yes`): Home/Transactions/Review/Assets/Report/Settings all render dark correctly, including calendar and review empty state. | done |

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

## Notification Pipeline Check (emulator, 2026-07-09)

Question: when a finance notification arrives, does the app recognize → parse → apply it?

**Verified working:**
- Listener service is registered correctly in the manifest (`BIND_NOTIFICATION_LISTENER_SERVICE` + intent filter).
- Granting notification access works: after grant, the OS lists AutoMoney's listener as approved (confirmed via `dumpsys notification`), and the Settings screen flips to "권한 켜짐" on the next resume. The real user flow (권한 설정 열기 → grant → back) pauses/resumes the app, so the status refreshes correctly. (It looked stale in testing only because the grant was injected via adb without leaving the app.)
- A notification from an unsupported package is safely ignored: no crash, process stable. This is by design (`FinancialAppRegistry.isSupportedPackage` gate before any processing).
- The parse → categorize → low-confidence-to-review → duplicate-detect → save chain is unit-tested and green (156/156): `TossNotificationParserTest`, `CommonFinanceNotificationParserTest`, `NotificationParserRouterTest`, `FinancialAppRegistryTest`, `MoneyNotificationListenerServiceTest`, `NotificationIngestionAtomicityTest`, `NotificationSnapshotBuilderTest`, `NotificationDiagnosticsStoreTest`.

**Limitation of this check:**
- A full live end-to-end run (real Toss/KB notification → row appears in the app) could NOT be exercised on the emulator: `adb shell cmd notification post` posts from `com.android.shell`, and the posting package cannot be spoofed, so the registry gate (correctly) drops it. The logic below the gate is covered by unit tests, but the final on-device confirmation still needs a real phone with the Toss or KB app installed.

**Findings / suggestions (for Codex):**
| # | Item | Detail | Owner |
|---|------|--------|-------|
| N1 | Unsupported finance apps are dropped with zero trace | The package gate returns before the snapshot is built, so nothing reaches `NotificationDiagnosticsStore`. A user whose bank is not Toss/KB sees "아직 처리한 금융 앱 알림이 없어요" forever with no clue why. Consider recording a lightweight "last ignored package" diagnostic (or a counter) so Settings can say "지원하지 않는 앱의 알림이 왔어요". | [LOGIC] |
| N2 | Registry covers only 2 apps | `FinancialAppRegistry` = Toss + KB국민은행 only. Samsung Pay, KakaoBank, KakaoPay, NaverPay app notifications never enter the pipeline (topups are only caught indirectly via Toss/KB messages). Expanding is a product decision — flagging so it is a decision, not an accident. | [LOGIC] |
| N3 | No in-app way to demo/verify the pipeline | `SampleNotificationScenarios` exists but is referenced only by its own test. A debug-only "테스트 알림 넣기" button in Settings that pushes a sample scenario through `NotificationIngestionUseCase` would let anyone verify recognize→parse→apply on any device in seconds (and double as a first-run demo). | [LOGIC] exposes, [UI] renders |
| N4 | Real-device smoke test still pending | Suggest one manual pass on the physical phone (SM_S931N known from earlier): trigger a small real payment/transfer notification from Toss or KB, then check 거래 tab and Settings' "최근 알림 결과". | user + both agents |

**Files for Codex to read (pipeline, in order):**
1. `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt` — entry point; package gate; diagnostics write (N1 lives here)
2. `app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt` — supported-app allowlist (N2)
3. `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationSnapshotBuilder.kt` — raw notification → snapshot
4. `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouter.kt` + `TossNotificationParser.kt` + `CommonFinanceNotificationParser.kt` — text → transaction draft
5. `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt` — rules, low-confidence→review, duplicate handling, save
6. `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt` — what Settings' "최근 알림 결과" shows
7. `app/src/main/java/com/choiyoonseo/automoney/notification/SampleNotificationScenarios.kt` — unused in-app; candidate for N3
8. Tests mirroring each of the above under `app/src/test/java/com/choiyoonseo/automoney/{notification,domain/parser}/`

## Code Audit — dead code & migrations (2026-07-09)

Whole-repo pass (UI + Codex areas). Method: usage grep per public symbol + file-size scan + migration SQL vs entity declarations.

**Dead code — [UI] Claude can delete:**
| Item | Evidence | Action |
|---|---|---|
| `SoftShadowCard.kt` | 0 call sites (cards ended up on `Surface`) | delete, or adopt in Home cards — pick one |
| Color vals `MoneyInk, MoneyCanvas, MoneySoftBlue, MoneySoftMint, MoneySoftCoral, MoneySoftYellow` (MoneyVisuals/FinancePalette) | 0 uses after token migration | delete vals |

**Dead code — [LOGIC] Codex decision:**
| Item | Evidence | Action |
|---|---|---|
| `export/CsvExporter.kt` | 0 prod call sites (only its own test) | delete, or wire an export button later — decide |
| `notification/DailyReviewNotifier.kt` | 0 call sites; daily reminder never wired | delete, or wire (it was a captured requirement — "review reminder") |
| `WalletDao` + `WalletEntity` + `wallets` table | dao exposed by `AppDatabase`, zero callers anywhere; topup flow uses transactions instead | code can go now; dropping the table needs migration v5 — fine to defer, harmless |

**Oversized files (split candidates, [UI]):**
- `ReviewScreen.kt` 862 lines — extract 4 dialogs (wallet usage, unused memo, review memo, account transfer) into own files (= existing D2)
- `MoneyVisuals.kt` 725 lines — many unrelated components in one file; split by component when convenient

**Not dead (checked, alive):** `accountOptionsForEdit`/`accountLabelForEdit`, `ManualTransactionFormDefaults`, `WalletTopupReviewService` (preview fallback + use case), all `FinancePalette` accent functions, sample data (previews).

**Migrations re-check (v1→4): SOUND.**
- `MIGRATION_1_2`: creates 3 asset tables — matches entities.
- `MIGRATION_2_3`: dedupes `sourceNotificationHash` before unique index (keeps MIN id); rebuilds `review_items` with FK `ON DELETE CASCADE` + unique `transactionId`, keeping newest-unresolved row per transaction; drops orphans. Careful, correct.
- `MIGRATION_3_4`: 4 perf indices — names/columns match entity `indices` declarations exactly (Room schema validation passes).
- `exportSchema = true`, schema JSONs in repo, on-device `AppDatabaseMigrationTest` exists and passed (2026-07 run).
- Only smell: `wallets` table is dead schema surface (above).

## Suggested Order

**A1 → A2 → A3** (first impression) → **B3** (review-flow correctness) → **C1 → C2 → C3** (full visual consistency + dark) → **B1 → B2** → **D**.

## Notes for Codex

- B1 needs a mapper/UI-model change (source flag on `TransactionRowUi`) — Claude will claim the contract and ask before touching anything in `ui/model/**`.
- A1 may want a tiny persistent "onboarding seen" store analogous to `SharedPreferencesWalletTopupNoticeStore`; if you prefer, expose it from `AppContainer` and Claude will consume it.
- Everything in C/D-UI stays inside `ui/**` and will not touch logic, mappers, or the DB.

---

## Codex Update - 2026-07-09

Implemented logic/contract items for Claude to consume:

- A1: Added `NotificationOnboardingStore` + `SharedPreferencesNotificationOnboardingStore`; `AppRoot` now passes first-run notification-access onboarding state to `HomeScreen`.
- A2: `HomeScreen` and `TransactionsScreen` receive `notificationAccessEnabled` + `onOpenNotificationSettings`; empty states can show the settings CTA when access is off.
- B1: `TransactionRowUi.sourceLabel` added. Mapper sets it to `"자동"` for `SourceType.NOTIFICATION`; manual/import rows stay `null`. `TransactionRow` renders this as a small badge.
- B2: Added `AutoClearMessageEffect` with `TRANSIENT_MESSAGE_DURATION_MILLIS = 3_000L`; wired to `TransactionsScreen`, `AssetsScreen`, and `ReviewScreen` status messages.
- B4: Added `AppDateZoneId = Asia/Seoul`; manual entry, transaction date sections, Home, Report, and spending calendar now use the same app date zone instead of `ZoneId.systemDefault()`.
- N1: Unsupported notification packages now still build a snapshot and write `LastNotificationDiagnostic.fromUnsupportedPackage(...)` with result `IGNORED`.
- N3: Added `RunSampleNotificationScenarioUseCase`; `SettingsScreen` can receive a debug sample-run callback and `AppRoot` wires it when `BuildConfig.DEBUG`.

Claude next:

- Polish the exact copy/layout of A1/A2 onboarding and empty states.
- Continue B3 review-flow UI corrections from `docs/AI_COLLABORATION.md`.
- Continue C1/C2/C3 visual token migration and dark-mode readiness.
- Keep `TransactionRowUi.sourceLabel` and `AppDateZoneId` contracts intact.
