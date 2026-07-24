# Notification Parser Hardening Plan

Status: in-progress
Owner: Codex

## Tasks

- [x] 공통 광고·실패·잔액성 문구 규칙과 Common 파서 회귀 수정
- [ ] Toss 현재 본문 우선 이벤트·금액 선택 회귀 수정
- [ ] Generic 잔액성 문구와 처리 이력 금액 정합성 수정
- [ ] 전체 단위 테스트와 `:app:assembleDebug` 검증
- [ ] 코드 리뷰 반영, `main` 커밋·푸시

## Scope Limits

- 알림 큐 용량 변경은 실제 적체 증거가 없어 제외
- Android 추가 notification extras 수집은 실제 누락 표본이 없어 제외
- UI, Room schema, 외부 의존성 변경 없음

## 이어받기 바통

`main`에서 첫 미체크 태스크부터 RED→GREEN 순서로 진행하고, 관련 테스트와 debug APK 빌드 후 푸시함
