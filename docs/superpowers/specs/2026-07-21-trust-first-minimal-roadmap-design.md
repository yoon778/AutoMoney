# AutoMoney 신뢰성 우선 최소 보완 설계

Status: approved
Owner: Codex logic → Claude UI 순차 작업
Date: 2026-07-21

## 1. 목표

AutoMoney의 핵심은 계좌 잔액이나 투자 수익률 관리가 아니라 월 사용 계획과 실제 사용 기록이다.
이번 보완은 새 화면과 지표를 늘리지 않고 다음 문제만 해결한다.

1. 기록을 잃지 않고 복원할 수 있어야 한다
2. 알림이 왜 기록되지 않았는지 확인할 수 있어야 한다
3. 결제 후 취소·환급이 오면 실제 사용액이 순액으로 계산돼야 한다
4. 새 달 계획을 반복 입력하지 않아야 한다
5. Room migration이 CI에서 실제 실행 검증돼야 한다

## 2. 포함 범위

- 암호화 전체 백업·전체 교체 복원
- 최근 알림 처리 이력
- 처리되지 않은 알림에서 수동 거래 기록
- 결제·취소·환급 연결과 순사용액 집계
- 지난달 월 계획 복사
- emulator 기반 Room migration CI

## 3. 제외 범위

- 하루 권장 사용액과 월말 예상 지출
- 예산 초과 예상 알림
- 자동 고정지출 추천
- 검토 항목 일괄 처리
- 클라우드 동기화와 다중 기기 병합
- 계좌 잔액 중심 자산관리
- 종목별 매수·매도·수익률 관리
- 백업 데이터 병합 복원

## 4. 사용자 화면 원칙

새 화면 노출은 최소화한다.

- 설정에 `백업 및 복원`
- 설정에 `알림 처리 내역`
- 월 계획 화면에 `지난달 계획 불러오기`
- 환급 연결은 기존 거래 상세·검토 흐름을 사용
- 홈에 카드·그래프·예측 지표를 추가하지 않음

## 5. 구현 순서

1. 결제·취소·환급 연결
2. 알림 처리 이력·누락 거래 기록
3. 지난달 계획 복사
4. 암호화 백업·복원
5. migration CI

환급 연결을 먼저 구현한다. 기존 계획 사용액의 정확도에 직접 영향을 주며, 이후 백업 format이
최종 거래 관계를 포함할 수 있기 때문이다.

## 6. 결제·취소·환급 연결

### 6.1 데이터 모델

환급 거래에 nullable `refundParentTransactionId`를 추가한다.

- 대상은 `TransactionType.REFUND`
- 원결제는 실제 지출 유형만 허용
- self foreign key는 `ON DELETE SET NULL`
- 조회용 index 추가
- 연결되지 않은 환급은 기존처럼 검토 상태 유지

### 6.2 자동 연결

`RefundLinkMatcher`는 다음 조건을 모두 만족하는 단일 후보만 반환한다.

- 같은 금융 앱
- 환급이 결제보다 늦게 발생
- 최근 30일 내 결제
- 환급액이 해당 결제의 남은 미환급액 이하
- 후보가 정확히 하나

신뢰도 강화 신호:

- 상호명이 있으면 정규화 후 일치 우선
- 즉시 캐시백은 가까운 시각의 같은 앱 단일 결제 우선
- 동일 조건 후보가 둘 이상이면 자동 연결 금지

### 6.3 통계 의미

결제와 환급은 각각 보존한다. 수입으로 합산하지 않는다.

```text
순사용액 = 원결제 금액 - 연결된 유효 환급 합계
```

- `6,000원 결제 + 6원 환급 = 5,994원 사용`
- 전액 취소는 0원 사용
- 부분 환급은 차액만 사용
- 생활비·카테고리 예산·투자 계획 모두 동일한 순액 원칙 사용
- 검토 중이거나 제외된 환급은 차감하지 않음
- 연결 합계가 원결제 금액을 넘지 못함

원결제를 삭제할 때 연결 환급은 삭제하지 않는다. 연결을 해제하고 `NEEDS_REVIEW`로 전환하며
review item을 Room transaction 안에서 생성한다.

### 6.4 구성 요소

- `RefundLinkMatcher`: 순수 후보 판정
- `LinkRefundUseCase`: 수동 연결과 검증
- repository atomic API: 연결·해제·원결제 삭제 후 재검토
- report contribution 규칙: 원결제별 순사용액 계산

## 7. 알림 처리 이력·누락 거래 기록

### 7.1 저장 정보

최근 허용 출처 알림의 처리 결과를 Room에 저장한다.

- source package와 표시명
- 수신 시각
- `SAVED`, `REVIEW`, `IGNORED`, `DUPLICATE`, `ERROR`, `RESOLVED_MANUALLY`
- 거래 유형·금액: 파싱 성공 시만
- 고정 reason code
- 연결된 transaction ID

저장 금지:

- 알림 제목·본문·bigText
- 계좌번호·카드번호·사람 이름
- raw exception message
- 알림 icon, key, PendingIntent

차단 출처는 기존 metadata-only 관찰 저장소만 사용하며 처리 이력에 본문 파생 정보를 남기지 않는다.

### 7.2 보관 정책

- 최근 30일
- 최대 200건
- insert 시 만료·초과 항목 정리
- 설정에서 전체 삭제 가능
- 이력 저장 실패가 거래 저장을 실패시키지 않음

### 7.3 누락 거래 기록

`IGNORED` 또는 `ERROR` 행에서 `직접 기록`을 연다.

- 안전하게 추출된 금액이 있으면 금액만 prefill
- 금액이 없으면 빈 수동 입력 폼
- 저장 결과는 `SourceType.MANUAL`
- history 행의 `linkedTransactionId`만 갱신
- 알림 원문 재처리 없음
- 외부 서버 전송 없음

## 8. 지난달 계획 복사

### 8.1 대상

- 월 수입 계획
- 카테고리 생활비 계획
- 저축 계획
- 투자 계획

고정지출 계획은 월별 항목이 아니라 계속 활성 상태이므로 복사하지 않는다. 실제 거래와 사용액도 복사하지 않는다.

### 8.2 동작

- 이전 달 계획이 있을 때 버튼 표시
- 확인 dialog에 복사 예정 수 표시
- 현재 달의 중복 항목은 자동 skip
- 누락 항목만 한 Room transaction에서 복사
- 재실행해도 추가 중복이 생기지 않는 idempotent use case

중복 key:

```text
plan type + built-in category 또는 custom category ID + normalized label
```

금액은 이전 달 값을 그대로 복사하며 사용자가 이후 수정할 수 있다.

## 9. 암호화 백업·전체 복원

### 9.1 백업 범위

포함:

- 거래와 환급·정산 관계
- 월 계획과 고정지출 계획
- 사용자 분류와 분류 표시 설정
- 분류 규칙
- 테마·통화 표시·월 시작일 등 기기와 무관한 portable UI 설정

제외:

- 알림 원문과 처리 이력
- 최근 진단
- notification listener OS 권한
- 기기별 알림 출처 허용 목록
- 임시 UI 상태

### 9.2 파일 format

Android Storage Access Framework로 사용자가 위치를 선택한다.

```text
고정 header
→ format version / KDF parameter / salt / nonce
→ AES-256-GCM encrypted compressed JSON payload
```

- `PBKDF2WithHmacSHA256`
- 파일마다 random salt와 12-byte GCM nonce
- 백업 비밀번호는 8자 이상이며 생성 시 두 번 확인
- 비밀번호와 파생 key 저장 금지
- manifest에 생성 시각, 앱 버전, payload schema version, checksum 포함
- checksum과 manifest는 암호문 내부에 포함
- 미래 format version은 거부하고 앱 업데이트 안내

### 9.3 전체 교체 복원

병합하지 않고 전체 교체한다.

1. header·버전·암호 태그 검증
2. payload schema·금액 범위·enum·참조 무결성 검증
3. 현재 상태를 Android Keystore key로 암호화한 내부 rollback snapshot으로 저장
4. restore journal을 `PREPARED`로 기록
5. Room transaction에서 user tables 전체 교체
6. portable preferences를 synchronous commit
7. journal 완료 후 rollback snapshot 삭제

앱 시작 시 미완료 journal이 있으면 rollback snapshot으로 이전 상태를 복구한다. 복원 실패 시 기존 데이터가
유지돼야 하며, 임시 snapshot은 성공·rollback 완료 후 삭제한다.

비밀번호 오류를 데이터 손상과 구분해 표시한다. 연속 5회 실패 시 해당 화면 세션에서 짧은 입력 지연을 적용한다.

### 9.4 구성 요소

- `BackupManifest`와 versioned payload DTO
- `BackupCodec`: JSON·압축
- `BackupCrypto`: KDF·AES-GCM
- `BackupValidator`: 구조·참조 검증
- `BackupService`: SAF stream orchestration
- `RestoreCoordinator`: journal·DB·preferences·rollback

domain/entity 객체를 직접 직렬화하지 않고 versioned DTO를 사용한다.

## 10. Migration CI

- 현재 migration 목록을 단일 production 상수로 제공
- `AppContainer`와 migration test가 동일 목록 사용
- GitHub CI emulator에서 `AppDatabaseMigrationTest` 실행
- 최소 v2→current와 보존된 schema 시작점별 migration 검증
- 새 DB version PR은 새 schema JSON과 migration test 없으면 실패
- `1.json`, `3.json`은 원본이 없어 생성하지 않으며 알려진 제한으로 유지

local JVM의 source `contains()` 검사는 보조 smoke test만 담당한다. 실제 migration 성공 판정은
`MigrationTestHelper.runMigrationsAndValidate()` 결과로 한다.

## 11. 오류 처리

- 환급 후보가 모호하면 자동 수정하지 않고 검토로 보냄
- history 저장 오류는 원거래 저장 결과를 변경하지 않음
- 계획 복사는 전부 성공하거나 전부 rollback
- 백업 write 실패 시 불완전 파일을 성공으로 표시하지 않음
- 복원 validation 실패 시 DB·preferences 접근 금지
- 복원 중 process death는 journal 기반 rollback
- 미래 enum과 backup version은 추측 변환하지 않고 거부

## 12. 테스트 전략

### Unit

- 전액 취소·부분 환급·즉시 캐시백·복수 후보·초과 환급
- linked refund의 생활비·카테고리·투자 순사용액
- history TTL 30일·200건 cap·민감 필드 부재
- 누락 수동 기록의 history 연결
- 계획 복사 중복 skip·부분 복사·idempotency
- backup DTO round trip·wrong password·tamper·미래 version
- restore validator의 깨진 참조·음수 금액·알 수 없는 enum 거부

### Android integration

- Room migration과 새 index/FK 검증
- DB 전체 교체 복원과 rollback
- process restart 시 restore journal 회복
- SAF stream read/write
- SharedPreferences portable subset 복원

### Regression

- K뱅크 `6,000원 결제 + 6원 환급`
- 기존 결제·수입·송금·증권 예수금 parser
- 검토 전 거래가 report에 반영되지 않는 규칙
- 기존 정산·예산 연결 유지

## 13. 역할과 경계

Codex:

- Room schema·migration·repository
- matcher/use case/report 계산
- backup/history/restore 로직
- UI model mapper와 테스트
- CI migration 검증

Claude:

- 설정의 백업·이력 화면
- 지난달 계획 복사 CTA와 확인 dialog
- 기존 거래·검토 화면의 환급 연결 UI

`di/AppContainer.kt`, UI model 계약, `ui/AppRoot.kt`, `MainActivity.kt`, `app/build.gradle.kts`는 편집 전 claim한다.
Codex가 logic/contract를 작은 main commit으로 먼저 push하고 Claude가 pull한 뒤 UI를 구현한다.

## 14. 완료 기준

- 홈 화면 정보량 증가 없음
- 결제·환급 연결 후 모든 계획 사용액이 순액으로 일치
- 최근 알림 처리 실패를 사용자가 확인하고 수동 기록 가능
- 지난달 계획 복사를 여러 번 눌러도 중복 없음
- 암호화 백업에서 원본 복원과 실패 rollback 검증
- migration emulator CI 통과
- 알림 원문·기기별 권한 정보가 백업과 history에 없음
