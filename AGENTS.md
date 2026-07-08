# AGENTS.md — Instructions for Codex

You (Codex) share this repository with Claude. Read this file before doing any work.
The full workflow is in `docs/AI_COLLABORATION.md`; this file is the short, must-follow version.

## Your Role

- You are the **logic / feature** agent. Claude is the **UI** agent.
- You own: app logic, Android architecture, notification parsing, database (Room),
  asset/account calculations, rules, `ui/model/**` mappers, tests, and build verification.
- You do **not** edit UI screens/visuals under `ui/` (except the `ui/model/**` mapper logic).
- Detailed ownership is in the "File Ownership Map" section of `docs/AI_COLLABORATION.md`.

## Branch Rules

- Work only on branches prefixed `codex/`. Current work branch: `codex/app-logic`.
- Never commit directly on `claude/*` branches.
- Never commit directly on `main` except for user-approved workflow/documentation setup.
- Push your own branch. The user reviews and merges into `main`.
- Before starting a new task, update your branch from `main`.

## CRITICAL: Do Not Sweep Up Files You Don't Own

Claude and Codex may have the repository open at the same time. If you stage everything,
you can accidentally commit Claude's half-finished edits into your commit. This has already
happened once. To prevent it:

- **Never run `git add .` or `git add -A`.** Stage only the specific paths you changed,
  e.g. `git add app/src/main/java/com/choiyoonseo/automoney/domain/...`.
- Run `git status` before committing and confirm every staged file is one you own.
- If you see modified files under `ui/` (screens/visuals) that you did not change, leave
  them unstaged — they are Claude's in-progress work.
- Prefer working in a **separate git worktree or clone** so you never share a working
  directory and branch with Claude at the same time.

## Boundary Zones — Claim Before Editing

Four spots are shared and can collide. Before editing any of them, add a line to the
**Shared File Claims** log at the bottom of `docs/AI_COLLABORATION.md` (and mention it in
your PR), then remove the line after it merges to `main`:

1. **UI model contract** — UI model data classes (e.g. `DashboardUiModels`). Treat as a
   contract: change fields in a single small commit so Claude can rebase onto it.
2. **`ui/model/**` mappers** — yours, but Claude reads their output, so announce breaking changes.
3. **`di/AppContainer.kt`** — dependency wiring shared with Claude.
4. **`ui/AppRoot.kt` / `MainActivity.kt`** and **`app/build.gradle.kts`** — navigation and
   library additions.

Claim format: `- [YYYY-MM-DD] Codex claims <path> — <reason>`

## Before You Finish

- When code affects the Android app, run the relevant unit tests and `:app:assembleDebug`.
- Keep functional correctness ahead of visual polish until core flows are stable.
