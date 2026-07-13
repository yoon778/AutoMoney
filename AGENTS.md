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

- **`main` only.** Do not create, switch to, or push `codex/*`, `claude/*`, or any other branch.
- Do not create a separate worktree or clone for agent work.
- Only one agent works at a time. Codex and Claude hand off sequentially on `main`.
- Before editing: `git switch main`, then `git pull --ff-only origin main`.
- Ensure `git config --get core.hooksPath` is `.githooks`; otherwise set it locally.
- Before committing, verify `git branch --show-current` returns exactly `main`.
- Commit small units directly to `main`; push `main` after verification.
- Repository hooks reject commits and non-deletion pushes outside `main`.

## CRITICAL: Do Not Sweep Up Files You Don't Own

Codex and Claude work sequentially in the same `main` worktree. A dirty tree may contain
the previous agent's unfinished work. To prevent mixing changes:

- **Never run `git add .` or `git add -A`.** Stage only the specific paths you changed,
  e.g. `git add app/src/main/java/com/choiyoonseo/automoney/domain/...`.
- Run `git status` before committing and confirm every staged file is one you own.
- If you see modified files under `ui/` (screens/visuals) that you did not change, leave
  them unstaged — they are Claude's in-progress work.
- If unrelated changes exist, stop and hand them back to their owner. Do not create a branch
  or worktree to bypass the dirty state.

## Boundary Zones — Claim Before Editing

Four spots are shared and can collide. Before editing any of them, add a line to the
**Shared File Claims** log at the bottom of `docs/AI_COLLABORATION.md`, then remove the
line in the completion commit pushed to `main`:

1. **UI model contract** — UI model data classes (e.g. `DashboardUiModels`). Treat as a
   contract: change fields in a single small commit, push `main`, then hand off to Claude.
2. **`ui/model/**` mappers** — yours, but Claude reads their output, so announce breaking changes.
3. **`di/AppContainer.kt`** — dependency wiring shared with Claude.
4. **`ui/AppRoot.kt` / `MainActivity.kt`** and **`app/build.gradle.kts`** — navigation and
   library additions.

Claim format: `- [YYYY-MM-DD] Codex claims <path> — <reason>`

## Before You Finish

- When code affects the Android app, run the relevant unit tests and `:app:assembleDebug`.
- Keep functional correctness ahead of visual polish until core flows are stable.
