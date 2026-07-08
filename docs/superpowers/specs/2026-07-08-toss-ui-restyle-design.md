# Toss-style UI Restyle — Design Spec

- Date: 2026-07-08
- Branch: `claude/ui-polish`
- Agent: Claude (UI). Logic/mappers stay Codex-owned.
- Status: approved direction, ready for implementation plan.

## Goal

Restyle AutoMoney's existing Compose UI to a clean, rounded, high-visibility
Toss-like look with iPhone-ish softness. No new features — this is a **visual
system refresh** of screens that already exist (home / assets / review / report /
transactions + bottom nav).

## Approved decisions

1. **Reference fidelity:** loose ("느낌만 참고"). Match the *feel* of the attached
   mockup — rounded cards, colored icon chips, clear money hierarchy — but use
   lightweight vectors / icon chips, **not** heavy 3D illustrations.
2. **Dark mode:** included. Light + dark from the start via a token layer.
3. **Rollout:** foundation-first, then validate on **Home + bottom nav**, then
   expand to the other screens in a later plan.
4. **Font:** bundle **Pretendard** in `res/font/` and wire it into Typography.

### Deliberate reversal from prior direction

`docs/visual-refresh-progress.md` previously recorded an approved rule:
**"Keep 8dp card radius."** This spec **supersedes** that. New card radius is
18–20dp (rounder, per user request "둥글둥글"). This is intentional, not an
oversight.

## Architecture — design token layer (new)

Introduce a proper token layer under `ui/theme/` that components consume, instead
of the current scattered `Color(0xFF…)` literals. Component **public APIs stay
unchanged** so screen code barely changes and dark mode comes "for free."

Rejected alternatives:
- *Bump radius + font only, no token layer* — leaves magic numbers, painful dark
  mode, drifts again when other screens are restyled.
- *Material3 `dynamicColor`* — follows the OS wallpaper palette; breaks the fixed
  Toss brand look.

### Colors — `ui/theme/MoneyColors.kt`

Material3's color slots alone don't cover the app's needs (white card vs. gray
canvas, muted text, soft-accent fills, income/expense colors). Provide semantic
tokens via a `MoneyColors` holder exposed through a `CompositionLocal`, and also
map the relevant ones into the Material `colorScheme` so stock components (Button,
etc.) still theme correctly.

| Token | Light | Dark |
|---|---|---|
| `canvas` (screen bg) | `#F2F4F6` | `#17171C` |
| `surface` (card) | `#FFFFFF` | `#242429` |
| `divider` | `#F2F4F6` | `#2E2E35` |
| `ink` (primary text) | `#191F28` | `#F2F4F6` |
| `inkSub` (secondary text) | `#4E5968` | `#9DA5B4` |
| `muted` (hint text) | `#8B95A1` | `#8B95A1` |
| `primary` (Toss blue) | `#3182F6` | `#4593FC` |
| `positive` (income/saving) | `#00C471` | `#2AC769` |
| `negative` (expense) | `#F04452` | `#FF6A76` |
| soft-accent fill | accent @ 12% alpha | accent @ 22% alpha |

- Toss blue `#3182F6` replaces the current `#2F80ED`.
- The brand/category/review accent *logic* in `FinancePalette.kt`
  (`accountAccentForName`, `categoryAccentForName`, `reviewAccentForLabel`) is
  kept as-is; only the raw hex constants are tokenized and given dark variants.

### Typography — `ui/theme/Typography.kt`

Bundle Pretendard (`res/font/pretendard_*`), build a `FontFamily`, define a Compose
`Typography`:

| Role | Size / Weight | Use |
|---|---|---|
| display | 28 / SemiBold | big amounts (남은 돈) |
| headline | 22 / Bold | screen title (홈) |
| title | 17 / SemiBold | card titles |
| body | 15 / Regular | rows, values |
| label | 13 / Medium | captions/labels |
| caption | 12 / Regular | timestamps/hints |

- Amount texts use tabular figures (`FontFeatureSettings("tnum")`) so digits don't
  jitter — a visibility win.

### Shapes — `ui/theme/Shapes.kt`

`small = 12dp`, `medium = 18dp`, `large = 20dp`. Cards default to 18–20dp; icon
chips 14dp; pills fully rounded.

### Spacing — `ui/theme/Spacing.kt`

Scale `4 / 8 / 12 / 16 / 20 / 24`. Screen padding 20, card inner padding 18–20,
gap between cards 12.

### Elevation / shadow

Toss look = white card on gray canvas with a very soft shadow in light mode; in
dark mode use a hairline border instead of shadow. One shared soft-shadow preset.

## Components — restyle in place (API-stable)

Refactor these in `ui/components/MoneyVisuals.kt` to consume tokens + new radii;
**do not change their parameter signatures** so screens still compile:
`ScreenTitle`, `FinanceSectionCard`, `MetricTile`, `TransactionRow`,
`ReviewActionCard`, `CategoryBar`, `IconBadge`, `SourceAppBadge`.

New shared components:
- `MoneyBottomBar` — rounded bottom nav, selected tab in primary color (the
  mockup's 5-tab bar).
- `SoftShadowCard` — the common white/rounded/soft-shadow card shell so every
  surface is consistent.

### Icon chips

- **Flow steps** (수입/지출/저축 on the hero card): rounded-square 14dp chips.
- **List rows** (transactions, review): circular chips.
This mix is intentional (matches the mockup); it reads as "summary vs. list."

## Validation target — Home + bottom nav

After the token layer + components land, apply them to `HomeScreen.kt` and the
bottom bar in `AppRoot.kt`. Confirm against the mockup with light + dark
`@Preview`. Lock the look here **before** expanding to assets/review/report/
transactions (separate plan).

## Legacy / dead assets

Not deleted in this pass (they predate this work; Karpathy rule — don't remove
code you didn't write unless asked). Tracked for a later cleanup decision:
- Legacy composables: `MonthlyFlowCardOld`, `MoneyHeroCard`, `MoneyFlowHeroCard`,
  `IllustratedSummaryCard` (verify usage before removing).
- 3D-ish vector drawables: `illustration_wallet_coins`, `illustration_bank_mint`,
  `illustration_review_magnifier`, `illustration_flow_*`, `illustration_cash_flow`,
  `illustration_notification_flow`. The restyle uses icon chips instead; these can
  be dropped once no screen references them.

## Codex-requested UI work (tracked, sequenced after restyle)

From `docs/AI_COLLABORATION.md` → "Claude UI Notes". These are review-flow behavior
tweaks, separate from the visual restyle. Address after Home validation, and claim
`AppContainer.kt` / `ReviewScreen.kt` before touching wiring:
1. `ACCOUNT_UNMATCHED` cards ("계좌 확인") route to account edit, not the generic
   memo confirm flow.
2. Review actions call the atomic review use cases from `AppContainer` instead of
   `updateTransaction()` + `resolveReviewItem()` separately.
3. Account-transfer review UI calls the atomic account-transfer use case.
4. Keep visual/copy polish in `ReviewScreen.kt`; Codex keeps mapper/domain aligned.

## Constraints (CLAUDE.md)

- Work on `claude/ui-polish`. Edit only `ui/**` (+ `res/font/` for the font).
- Do **not** touch `data/ domain/ notification/ ui/model/**` (Codex-owned).
- `AppRoot.kt` is a shared boundary zone → add a claim line to the "Shared File
  Claims" log before editing the bottom bar.
- Bundling Pretendard uses `res/font/` only → **no `app/build.gradle.kts` change**,
  which avoids that claim zone.
- Never `git add .`; stage only files I own; verify `git status` before committing.

## Verification criteria

1. `:app:assembleDebug` passes → green build.
2. Home renders in both themes → two `@Preview` (light + dark) screenshots.
3. Screen-code diff stays small (component APIs unchanged) → `HomeScreen.kt` change
   is minimal.

## Out of scope

- Assets / review / report / transactions restyle (later plan).
- Any logic, DB, parser, mapper, or report-calculation change.
- Heavy 3D illustration assets.
- New features or navigation changes.
