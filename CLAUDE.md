# CLAUDE.md — Instructions for Claude

You (Claude) share this repository with Codex. Read this file before doing any work.
The full workflow is in `docs/AI_COLLABORATION.md`; this file is the short, must-follow version.

## Your Role

- (2026-07-11 변경) Codex와 **계획서 릴레이**로 협업한다: `docs/superpowers/plans/`의 체크박스 계획서가 단일 진실이며, 누구든 어떤 층(logic/UI)이든 이어서 작업할 수 있다. 상세 규칙은 `docs/AI_COLLABORATION.md`의 "Shared Plan Relay" 절.
- 기본 선호: You are the **UI** agent. Codex is the **logic / feature** agent.
- You own: UI screens and visuals under `ui/` (`ui/theme`, `ui/components`, `ui/home`,
  `ui/report`, `ui/review`, `ui/settings`, `ui/assets`, `ui/edit`, `ui/transactions`),
  UX copy, visual layout, and screen composition.
- You do **not** edit app logic, database/migrations, notification parsing, report
  calculations, or `ui/model/**` mapper logic. Those are Codex's.
- You render UI model data classes (e.g. `DashboardUiModels`) but do not change their
  fields without claiming them first (see Boundary Zones).
- Detailed ownership is in the "File Ownership Map" section of `docs/AI_COLLABORATION.md`.

## Branch Rules

- Work only on branches prefixed `claude/`. Current work branch: `claude/ui-polish`.
- Never commit directly on `codex/*` branches.
- Never commit directly on `main` except for user-approved workflow/documentation setup.
- Push your own branch. The user reviews and merges into `main`.
- Before starting a new task, update your branch from `main`.

## CRITICAL: Do Not Sweep Up Files You Don't Own

Claude and Codex may have the repository open at the same time. If you stage everything,
you can accidentally commit Codex's half-finished edits into your commit. This has already
happened once. To prevent it:

- **Never run `git add .` or `git add -A`.** Stage only the specific paths you changed,
  e.g. `git add app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt`.
- Run `git status` before committing and confirm every staged file is one you own.
- If you see modified files under `data/`, `domain/`, `notification/`, or `ui/model/`
  that you did not change, leave them unstaged — they are Codex's in-progress work.
- Prefer working in a **separate git worktree or clone** so you never share a working
  directory and branch with Codex at the same time.

## Boundary Zones — Claim Before Editing

Four spots are shared and can collide. Before editing any of them, add a line to the
**Shared File Claims** log at the bottom of `docs/AI_COLLABORATION.md` (and mention it in
your PR), then remove the line after it merges to `main`:

1. **UI model contract** — UI model data classes (e.g. `DashboardUiModels`). Codex's
   mappers produce them; you render them. If you need a new field, claim it and ask Codex
   to change the mapper — do not edit mapper logic yourself.
2. **`ui/model/**` mappers** — Codex-owned. Read their output; do not edit them.
3. **`di/AppContainer.kt`** — dependency wiring shared with Codex.
4. **`ui/AppRoot.kt` / `MainActivity.kt`** and **`app/build.gradle.kts`** — navigation and
   library additions.

Claim format: `- [YYYY-MM-DD] Claude claims <path> — <reason>`

## Before You Finish

- When UI changes affect the Android app, build to verify (`:app:assembleDebug`) and, where
  practical, capture a screenshot of the changed screen.
- The UI direction is clean and Toss-like; keep visuals simple and scannable.
- Functional correctness takes priority over visual polish until core flows are stable.
