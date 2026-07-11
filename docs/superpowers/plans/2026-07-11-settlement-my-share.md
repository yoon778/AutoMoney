# F6: N분의1 정산 "내 몫 + 받을 돈" Implementation Plan

Status: in-progress
Owner: Claude
Relay rule: 태스크 완료 시 체크 + main push. 중단 시 첫 미체크 태스크부터 이어받기.

**Goal (v2 설계, APP_REVIEW_FIX_LIST F6):** 정산 선택 시 내 몫만 지출 통계에 반영하고 그 즉시 통계 확정. 받을 돈은 참고 표시(할 일 아님, 숨김 가능). 회수 입금은 고신뢰(금액 일치)일 때만 1탭 연결 제안, 연결돼도 수입 통계 미포함.

**Data:** Room 7→8. `transactions`에 3열 추가:
- `settlementMyShareWon INTEGER NULL` — SETTLEMENT 거래의 내 몫
- `settlementPartyCount INTEGER NULL` — 나눈 인원(기본 제안용)
- `settlementParentId INTEGER NULL` — 회수 입금 거래가 가리키는 원 정산 거래 id (인덱스)
받을 돈 잔액 = amount - myShare - sum(children.amount). 숨김 = myShare 지정 시 별도 플래그 대신 receivable<=0 또는 `settlementPartyCount = -1`을 숨김 마커로 쓰지 말고 4번째 열 `settlementTrackingHidden INTEGER NOT NULL DEFAULT 0` 추가.

## Tasks

- [ ] **Task 1: 도메인 모델 + 마이그레이션 7→8**
  - `MoneyTransaction`에 4필드(기본 null/false). Entity 매핑, MIGRATION_7_8(ALTER×4 + settlementParentId 인덱스), version 8, schema 8.json, 마이그레이션 테스트.
- [ ] **Task 2: 리포트 규칙 — 내 몫만 실지출**
  - `TransactionReportRules.kt`: SETTLEMENT이고 myShare!=null이면 `countsAsActualExpense=true`로 취급하되 금액은 myShare 기준. 주의: 금액 치환이 필요하므로 합산 지점(`MonthlyReportCalculator`, `MonthlySummaryMapper`, `HomeScreen` expenseWon 계산, `CalendarMapper`)에서 `effectiveExpenseWon(transaction)` 헬퍼(신규, domain/report) 사용으로 통일. myShare null인 기존 SETTLEMENT는 현행(제외) 유지.
  - 회수 입금(settlementParentId!=null)은 수입 통계 제외(`countsAsReportIncome=false`).
  - 기존 리포트 테스트 갱신 + 신규 케이스(내몫 반영, 회수 비수입).
- [ ] **Task 3: 정산 확정 흐름**
  - `ResolveReviewUseCase`: SETTLEMENT 해석 시 `myShareWon`/`partyCount` 파라미터(기본 amount/partyCount 계산). `ReviewResolution.SETTLEMENT` 경로에서 두 필드 저장.
  - `ReviewScreen`: N분의1 선택 다이얼로그 확장 — 인원수 스텝퍼(2~10) + 내 몫 금액 필드(기본 amount/인원 반올림) + 메모. `MoneyDialog` 사용.
- [ ] **Task 4: 회수 연결**
  - 신규 `LinkSettlementRepaymentUseCase(repository)`: 입금 거래에 settlementParentId 지정 + 검토 해제(원자). 잔액효과는 기존 로직 그대로(계좌 반영은 유지, 통계만 비수입).
  - 고신뢰 제안: `ReviewScreen` 입금(INCOME_UNKNOWN/TRANSFER_UNKNOWN) 카드에서, 열려있는 정산 중 `incoming.amount == round(amount/partyCount)` 또는 `== 남은 receivable`이고 14일 이내면 "정산 받은 돈" primary CTA 노출 → 1탭 연결. 조건 계산은 domain 헬퍼 `findSettlementMatch(...)` + 단위테스트.
- [ ] **Task 5: 정산 카드/행 표시**
  - 거래 행: SETTLEMENT+myShare → 금액 대신 "내 몫 X원" 표기 검토(행은 원금 유지, 보조줄에 "내 몫 X원 반영"). 정산 상세(수정 다이얼로그 진입 시)에 "받을 돈 Y / Z원 수령 · 그만 보기" 참고 행 + 숨김 토글(settlementTrackingHidden).
  - 배지: 회수 입금 행에 "정산 회수" 칩(수입 아님 시각화).
- [ ] **Task 6: 검증 + 문서**
  - 전체 unit + assembleDebug + 기기 흐름(정산 확정→홈 반영액=내 몫→회수 연결→수입 미증가) 확인. F6 상태 done, Status: complete, main push.

---
## 이어받기 (릴레이 바통)

한쪽이 멈추면 다른 세션(Claude/Codex)에 아래를 그대로 전달:

> AutoMoney 이어서 작업. `docs/AI_COLLABORATION.md`의 "Shared Plan Relay"를 읽고, `git fetch` 후 이 계획서의 첫 미체크(`- [ ]`) 태스크부터 진행. 시작 전 `Owner:`를 본인으로 바꿔 커밋. 커밋 안 된 반쪽 작업이 있으면 `git status`로 확인 후 완성하거나 이 절에 메모.

현재 미커밋 메모: (없음)
