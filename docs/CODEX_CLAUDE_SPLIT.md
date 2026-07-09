# Codex / Claude 작업 분리

기준일: 2026-07-09

## Codex 담당

- 거래 수정 분류 로직
  - 수입/입금: 월급, 용돈, 투자성과, 환급, 기타
  - 지출: 식비, 카페/간식, 교통비, 쇼핑, 생활, 기타
  - 계좌 이동/정산/지출 제외: 분류 선택 숨김 가능
- 은행명 별칭 정규화
  - KB, KB국민은행, 국민은행을 같은 계좌 후보로 처리
  - 거래 수정 계좌 목록 중복 제거
- 거래 수정 날짜/시간 선택 로직
  - `TransactionEditDateTime.kt` 기준
  - 달력/시계 선택값을 KST `Instant`로 변환
- 고정지출 출금일 로직
  - `fixedExpenseWithdrawalDayOptions = 1..31`
  - 저장 전 `FixedExpensePlan.validatedForSave()` 호출
- 자산 요약 문구
  - 지난달 비교 데이터 없음
  - “지난 달보다 안정적으로 관리 중” 제거
  - 현재 문구: “N개 계좌 · 현재 등록 잔액 기준”

## Claude 담당

- 거래 수정 팝업 UI polish
  - 삭제/지출제외/취소/저장 버튼 2x2 균형 정렬
  - 분류/계좌/날짜/시간 선택 영역 시각 구분
- 자산 계좌 잔액 수정 affordance 강화
  - 수정 버튼/아이콘/탭 가능 영역 명확화
- 전체 팝업 공통 스타일 개선
  - 제목, 설명, 입력 필드 간격, 버튼 hierarchy 정리
- 홈 수입/지출/저축·이체 그림 위치/크기 조정
  - 카드별 시각 무게 균형

## Claude 완료 기록 (2026-07-09)

- 거래 수정 팝업: `MoneyDialog`/`MoneyPickerField` 신설, 라벨+값+화살표 선택 필드로 시각 구분, 삭제(빨강)/지출제외 · 취소/저장(파랑 채움) 2x2 정렬. 에뮬레이터 확인.
- 자산 계좌 잔액: 행 전체 탭 가능 + 연필 아이콘 "수정" 칩. 에뮬레이터 확인.
- 공통 팝업 셸 `MoneyDialog` 도입(28dp, 제목/설명/내용/버튼 간격 통일) — 다른 팝업들도 점진 적용 예정.
- 홈 흐름 아이콘: 칩 52dp/그림 34dp로 여백 확보, 연결선을 칩 중앙에 정렬, 라벨-값 위계 조정. 에뮬레이터 확인.

## Claude 시작 문장

Claude는 `docs/CODEX_CLAUDE_SPLIT.md`를 먼저 읽고, Codex 담당 로직 파일은 건드리지 말고 Claude 담당 UI 항목만 순서대로 수정해줘.

## 충돌 방지

- Claude는 `TransactionEditDateTime.kt`, `MoneyNameMatcher.kt`, `AssetModels.kt` 로직 변경 피하기
- Claude는 Compose UI 배치/스타일 위주 수정
- Codex는 UI polish 대규모 변경 피하기
