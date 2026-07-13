# CLAUDE.md — Instructions for Claude

You (Claude) share this repository with Codex. Read this file before doing any work.
The full workflow is in `docs/AI_COLLABORATION.md`; this file is the short, must-follow version.

## Your Role

- (2026-07-13) 평소엔 원래 역할대로(Claude=UI, Codex=로직) 맡되 **한 번에 한 agent만 main에서 순차 작업**. 한쪽이 멈췄을 때만 계획서(`docs/superpowers/plans/`) 체크박스를 바통 삼아 상대 층까지 이어받는다. 상세: `docs/AI_COLLABORATION.md`의 "Collaboration Mode".
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

- (2026-07-12) main-only 전환. Claude와 Codex는 한 번에 한쪽만 활성화해 순차 작업하므로
  에이전트별 브랜치(`claude/*`, `codex/*`)를 두지 않고 **`main`에서 직접 작업**한다.
- 다른 브랜치 생성·전환·push 및 별도 worktree/clone 사용 금지.
- 작은 단위로 자주 커밋한다 (되돌릴 때 `git revert`, 리뷰는 커밋 diff로).
- `git switch main` + `git pull --ff-only origin main` 후 시작하고, 작업이 끝나면 main을 push 한다.
- commit 전 `git branch --show-current`가 정확히 `main`인지 확인.
- `git config --get core.hooksPath`가 `.githooks`인지 확인하고 아니면 local config에 설정.
- 저장소는 `C:\Users\cys04\Desktop\AutoMoney` 한 곳만 사용 (OneDrive 사본 금지).
- 저장소 hook이 main 외 commit 및 비삭제 push를 거부함.

## CRITICAL: Do Not Sweep Up Files You Don't Own

Claude와 Codex는 같은 main worktree에서 순차 작업한다. dirty tree에는 이전 agent의
미완료 작업이 있을 수 있으므로 다음을 지킨다:

- **Never run `git add .` or `git add -A`.** Stage only the specific paths you changed,
  e.g. `git add app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt`.
- Run `git status` before committing and confirm every staged file is one you own.
- If you see modified files under `data/`, `domain/`, `notification/`, or `ui/model/`
  that you did not change, leave them unstaged — they are Codex's in-progress work.
- 관련 없는 변경이 있으면 중단하고 소유자에게 인계. branch/worktree로 우회 금지.

## Boundary Zones — Claim Before Editing

Four spots are shared and can collide. Before editing any of them, add a line to the
**Shared File Claims** log at the bottom of `docs/AI_COLLABORATION.md`, then remove the
line in the completion commit pushed to `main`:

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
