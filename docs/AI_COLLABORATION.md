# AI Collaboration Workflow

This repository uses one linear `main` branch for Codex and Claude work.

Encoding note: this file is UTF-8. If Korean text looks garbled in Windows PowerShell, read it with `Get-Content -Encoding UTF8`.

## Single Branch

- `main` is the only working and remote branch.
- GitHub default branch must remain `main`.
- Do not create or switch to agent/feature branches, worktrees, or alternate clones.
- Only one agent is active at a time; handoffs are sequential.
- Start with `git switch main` and `git pull --ff-only origin main`.
- Commit small verified units directly to `main`, then push `main`.
- Never force-push, rewrite shared history, or bypass repository hooks with `--no-verify`.
- On a fresh clone, run `git config --local core.hooksPath .githooks` before work.

## Agent Roles

### Codex

- Primary responsibility: app logic, Android architecture, notification parsing, database, asset/account calculations, rules, tests, and build verification.
- Before finishing, run relevant unit tests and `:app:assembleDebug` when code changes affect the Android app.

### Claude

- Primary responsibility: UI polish, UX copy, visual layout, screen composition, and presentation improvements.
- Avoid changing core transaction logic, database migrations, notification parsing, or report calculations unless explicitly assigned.

## Collaboration Rules

- Codex and Claude must not edit concurrently.
- Before each turn, verify the current branch is exactly `main` and inspect `git status`.
- If the tree contains unrelated or previous-agent changes, stop and hand back; do not hide them in another branch.
- Stage exact owned paths only. `git add .` and `git add -A` remain forbidden.
- Finish and push one owner's layer before handing the clean `main` worktree to the next owner.
- If a task touches logic and UI, Codex commits/pushes the contract first; Claude then pulls `main` and consumes it.
- User review uses commit diffs on `main`; no merge workflow.

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
| `ui/theme`, `ui/components`, `ui/home`, `ui/report`, `ui/review`, `ui/settings`, `ui/assets`, `ui/edit`, `ui/transactions` | Screens & visuals | Claude |
| `ui/model/**` mapper logic (`CalendarMapper`, `MonthlySummaryMapper`, `ReviewItemMapper`, `SourceAppUi`) | Domain → UI data conversion | Codex (see boundary zones) |
| UI model data classes (e.g. `DashboardUiModels`) | Shared UI contract | Shared contract |
| `di/AppContainer.kt` | Dependency wiring | Shared |
| `ui/AppRoot.kt`, `MainActivity.kt` | Navigation / screen wiring | Shared (Claude-led) |
| `app/build.gradle.kts` | Library dependencies | Shared |

**Rule of thumb:** anything under `ui/` that renders pixels is Claude's; everything else, including the mapper logic that feeds the UI, is Codex's.

## Boundary Zones (the 10% that needs coordination)

These four spots are where the two agents can collide. Before editing any of them, announce it in the **Shared File Claims** log at the bottom of this file so the next agent does not touch it before handoff.

1. **UI model contract** — the UI model data classes (e.g. `DashboardUiModels`). Claude renders them; Codex's mappers produce them. Treat these classes as a **contract**: whoever needs to change a field must claim it first, then push one small `main` commit before handoff.
2. **`ui/model/**` mappers** — mapper logic is Codex-owned because it depends on the shape of domain models. Claude reads the output but does not edit mapper logic.
3. **`di/AppContainer.kt`** — Codex adds new use cases here; Claude may need a new dependency for a screen. Claim before editing.
4. **`ui/AppRoot.kt` / `MainActivity.kt`** and **`app/build.gradle.kts`** — navigation wiring and library additions. Different lines usually auto-merge, but claim first to be safe.

When a task genuinely needs both a contract change and UI work, Codex commits/pushes the contract to `main`, then Claude pulls `main` and builds the UI.

## Collaboration Mode (2026-07-13 main-only)

**평소: 원래 역할대로 순차 작업.** Claude = UI 층(`ui/**` 렌더링), Codex = 로직 층(data/domain/notification/mapper). 한 번에 한 agent만 `main`에서 작업하고 commit/push 후 다음 agent에게 인계한다.

**예외(비상 릴레이): 한쪽이 토큰 소진 등으로 멈췄을 때만.** 남은 쪽이 층 경계를 넘어 상대의 미완 작업을 이어받는다. 다시 양쪽이 살아나면 각자 원래 층으로 복귀한다.

릴레이가 가능하려면 여러 단계 작업은 항상 아래를 지킨다 (평소에도, 비상 대비용):

1. **계획서가 단일 진실**: 여러 단계 작업은 `docs/superpowers/plans/YYYY-MM-DD-<이름>.md`에 만든다. 헤더에 `Status:`(in-progress/complete), `Owner:`(현재 작업자), 태스크는 `- [ ]` 체크박스. 각 계획서 끝에 "이어받기 바통" 문구 포함.
2. **태스크 하나 끝날 때마다** 체크 `- [x]` + `main` commit/push. 이 커밋 이력이 비상 시 바통이 된다.
3. **비상 인계**: 멈춘 계획서를 이어받는 쪽은 `git switch main` → `git pull --ff-only origin main` → 첫 미체크 태스크부터. `Owner:`를 본인으로 바꿔 착수 선언. 커밋 안 된 반쪽은 `git status`로 확인.
4. 검증 규칙 동일: 태스크마다 관련 테스트 + `:app:assembleDebug`, TDD 우선, 경계 파일은 아래 Claims에 기록.
5. 완료 시 `Status: complete` + APP_REVIEW_FIX_LIST.md 갱신.

> 상태(2026-07-11): 예외 종료, 역할 정상 복귀. Claude가 비상 인계로 F1·F2 완료. 이후 F6·F1-PhaseB의 로직은 Codex, UI 마감은 Claude가 각자 담당.

## Shared File Claims

Append a line before you start editing a shared/boundary file; remove it in the completion commit pushed to `main`.

Format: `- [YYYY-MM-DD] <agent> claims <path> — <reason>`

<!-- active claims below -->
- [2026-07-21] Codex claims `di/AppContainer.kt` — wire automatic refund linking use case
- (해제됨 2026-07-21) Codex claims `ui/model/MonthlySummaryMapper.kt` — cleanup item 8 unused import removal (`f1366de`)
- (해제됨 2026-07-21) Claude claims `app/build.gradle.kts` — 미사용 의존성 5개 제거
- (해제됨 2026-07-13) Claude claims `ui/AppRoot.kt` — 예산 개편 T4: 탭 라벨 자산→예산
- (해제됨 2026-07-13) Claude claims `ui/AppRoot.kt` — T6 알림 수집 앱 설정 UI: SettingsScreen에 notificationSourceSettingsService 전달
- (해제됨 2026-07-13) Claude claims `MainActivity.kt` — T6: AppRoot에 notificationSourceSettingsService 전달
- (해제됨 2026-07-12) Claude claims `ui/AppRoot.kt` — 카테고리 예산 Task 3: AssetsScreen에 userCategoryRepository 전달
- (해제됨 2026-07-12) Claude claims `ui/AppRoot.kt` — AssetsScreen에 moneyRepository 전달 1줄 (Codex 토큰 소진 중, 같은 커밋으로 main 반영)
- (해제됨 2026-07-11) Codex F6 정산 회수 연결 유즈케이스 노출: `di/AppContainer.kt`
- (해제됨 2026-07-11) Codex F6 정산 내 몫 집계 적용: `ui/home/HomeScreen.kt`
- (해제됨 2026-07-11) F1/F2 완료. 역할 정상 복귀: F6·F1-PhaseB 로직은 Codex, UI 마감은 Claude.

## Claude UI Notes

Codex found UI-owned review flow issues while fixing app logic. Claude should apply these after the relevant Codex `main` commit is pushed and pulled:

1. Review actions should use the atomic review use cases exposed from `AppContainer` instead of calling `updateTransaction()` and `resolveReviewItem()` separately.
2. Account-transfer review UI should call the atomic account-transfer use case so account updates, paired transaction resolution, and review resolution happen in one repository transaction.
3. Keep visual/copy polish in `ui/review/ReviewScreen.kt`; Codex will keep mapper/domain/repository behavior aligned.

## Codex Cleanup Notes (2026-07-21)

Claude가 저장소 전체를 훑어 마이그레이션·죽은 코드를 점검했다. UI 층과 `app/build.gradle.kts`는
Claude가 이미 정리해 `main`에 push 했다. 아래는 로직/DB/테스트 층이라 Codex가 판단해야 하는 항목이다.
지우기 전에 "미완성 기능"인지 "잔재"인지 먼저 구분할 것.

### 남은 마이그레이션 판단

1. `app/schemas/`의 `1.json`과 `3.json`은 Git 전체 이력에도 존재하지 않는다. 저장소 최초 DB 코드는
   이미 version 2이고 `exportSchema = false`였으며, 현재 `2.json`도 나중에 수동 추가된 파일이다.
   따라서 원본 identity hash를 포함한 v1/v3 schema의 정확한 복구는 불가능하다. migration SQL을 역산해
   파일을 조작하는 방식은 검증 자료를 새로 만들어 내는 것이므로 적용하지 않는다. `MIGRATION_1_2`의
   기기 migration test 부재를 알려진 제한으로 유지하고, 보존된 v2 이후 schema만 신뢰한다.
2. `DatabaseIntegritySchemaTest`의 `contains()` 검사는 등록 누락을 잡는 smoke test일 뿐 실제 migration을
   실행하지 못한다. 개선안은 migration 목록을 단일 상수로 만들고 `AppContainer`가 이를 사용하게 한 뒤,
   emulator CI에서 `AppDatabaseMigrationTest`를 필수 실행하는 것이다. 공유 DI 계약과 CI를 함께 바꾸는
   별도 작업이므로 이번 cleanup에서는 제안만 남긴다.

### 보존하는 legacy 값

3. `ReviewReason.ACCOUNT_UNMATCHED`, `ACCOUNT_AMBIGUOUS`, `BALANCE_MISMATCH`는 새 생성 경로를 만들지 않는다.
   거래-계좌 결합은 `deb33b7`과 `79afe2f`에서 의도적으로 폐기됐다. 다만 reason은 Room에 TEXT로 저장돼
   과거 행이 존재할 수 있으므로 enum과 mapper 처리는 읽기 호환성 용도로 유지한다.

### Claude 후속 항목

9. 기본 카테고리 목록이 세 군데 중복돼 있다: `ui/settings/CategoryPreferenceStore.kt`의
   `defaultEnabledExpenseCategories`, `ui/components/TransactionEditCategoryOptions.kt`의
   `transactionEditExpenseCategoryOptions`, `ui/transactions/ManualCategoryOptions.kt`의
   `manualExpenseCategoryOptions`. 멤버가 완전히 같아서 카테고리를 하나 추가할 때마다 3곳을 고쳐야 한다.
   수입 쪽도 마찬가지다. 단일 출처로 합치는 게 좋다.
   **세 파일 모두 `ui/` 소유라 이 항목은 Claude가 처리한다. Codex는 건드리지 말 것.**
## Current Product Direction

- The app is an automated money management app for Android/Galaxy.
- It reads finance notifications, creates transactions, sends ambiguous transactions to review, and minimizes manual bookkeeping.
- The UI direction is clean and Toss-like, with simple visual assets used where they improve scanning.
- Functional correctness takes priority over visual polish until core flows are stable.
