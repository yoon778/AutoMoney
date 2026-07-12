# 자산 화면: 고정지출·월계획 개선 Implementation Plan

Status: complete
Owner: Codex
역할: Codex=로직(계좌 FK 링크, 오버뷰 계산/의미), Claude=UI(출금일 다이얼, 상단 카드 카피/진행바, 피커 연결).

**Goal:** 자산 화면의 "고정지출"과 "월계획"을 사용자가 이해·신뢰할 수 있게 고친다.
사용자 보고 4가지:
1. 고정지출 출금일을 다이얼/숫자 선택으로 (지금은 자유 텍스트).
2. 고정지출 출금계좌를 "내가 설정한 계좌"와 실제로 연결.
3. 월계획을 추가해도 상단 "생활예산"이 반영 안 되는 것처럼 보임.
4. 상단 "월 고정지출", "생활예산" 카드가 뭘 보여주는지 불명확.

## 현재 코드 사실 (2026-07-12 기준)
- `domain/assets/AssetModels.kt`
  - `FixedExpensePlan(name, amountWon, withdrawalDay: Int, accountName: String, active)` — 계좌를 **이름 문자열로만** 참조. `accountId` 없음.
  - `fixedExpenseWithdrawalDayOptions = 1..31` 이미 존재.
  - `buildAssetOverview(...)` → `totalFixedExpenseWon`(active 합), `totalBudgetWon`(BUDGET 항목 합), `totalIncomeWon`, `plannedRemainingWon`. **진행바 비율(ratio)은 도메인에 없음.**
- `data/local/entity/Entities.kt` `FixedExpenseEntity` — 동일하게 `accountName: String`, `accountId` 없음.
- `ui/assets/AssetsScreen.kt`
  - line 144~153: 상단 MetricTile 2개. `progress = 0.42f`, `progress = 0.55f` **하드코딩** → 값은 갱신돼도 막대가 안 움직임 (issue 3의 체감 원인). helper "N건" / "월계획 기준".
  - line 620: 출금일 = 숫자 `OutlinedTextField` (issue 1).
  - line 624 `AssetAccountNamePicker`: 계좌 피커 이미 있음. 단 선택 결과를 `accountName`(문자열)로만 저장 (issue 2).
  - overview는 line 110 `remember(accounts, fixedExpenses, monthlyPlans)`로 재계산 → **월계획 값 자체는 반영됨**. issue 3은 실제 로직 버그가 아니라 하드코딩 진행바 + 모호 카피 문제일 가능성 큼. Codex가 flow 반응성만 한 번 확인.

---

## 분리 원칙
- **로직(Codex):** 데이터 모델/DB 마이그레이션, 계좌 FK, 오버뷰 계산과 "카드가 의미하는 값" 정의.
- **UI(Claude):** 입력 위젯(다이얼), 상단 카드 라벨/헬퍼/진행바 표현, 피커를 Codex 계약에 연결.
- 경계: `AssetOverview` 데이터클래스와 `buildAssetOverview`는 도메인(Codex). Claude가 진행바용 새 필드가 필요하면 여기 계약으로 요청 (직접 수정 금지). `docs/AI_COLLABORATION.md` Shared File Claims에 등록.

---

## Tasks

### Codex (로직)
- [x] **C1: 고정지출 계좌 FK 링크** — `FixedExpensePlan`/`FixedExpenseEntity`에 `accountId: Long?` 추가, `accountName`은 표시용 스냅샷으로 유지(계좌 삭제/이름변경 후에도 과거 표시 보존, 기존 custom-category 패턴 동일). Room 마이그레이션 +1, `RoomAssetRepository` 매핑, 저장 시 선택 계좌의 id/name 함께 기록. 저장소 테스트.
  - **Claude 계약(C1):** UI는 피커에서 고른 `AssetAccount`의 `id`+`name`을 `FixedExpensePlan(accountId=, accountName=)`로 넘김. 목록 표시는 `accountName` 스냅샷 사용.
- [x] **C2: 오버뷰 의미·진행바 계약** — 상단 카드가 "무엇 대비 얼마"인지 정의하고 도메인이 비율까지 계산. `AssetOverview`에 필드 추가:
  - `fixedExpenseRatio: Float` = `totalFixedExpenseWon / totalIncomeWon` (수입 0이면 0, 0..1 clamp).
  - `budgetRatio: Float` = `totalBudgetWon / totalIncomeWon` (동일 규칙).
  - 의미 확정: "월 고정지출"=이번 달 자동 출금 합(수입 대비 %), "생활예산"=월계획의 예산 항목 합(수입 대비 %). 이 정의를 이 파일 하단 "카드 의미"에 반영.
  - **Claude 계약(C2):** MetricTile `progress`에 하드코딩 대신 `overview.fixedExpenseRatio` / `overview.budgetRatio` 사용.
- [x] **C3: 반영 버그 확인** — `observeMonthlyPlanItems()` flow가 저장 후 실제 재emit되는지 Room 저장소 테스트로 확인됨. 로직 수정 불필요.

### Claude (UI) — Codex 계약 나온 뒤
- [ ] **U1: 출금일 다이얼/숫자 선택** — line 620 자유 텍스트를 1~31 선택 위젯으로 교체(`fixedExpenseWithdrawalDayOptions` 사용). 드롭다운 또는 wheel 형태, 기존 컴포넌트 재사용 우선. 저장 로직/검증은 그대로.
- [ ] **U2: 출금계좌 피커 연결** — 이미 있는 `AssetAccountNamePicker`가 이름만 넘기던 것을 C1 계약대로 `accountId`+`accountName` 넘기게. 계좌 없을 때 fallback 텍스트 입력은 `accountId=null`.
- [ ] **U3: 상단 카드 명확화** — MetricTile 라벨/헬퍼를 사용자가 이해하게 수정하고 진행바를 C2 비율에 연결.
  - 진행바 `progress = overview.fixedExpenseRatio` / `overview.budgetRatio`.
  - 헬퍼를 "무엇인지" 설명으로: 예) 월 고정지출 → "매월 자동 출금 · 수입의 XX%", 생활예산 → "월계획 예산 합 · 수입의 XX%". (문구는 U 단계에서 확정)
- [ ] **U4: 월계획 패널 카피** — "월계획" 서브타이틀에 상단 생활예산과의 관계 한 줄 추가(예산 항목이 생활예산으로 합산된다는 안내).

---

## 카드 의미 (C2 확정)
- 월 고정지출: 이번 달 활성 자동 출금 합, 월 수입 대비 비율
- 생활예산: 월계획 예산 항목 합, 월 수입 대비 비율

## 순서/의존성
1. Codex C1·C2 계약 확정 → main 반영.
2. Claude U1(독립, 계약 무관 먼저 가능) / U2·U3(C1·C2 이후).
3. 빌드 `:app:assembleDebug` + 자산 화면 스크린샷.

---
## 이어받기 (릴레이 바통)
한쪽이 멈추면 다른 세션에 전달:
> AutoMoney 이어서 작업. `docs/AI_COLLABORATION.md`의 "Collaboration Mode"를 읽고, `git fetch` 후 이 계획서의 첫 미체크 태스크부터. `Owner:`를 본인으로 바꿔 커밋.
현재 미커밋 메모: 없음. issue 3(생활예산 반영)은 로직 버그 아닌 하드코딩 진행바(0.55f)가 유력 — C3에서 확인, U3에서 진행바 실제 연결.
