# AI Collaboration Workflow

This repository uses separate branches for Codex and Claude work.

Encoding note: this file is UTF-8. If Korean text looks garbled in Windows PowerShell, read it with `Get-Content -Encoding UTF8`.

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

## Split Axis: by Layer (not by feature)

Work is divided **horizontally by layer**, not vertically by feature zone.

- Codex owns the logic layers across every feature; Claude owns the UI layer across every feature.
- This keeps each agent specialized (Codex = logic, Claude = UI) and keeps the split consistent.
- The cost of a layer split is that every feature has a seam where logic meets UI. Those seams are the "boundary zones" below and are the only places that need coordination.

## File Ownership Map

Package root: `app/src/main/java/com/choiyoonseo/automoney`

| Path | Purpose | Owner |
| --- | --- | --- |
| `data/**` (Room DB, DAO, entity, repository) | Persistence | Codex |
| `domain/**` (parser, rules, report, review, use cases) | Business logic | Codex |
| `notification/**` | Notification listener / ingestion | Codex |
| `export/**` | CSV export | Codex |
| `ui/theme`, `ui/components`, `ui/home`, `ui/report`, `ui/review`, `ui/settings`, `ui/assets`, `ui/edit`, `ui/transactions` | Screens & visuals | Claude |
| `ui/model/**` mapper logic (`CalendarMapper`, `MonthlySummaryMapper`, `ReviewItemMapper`, `SourceAppUi`) | Domain → UI data conversion | Codex (see boundary zones) |
| UI model data classes (e.g. `DashboardUiModels`) | Shared UI contract | Shared contract |
| `di/AppContainer.kt` | Dependency wiring | Shared |
| `ui/AppRoot.kt`, `MainActivity.kt` | Navigation / screen wiring | Shared (Claude-led) |
| `app/build.gradle.kts` | Library dependencies | Shared |

**Rule of thumb:** anything under `ui/` that renders pixels is Claude's; everything else, including the mapper logic that feeds the UI, is Codex's.

## Boundary Zones (the 10% that needs coordination)

These four spots are where the two agents can collide. Before editing any of them, announce it in the **Shared File Claims** log at the bottom of this file (and in the PR description) so the other agent does not touch it in parallel.

1. **UI model contract** — the UI model data classes (e.g. `DashboardUiModels`). Claude renders them; Codex's mappers produce them. Treat these classes as a **contract**: whoever needs to change a field must claim it first, and the change should be a single small commit that both branches can rebase onto.
2. **`ui/model/**` mappers** — mapper logic is Codex-owned because it depends on the shape of domain models. Claude reads the output but does not edit mapper logic.
3. **`di/AppContainer.kt`** — Codex adds new use cases here; Claude may need a new dependency for a screen. Claim before editing.
4. **`ui/AppRoot.kt` / `MainActivity.kt`** and **`app/build.gradle.kts`** — navigation wiring and library additions. Different lines usually auto-merge, but claim first to be safe.

When a task genuinely needs both a contract change and UI work, do the contract change first (Codex), merge it to `main`, then let Claude build UI on top.

## Collaboration Mode (2026-07-11)

**평소: 원래 역할대로.** Claude = UI 층(`ui/**` 렌더링), Codex = 로직 층(data/domain/notification/mapper). 각자 자기 층에서 병렬로 일하며 서로의 파일을 건드리지 않는다. 이게 기본이고 가장 효율적이다.

**예외(비상 릴레이): 한쪽이 토큰 소진 등으로 멈췄을 때만.** 남은 쪽이 층 경계를 넘어 상대의 미완 작업을 이어받는다. 다시 양쪽이 살아나면 각자 원래 층으로 복귀한다.

릴레이가 가능하려면 여러 단계 작업은 항상 아래를 지킨다 (평소에도, 비상 대비용):

1. **계획서가 단일 진실**: 여러 단계 작업은 `docs/superpowers/plans/YYYY-MM-DD-<이름>.md`에 만든다. 헤더에 `Status:`(in-progress/complete), `Owner:`(현재 작업자), 태스크는 `- [ ]` 체크박스. 각 계획서 끝에 "이어받기 바통" 문구 포함.
2. **태스크 하나 끝날 때마다** 체크 `- [x]` + 즉시 main까지 push. 이 커밋 이력이 비상 시 바통이 된다.
3. **비상 인계**: 멈춘 계획서를 이어받는 쪽은 `git fetch` → 첫 미체크 태스크부터. `Owner:`를 본인으로 바꿔 착수 선언. 커밋 안 된 반쪽은 `git status`로 확인.
4. 검증 규칙 동일: 태스크마다 관련 테스트 + `:app:assembleDebug`, TDD 우선, 경계 파일은 아래 Claims에 기록.
5. 완료 시 `Status: complete` + APP_REVIEW_FIX_LIST.md 갱신.

> 상태(2026-07-11): 예외 종료, 역할 정상 복귀. Claude가 비상 인계로 F1·F2 완료. 이후 F6·F1-PhaseB의 로직은 Codex, UI 마감은 Claude가 각자 담당.

## Shared File Claims

Append a line before you start editing a shared/boundary file; remove it after the change is merged to `main`.

Format: `- [YYYY-MM-DD] <agent> claims <path> — <reason>`

<!-- active claims below -->
- (해제됨 2026-07-11) Codex F6 정산 내 몫 집계 적용: `ui/home/HomeScreen.kt`
- (해제됨 2026-07-11) F1/F2 완료. 역할 정상 복귀: F6·F1-PhaseB 로직은 Codex, UI 마감은 Claude.

## Claude UI Notes

Codex found UI-owned review flow issues while fixing app logic. Claude should apply these after the Codex logic branch is merged or rebased:

1. `ACCOUNT_UNMATCHED` review cards should not use the generic memo confirm flow. The primary action text is "계좌 확인", so it should open an account selection/edit flow or route to the transaction edit dialog with account focus.
2. Review actions should use the atomic review use cases exposed from `AppContainer` instead of calling `updateTransaction()` and `resolveReviewItem()` separately.
3. Account-transfer review UI should call the atomic account-transfer use case so account updates, paired transaction resolution, and review resolution happen in one repository transaction.
4. Keep visual/copy polish in `ui/review/ReviewScreen.kt`; Codex will keep mapper/domain/repository behavior aligned.

## Current Product Direction

- The app is an automated money management app for Android/Galaxy.
- It reads finance notifications, creates transactions, sends ambiguous transactions to review, and minimizes manual bookkeeping.
- The UI direction is clean and Toss-like, with simple visual assets used where they improve scanning.
- Functional correctness takes priority over visual polish until core flows are stable.
