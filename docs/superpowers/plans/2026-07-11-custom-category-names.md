# F1 Phase B: 사용자 정의 분류 이름 Implementation Plan

Status: in-progress
Owner: Codex
역할: Codex=Task1~2(도메인/DB), Claude=Task3(관리 UI 확장).

**Goal:** 사용자가 완전히 새 분류 이름(예: "데이트비용")을 만들 수 있게 한다. Phase A(내장 분류 켜고 끄기)는 이미 Claude가 완료; 이건 자유 텍스트 분류 저장.

**설계 후보(Codex 결정):** `MoneyTransaction.category: Category?`(enum) → 문자열 기반으로 확장하거나, 사용자 정의 분류 테이블 + 매핑. Room 마이그레이션 필요. 리포트/차트 집계가 category enum에 의존하므로 그 영향 범위가 큼 — Codex가 설계부터.

## Tasks
- [x] **Task 1: 분류 저장 모델 설계+구현** — `user_categories` 별도 테이블과 거래의 이름 스냅샷을 함께 저장해 이름 변경/삭제 후 과거 거래 표시를 보존함. Room 7→8 마이그레이션, 중복 방지 인덱스, 저장소 테스트 완료.
- [ ] **Task 2: 저장/수정/직접입력 경로 반영** — SaveManual/Edit 유즈케이스가 자유 분류명 수용, `CategoryPreferenceStore`와 통합 노출.
- [ ] **Task 3: 설정 "분류 관리" UI 확장(Claude)** — 기존 Phase A 칩 화면에 "새 분류 추가" 입력 + 삭제. Codex 계약 나온 뒤.

---
## 이어받기 (릴레이 바통)
한쪽이 멈추면 다른 세션에 전달:
> AutoMoney 이어서 작업. `docs/AI_COLLABORATION.md`의 "Collaboration Mode"를 읽고, `git fetch` 후 이 계획서의 첫 미체크 태스크부터. `Owner:`를 본인으로 바꿔 커밋.
현재 미커밋 메모: (없음)
