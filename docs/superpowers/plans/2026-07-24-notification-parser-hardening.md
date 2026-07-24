# Notification Parser Hardening Plan

Status: complete
Owner: Codex

## Tasks

- [x] 공통 광고·실패·잔액성 문구 규칙과 Common 파서 회귀 수정
- [x] Toss 현재 본문 우선 이벤트·금액 선택 회귀 수정
- [x] Generic 잔액성 문구와 처리 이력 금액 정합성 수정
- [x] 전체 단위 테스트와 `:app:assembleDebug` 검증
- [x] 코드 리뷰 반영, `main` 커밋·푸시
- [x] 실기기 기존 DB 유지 `adb install -r` 업데이트

## Scope Limits

- 알림 큐 용량 변경은 실제 적체 증거가 없어 제외
- Android 추가 notification extras 수집은 실제 누락 표본이 없어 제외
- UI, Room schema, 외부 의존성 변경 없음

## 이어받기 바통

완료 — 후속 작업 없음
