# 자산: 생활예산 소진율 표시 Implementation Plan

Status: complete
Owner: Claude (Codex 토큰 소진 — 로직 층 포함 Claude 진행)
날짜: 2026-07-12

**Goal:** 상단 "생활예산" 카드가 수입 대비 %만 보여줌. 예산 기능의 본래 목적인
**이번 달 실지출 대비 소진율**을 보여준다.

## 현재 코드 사실
- 이번 달 실지출 계산은 이미 존재: `moneyRepository.observeTransactionsForMonth(YearMonth)` +
  `domain/report/TransactionReportRules.kt`의 `countsAsActualExpense()` / `effectiveExpenseWon()`
  (HomeScreen이 동일 패턴 사용).
- `AssetsScreen`은 `assetRepository`만 받음 — `moneyRepository` 미전달 (AppRoot는 보유).
- `buildAssetOverview(accounts, fixedExpenses, monthlyPlanItems)` → `budgetRatio` = 예산/수입.
- `ratioOf(amount, total)` 도메인 헬퍼 이미 있음 (0 나눗셈/clamp 처리).

## 설계 (최소 diff)
- `AssetOverview`에 `spentThisMonthWon: Long`, `budgetUsedRatio: Float`(= 실지출/예산) 추가.
- `buildAssetOverview`에 `spentThisMonthWon: Long = 0` 파라미터 추가 — 기본값 0이라 기존 호출부/테스트 무변경 컴파일.
- UI: AppRoot가 `moneyRepository`를 AssetsScreen에 전달, AssetsScreen이 이번 달 거래 collect 후
  실지출 합계를 `buildAssetOverview`에 주입.
- 생활예산 카드: progress = `budgetUsedRatio`, helper = "이번 달 N원 씀 · M% 사용"
  (예산 0이면 기존 "수입 미등록"류 안내 유지).

## Tasks
- [x] **T1: 도메인** — `AssetOverview` 필드 2개 + `buildAssetOverview` 파라미터(기본 0). `AssetOverviewCalculatorTest`에 소진율 케이스 1개 추가.
- [x] **T2: 배선** — AppRoot → AssetsScreen에 `moneyRepository` 전달. AssetsScreen에서 이번 달 거래 collect, 실지출 합계 계산(HomeScreen 패턴 재사용).
- [x] **T3: 카드 표시** — 생활예산 카드 progress/helper를 소진율로 교체.
- [x] **검증** — `:app:compileDebugKotlin` + 도메인 테스트 + 에뮬레이터(예산 추가→지출 없음 0%→카드 문구 확인).

## 이어받기 (릴레이 바통)
어느 에이전트든 토큰 소진 시:
> AutoMoney 이어서 작업. `git pull` 후 이 계획서(`docs/superpowers/plans/2026-07-12-budget-usage-ratio.md`)의 첫 미체크 태스크부터. `Owner:` 본인으로 변경 후 커밋. T1→T2→T3 순.
현재 미커밋 메모: 없음.
