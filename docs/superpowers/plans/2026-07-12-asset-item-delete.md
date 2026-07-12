# 자산: 고정지출·월계획 항목 삭제 Implementation Plan

Status: complete (T4 후순위 제외)
Owner: Claude (Codex 토큰 소진으로 로직 층까지 Claude가 이어받음 — CLAUDE.md Collaboration Mode 예외)
날짜: 2026-07-12

**Goal:** 고정지출·월계획 항목은 추가만 되고 수정/삭제가 불가. 최소로 **삭제**부터 지원.
수정(edit)은 삭제+재추가로 대체 가능하므로 후순위(T4).

## 현재 코드 사실
- `data/local/dao/AssetDao.kt` — insert/observe만 있음. DELETE 쿼리 없음.
- `data/repository/AssetRepository.kt` — save/observe만. delete 없음.
- `data/repository/RoomAssetRepository.kt` — 위 인터페이스 구현.
- `ui/assets/AssetsScreen.kt` — `AssetRow`에 `actionLabel: String?`, `onAction: (() -> Unit)?` 파라미터 이미 존재(계좌 패널에서 사용 중) → 삭제 버튼으로 재사용.
- 고정지출 id: `FixedExpensePlan.id`, 월계획 id: `MonthlyPlanItem.id` (둘 다 Long, Room PK).

## Tasks
- [x] **T1: DAO 삭제 쿼리** — `AssetDao`에 `@Query("DELETE FROM fixed_expenses WHERE id = :id")` / `@Query("DELETE FROM monthly_plan_items WHERE id = :id")` 추가. 마이그레이션 불필요(스키마 변화 없음).
- [x] **T2: Repository 삭제 경로** — `AssetRepository`에 `deleteFixedExpense(id: Long)` / `deleteMonthlyPlanItem(id: Long)` 추가, `RoomAssetRepository` 구현.
- [x] **T3: UI 삭제 버튼** — `AssetsScreen` 고정지출/월계획 목록의 `AssetRow`에 `actionLabel="삭제"` + `onAction` 연결. 미리보기(repository null)면 안내 메시지. 삭제 후 스낵/칩 메시지.
- [ ] **T4 (후순위): 항목 수정** — 필요 시 별도 계획. 지금은 삭제+재추가로 대체.
- [x] **검증:** `:app:compileDebugKotlin` + 에뮬레이터에서 추가→삭제 동작 확인.

## 이어받기 (릴레이 바통)
어느 에이전트든(Claude/Codex) 토큰 소진 시:
> AutoMoney 이어서 작업. `git pull` 후 이 계획서(`docs/superpowers/plans/2026-07-12-asset-item-delete.md`)의 첫 미체크 태스크부터. `Owner:`를 본인으로 바꿔 커밋. T1→T2→T3 순서 (T3은 T2 시그니처 의존).
현재 미커밋 메모: 없음 (커밋마다 이 줄 갱신).
