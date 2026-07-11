# F2: 증권·페이 제공사 라벨 Implementation Plan

Status: in-progress
Owner: Claude
Relay rule: 태스크 완료 시 체크 + main push. 중단 시 첫 미체크 태스크부터 이어받기.

**Goal:** 증권·페이 자산에도 회사 이름(키움증권, 네이버페이 등)을 붙일 수 있게 한다. 기본 목록 제공 + 사용자 자유 입력. 은행의 알림 매칭(bankProvider)과 달리 표시 전용.

**Design:** `AssetAccount.providerLabel: String?` 추가 (자유 텍스트, 표시 전용). Room 마이그레이션 6→7 (`ALTER TABLE asset_accounts ADD COLUMN providerLabel TEXT`). BANK 종류는 기존 bankProvider 경로 유지(providerLabel 미사용). SECURITIES/PAY 폼에 프리셋 칩(증권: 키움/미래에셋/삼성/KB/NH/토스, 페이: 네이버/카카오/토스/삼성/페이코) + 직접 입력. 행 라벨 = `kind.label · (providerLabel ?: bankProvider.displayName) · ****suffix`.

## Tasks

- [x] **Task 1: 도메인 + 마이그레이션 6→7**
  - `AssetModels.kt`: `AssetAccount`에 `val providerLabel: String? = null` 추가. `validatedForSave()`에 providerLabel trim/blank→null, BANK면 providerLabel null 강제.
  - `Entities.kt` `AssetAccountEntity`: `providerLabel: String?` 추가 + 양방향 매핑(`RoomAssetRepository`).
  - `AppDatabase.kt`: version 7, `MIGRATION_6_7` = ALTER TABLE asset_accounts ADD COLUMN providerLabel TEXT. `AppContainer`에 등록.
  - schema `7.json` 생성 확인. `AppDatabaseMigrationTest`에 6→7 케이스(레거시 행 providerLabel null 유지).
  - 검증: `:app:testDebugUnitTest --tests "*AssetAccountForm*" --tests "*DatabaseIntegrity*"` + `:app:assembleDebug`
- [x] **Task 2: 폼 변환 로직**
  - `AssetAccountForm.kt`: `createAssetAccountFromForm`/`updateAssetAccountFromForm`에 `providerLabel: String? = null` 파라미터. SECURITIES/PAY일 때만 저장, BANK/CASH/OTHER는 null.
  - `assetAccountMetadataLabel`: bankProvider 없으면 providerLabel 사용.
  - `AssetAccountFormTest`에 케이스 3개(증권 라벨 저장, 은행이면 무시, 빈 문자열→null).
- [x] **Task 3: 자산 폼 UI**
  - `AssetsScreen.kt` AccountInputCard + AccountEditDialog: kind가 SECURITIES/PAY면 "회사" 선택 영역 노출 — 프리셋 칩(FlowRow) + "직접 입력" OutlinedTextField. 프리셋 상수는 `ui/assets/ProviderPresets.kt` (`securitiesProviderPresets`, `payProviderPresets`).
  - 계좌 행 라벨에 반영 확인(assetAccountMetadataLabel 사용처).
  - 검증: assembleDebug + 기기 스크린샷.
- [ ] **Task 4: 문서/마무리**
  - APP_REVIEW_FIX_LIST F2 상태 done, 이 계획서 Status: complete. main push.
