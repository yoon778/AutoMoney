# AI Collaboration Workflow

This repository uses separate branches for Codex and Claude work.

## Stable Branch

- `main` is the stable integration branch.
- The user owns final review and merge into `main`.
- Do not work directly on `main` except for repository workflow/documentation setup approved by the user.

## Agent Roles

### Codex

- Work on branches prefixed with `codex/`.
- Primary responsibility: app logic, Android architecture, notification parsing, database, asset/account calculations, rules, tests, and build verification.
- Current Codex work branch: `codex/app-logic`.
- Before finishing, run relevant unit tests and `:app:assembleDebug` when code changes affect the Android app.

### Claude

- Work on branches prefixed with `claude/`.
- Primary responsibility: UI polish, UX copy, visual layout, screen composition, and presentation improvements.
- Current Claude work branch: `claude/ui-polish`.
- Avoid changing core transaction logic, database migrations, notification parsing, or report calculations unless explicitly assigned.

## Collaboration Rules

- Codex and Claude must not edit the same branch at the same time.
- Each agent pushes its own branch to GitHub.
- The user reviews changes in GitHub Desktop or GitHub and merges into `main`.
- After `main` changes, each agent should update from `main` before starting the next task.
- If a task touches both logic and UI, split it into two branches or agree on one owner before editing.

## Current Product Direction

- The app is an automated money management app for Android/Galaxy.
- It reads finance notifications, creates transactions, sends ambiguous transactions to review, and minimizes manual bookkeeping.
- The UI direction is clean and Toss-like, with simple visual assets used where they improve scanning.
- Functional correctness takes priority over visual polish until core flows are stable.
