# Claude UI Handoff: Bank Balance Sync

먼저 읽을 문서:

- `docs/APP_REVIEW_FIX_LIST.md`
- `docs/AI_COLLABORATION.md`
- `docs/testing/bank-notification-balance-sync.md`

## Codex 완료 범위

- 안정 알림 식별자와 중복 방지
- KB, 신한, 하나, 우리, NH, IBK, 카카오뱅크 패키지 등록
- 토스는 본문에 은행명이 없으면 토스뱅크로 추론하지 않음
- 전체 계좌번호가 아닌 끝 4자리만 저장·표시
- 같은 은행·끝 4자리 단일 일치 계좌만 자동 연결
- 중복 계좌, 미등록 계좌, 방향 불명, 잔액 부족은 검토로 전환
- 입금 CREDIT, 출금 DEBIT, 카드·충전 NONE
- 저장·수정·삭제 잔액 효과를 Room transaction으로 원자 처리
- 검토·통계 제외 후에도 실제 잔액 효과 보존
- 내 계좌 이체 검토 시 이미 적용된 효과를 재차감하지 않음
- 수동 거래·수정 거래의 계좌 선택은 stable ID 기반
- 수동 이체는 단일 계좌 효과를 저장하지 않음
- 샘플 알림과 지원 매트릭스 추가

## UI 계약

### 자산 계좌

- `AssetAccount.bankProvider`: 은행 provider, 일반 자산은 `null` 가능
- `AssetAccount.accountLast4`: 숫자 끝 4자리, 일반 자산은 `null` 가능
- 표시 형식은 `****4567`
- `BANK`일 때만 은행·계좌번호 입력 노출
- 편집 시 계좌번호 입력을 비워도 기존 suffix 보존 가능

관련 파일: `domain/assets/AssetModels.kt`, `domain/assets/BankAccountMetadata.kt`, `ui/assets/AssetAccountForm.kt`, `ui/assets/AssetsScreen.kt`

### 거래·수동 입력

- `MoneyTransaction.linkedAssetAccountId`: 연결 자산 계좌 ID
- `MoneyTransaction.balanceImpact`: `CREDIT`, `DEBIT`, `NONE`, legacy는 `null`
- legacy의 `balanceImpact == null`을 UI에서 임의로 `NONE`으로 덮지 않음
- 계좌 선택값은 이름 문자열이 아닌 ID 기준
- 수동 이체는 단일 계좌 effect를 표시하거나 자동 잔액 반영하지 않음

관련 파일: `ui/components/TransactionEditDialog.kt`, `ui/components/TransactionEditAccountOptions.kt`, `ui/transactions/ManualTransactionForm.kt`, `ui/transactions/TransactionsScreen.kt`

### 검토 화면

- `ACCOUNT_UNMATCHED`: 계좌 확인 CTA와 수정 화면 연결
- `ACCOUNT_AMBIGUOUS`: 중복 계좌 안내
- `ACCOUNT_MOVEMENT_UNKNOWN`: 입출금 방향 확인 안내
- `BALANCE_MISMATCH`: 잔액 부족으로 자동 반영하지 않았다는 안내
- `TRANSFER_UNKNOWN`: N분의1·내 계좌 이동 여부 검토
- 검토 상태와 실제 잔액 효과를 구분해 표시

관련 파일: `ui/model/ReviewItemMapper.kt`, `ui/review/ReviewScreen.kt`

## Claude UI 작업 범위

- 자산 계좌 입력 카드의 은행 선택·suffix 마스킹·오류·저장 상태 개선
- 거래 수정의 계좌 선택, 금액·날짜·시간·메모 hierarchy 개선
- 검토 카드의 계좌 사유별 문구와 CTA 개선
- 홈·거래·자산·검토의 자동 반영 badge/chip 추가
- loading·empty·error·dark mode 상태 점검
- popup·bottom sheet 여백, 버튼 hierarchy, destructive action 구분 개선

## 수정 경계

- `domain/assets`, `domain/parser`, `data/local`, `data/repository` 로직 변경 금지
- `MoneyTransaction`, `AssetAccount` 필드 의미 변경 금지
- ID를 이름 문자열로 대체 금지
- 전체 계좌번호·원문 알림 텍스트를 UI state/log에 보관 금지
- 계약 변경 시 이 문서와 `docs/AI_COLLABORATION.md`에 먼저 기록

## 검증

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

실제 은행 원문을 확인하지 못한 provider는 `UNVERIFIED`로 표시. 합성 fixture 통과를 실제 은행 UI 지원 증거로 표현하지 않음

## Claude UI 완료 기록 (2026-07-10)

- 잔액 반영 badge: `TransactionRow`에 `balanceImpact` 파라미터 추가(CREDIT "잔액+" 초록 / DEBIT "잔액-" 빨강). 홈 최근기록·홈 상세 시트·거래 목록에 연결. `ReviewActionCard`는 `card.sourceTransaction.balanceImpact`를 직접 읽어 "잔액 반영" 칩 표시 — 계약 변경 없음.
- 검토 사유별 표현(화면 렌더 계층, mapper 미수정): `ACCOUNT_AMBIGUOUS`(중복 계좌 안내), `ACCOUNT_MOVEMENT_UNKNOWN`(방향 불명·미반영 안내), `BALANCE_MISMATCH`(잔액 부족·미반영 안내) 문구/태그/CTA 오버라이드. 네 가지 계좌 사유 모두 primary가 거래 수정 다이얼로그로 라우팅.
- 자산 폼: 종류/은행 선택을 `MoneyPickerField`(라벨+값+화살표)로 통일, 저장된 suffix는 "등록된 계좌번호 ****XXXX" 안내로 표시, 오류 문구 negative 색. 계좌 수정 팝업을 `MoneyDialog` 공통 셸(취소/저장 2버튼)로 전환.
- 수동 입력 "사용 계좌" 선택도 `MoneyPickerField`로 통일.
- 검증: `:app:testDebugUnitTest` + `:app:assembleDebug` PASS. 에뮬레이터에서 은행 8종 드롭다운·계좌번호 필드 노출·픽커 스타일 확인. 전체 계좌번호·원문 알림은 UI state에 저장하지 않음(입력 필드의 임시 상태만, remember 비저장).
