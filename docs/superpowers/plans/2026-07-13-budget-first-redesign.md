# 예산 중심 개편(라이트 짤랑) Implementation Plan

Status: in-progress
Owner: Codex 로직(T1·T2) + Claude UI(T3·T4·T5) + 통합(T6)
Branch: `main` only

> **For agentic workers:** 태스크별 TDD, 작은 경로별 커밋, `git add .` / `git add -A` 금지

## Goal

계좌 기능을 화면에서 완전히 제거하고, 앱을 "카테고리별 월 예산에서 차감하는 자동 가계부"로 재편한다.
계좌 잔액 관리는 은행 앱의 몫이라는 사용자 결정.

## 확정 결정 (2026-07-13 grilling)

1. 모드 토글 없음 — 앱 기본을 예산 중심으로 재편
2. 계좌는 **화면에서만 제거**, 로직·Room DB는 그대로 잠들게 둠 (`git revert`로 복구 가능)
3. 거래에 **차감 예산 필드 신설** — 기본값은 분류와 매칭되는 월계획 예산, 수정·수동입력에서 다른 예산으로 변경 가능
4. 검토 1탭 확정은 기본값으로 자동 차감 (검토 카드에 예산 선택 UI 추가하지 않음)
5. 매칭 예산이 없으면 **"예산 밖 지출"로 저장 허용** — 예산 탭에서 강한 하이라이트로 행동 유도
6. 예산 초과 허용 — 빨간 초과 표시만, 저장 차단·경고 dialog 없음
7. 자산 탭 → **예산 탭** 개편. 홈 화면은 이번 범위에서 제외
8. 이체·송금 검토는 현행 유지(내 지출/통계 제외), 계좌 연결 제안만 제거
9. 이월 없음 — 남은 예산은 월말 소멸, 사용액은 매달 자동 리셋
10. 월계획은 월 구분 없는 단일 계획 유지 — "전달 가져오기"는 현 구조에서 자동으로 충족되므로 별도 기능 없음

## 현재 코드 사실

- `MonthlyPlanItem(type=BUDGET, category/customCategoryId)` + `buildCategoryBudgetUsages()`가 카테고리별 예산·사용액·남은 금액·사용률을 이미 계산함 (`domain/assets/AssetModels.kt`)
- 차감 매칭은 `transaction.category` 기준 자동 — 거래별 override 개념은 없음
- 계좌는 27개 파일 224곳에 얽힘 (Room entity/DAO, 검토 이체 연결, 수정/수동입력, 잔액 동기화)
- `MonthlyPlanItem`에 월 필드 없음 — 계획은 전 기간 공유, 사용액만 당월 거래로 계산됨

## 불변 조건

1. 계좌 관련 Room 테이블·컬럼·migration 삭제 금지 (화면 단절만)
2. 기존 거래 데이터 보존 — 새 컬럼은 nullable, 기존 행은 "분류 매칭" 동작 유지
3. 예산 차감은 `countsAsActualExpense()` 거래만 — 통계 제외·이체·충전은 예산에 안 잡힘
4. 검토 1탭 확정 흐름의 탭 수 증가 금지
5. 알림 파싱·수집 경로 변경 없음

## Task 개요

| 단계 | Owner | 산출물 |
| --- | --- | --- |
| T1 | Codex | 거래 차감 예산 필드 + Room migration + 차감 계산 override |
| T2 | Codex | 저장/수정/검토확정 경로에 예산 지정 배선 + 기본값 해석 |
| T3 | Claude | 수정 dialog·수동입력: 차감 예산 선택(남은 금액 표시), 계좌 칸 제거 |
| T4 | Claude | 자산 탭 → 예산 탭 개편 (계좌 UI 제거, 진행바·예산 밖 지출 하이라이트·초과 빨강) |
| T5 | Claude | 검토 화면 계좌 이동 제안 숨김, 문구 정리 |
| T6 | 남은 쪽 | 통합·실기기 검증·문서 |

### Task 상태

- [x] **T1 차감 예산 도메인·DB**
- [x] **T2 저장 경로 배선**
- [ ] **T3 수정·수동입력 UI**
- [ ] **T4 예산 탭 개편**
- [ ] **T5 검토 화면 정리**
- [ ] **T6 통합 검증**

## T1 계약 초안 (Codex 구체화)

- `MoneyTransaction.budgetPlanId: Long?` (nullable, 기본 null = 분류 매칭)
- Room migration: `transactions`에 `budgetPlanId INTEGER` 추가 (기존 행 null)
- `buildCategoryBudgetUsages`: `budgetPlanId`가 있으면 해당 plan에 차감, 없으면 기존 분류 매칭
- "예산 밖 지출" 집계: 실지출인데 어느 plan에도 안 잡히는 거래 합계 노출
- 삭제된 plan을 가리키는 budgetPlanId는 분류 매칭으로 fallback

## UI 요구 (T3-T5, Claude)

- 수정 dialog: 계좌 dropdown 제거 → "차감 예산" dropdown (항목: `식비 · 남음 120,000원`, `예산 없음`)
- 예산 탭: 카테고리별 진행바(초과 시 빨강 + `-N원 초과`), 상단에 `예산 밖 지출 N원 · 예산을 만들어 관리해 보세요` 하이라이트 배너
- 계좌 서브탭·총 계좌 잔액 카드·계좌 추가 폼·검토 계좌 이동 카드 제거
- 탭 라벨 자산 → 예산, 아이콘 교체

## Shared File Claims

`ui/AppRoot.kt`·`MainActivity.kt`·`di/AppContainer.kt` 편집 전 `docs/AI_COLLABORATION.md`에 claim

## 검증

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
```

Room migration은 `:app:connectedDebugAndroidTest` migration 테스트 + 실기기 업데이트 설치로 검증

## 범위 제외

- 홈 화면 예산 요약 (다음 단계)
- 월별 예산 히스토리·지난달 복사
- 예산 이월, 봉투 간 이동
- 계좌 로직·DB 삭제
