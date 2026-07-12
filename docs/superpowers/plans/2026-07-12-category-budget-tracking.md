# 카테고리 예산·실소비 추적 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` or `subagent-driven-development` task-by-task. 체크박스·작은 커밋 유지.

**Goal:** 식비·여가·생활 등 카테고리별 월 예산에 실제 거래를 자동 합산해 `사용 / 남음 / 소진율`을 표시한다.

**Architecture:** 고정지출은 별도 유지. `BUDGET` 월계획은 거래 카테고리 하나에 연결하고, 이번 달 `countsAsActualExpense()` 거래만 동일 카테고리에 합산한다. 수입 항목은 기존처럼 자유 라벨을 유지한다.

**Tech Stack:** Kotlin, Room, Flow, Compose, JUnit/Truth

## 확정 계약

- 예산 하나 = 내장 `Category` 또는 사용자 정의 지출 분류 하나
- `BUDGET`만 분류 연결 필수. `INCOME`은 연결 없음
- 예산 사용액 = 이번 달 `countsAsActualExpense()` 거래의 `effectiveExpenseWon()` 합
- 내장 분류 매칭: `transaction.category == plan.category` 및 `customCategoryId == null`
- 사용자 분류 매칭: `transaction.customCategoryId == plan.customCategoryId`
- 분류 없음·예산 미연결 거래는 예산 사용액에서 제외
- 고정지출은 카테고리 예산 사용액에 합산하지 않음
- 기존 `BUDGET` 레코드는 migration 후 연결 없음으로 보존. UI에서 “분류 연결 필요” 표시

## 파일 구조

- `domain/assets/AssetModels.kt` — 예산 연결 모델·집계 결과
- `data/local/entity/Entities.kt`, `AppDatabase.kt`, `RoomAssetRepository.kt`, `di/AppContainer.kt` — Room 9→10 저장/마이그레이션
- `ui/assets/AssetsScreen.kt` — 예산 입력·행 표시
- `ui/AppRoot.kt` — `userCategoryRepository` 전달
- `ui/settings/CategoryPreferenceStore.kt` — 내장 지출 분류 목록 재사용
- `domain/report/TransactionReportRules.kt` — 기존 실제 지출 판정 재사용

---

### Task 1: 예산-분류 도메인 계약

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/assets/AssetModels.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/assets/CategoryBudgetUsageTest.kt`

**Produces:**

```kotlin
data class MonthlyPlanItem(
    val id: Long = 0,
    val label: String,
    val amountWon: Long,
    val type: MonthlyPlanItemType,
    val category: Category? = null,
    val customCategoryId: Long? = null,
    val customCategoryName: String? = null
)

data class CategoryBudgetUsage(
    val plan: MonthlyPlanItem,
    val spentWon: Long,
    val remainingWon: Long,
    val usedRatio: Float
)

fun buildCategoryBudgetUsages(
    plans: List<MonthlyPlanItem>,
    transactions: List<MoneyTransaction>
): List<CategoryBudgetUsage>
```

- [ ] 테스트: 식비 예산 400,000원 + 식비 거래 120,000원 → 사용 120,000원, 남음 280,000원, `0.3f`
- [ ] 테스트: 사용자 “데이트비용” 예산은 같은 `customCategoryId` 거래만 합산
- [ ] 테스트: 저축·이체·미분류·다른 카테고리 거래는 제외
- [ ] 테스트: 예산 초과 시 남음 음수, ratio는 `1f` clamp
- [ ] 구현: 위 계약. `BUDGET` 필수 연결 검증은 `validatedForSave()`에 추가, legacy null은 읽기 허용
- [ ] 실행: `./gradlew.bat :app:testDebugUnitTest --tests "com.choiyoonseo.automoney.domain.assets.CategoryBudgetUsageTest"`
- [ ] 커밋: `feat: calculate category budget usage`

### Task 2: Room 9→10 저장 계약

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomAssetRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt`
- Create: `app/src/androidTest/java/com/choiyoonseo/automoney/data/repository/RoomCategoryBudgetRepositoryTest.kt`

**Produces:** nullable `category TEXT`, `customCategoryId INTEGER`, `customCategoryName TEXT` columns on `monthly_plan_items`; `MIGRATION_9_10`

- [ ] 테스트: 9→10 migration 기존 월계획 label/금액/type 보존, 새 세 컬럼 null
- [ ] 테스트: Room 저장·관찰 round-trip이 내장·사용자 정의 분류 id/name을 보존
- [ ] 구현: DB version 10, `ALTER TABLE monthly_plan_items ADD COLUMN ...` 3회, AppContainer에 migration 등록, KSP schema `10.json` 생성
- [ ] 실행: `./gradlew.bat :app:compileDebugAndroidTestKotlin`
- [ ] 커밋: `feat: persist budget category links`

### Task 3: 자산 화면 계약·입력

**Owner:** Claude UI, Codex는 `ui/model`/도메인 계약 지원

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/assets/AssetsScreen.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/assets/CategoryBudgetUsageTest.kt`

**Boundary claim required before edit:** `ui/AppRoot.kt`

- [ ] `AssetsScreen`에 `userCategoryRepository: UserCategoryRepository?` 전달
- [ ] 월계획 입력에서 `BUDGET` 선택 시 내장 지출 분류(`expenseCategoryPool`)와 활성 사용자 지출 분류를 한 목록으로 표시
- [ ] 저장값: 내장은 `category`, 사용자 정의는 `category=Category.OTHER`, `customCategoryId`, `customCategoryName`; `INCOME`은 분류 선택 숨김
- [ ] 목록 행: `식비 · 12만 사용 / 40만 예산 · 28만 남음`, progress=`usedRatio`; legacy null은 `분류 연결 필요`
- [ ] 상단 카드는 “생활예산” 대신 `변동지출 예산`, 총 `사용 / 예산 / 남음` 표시. 기존 전체 실지출 `budgetUsedRatio`는 카테고리 예산 집계값으로 교체
- [ ] 실행: 수동 시나리오 — 식비 예산 40만 → 식비 거래 12만 → 12/40·28만 표시; 여가 거래는 식비에 미합산
- [ ] 커밋: `feat: show category budget usage`

### Task 4: 자동 분류·검토 연결 검증

**Files:**
- Modify: `app/src/test/java/com/choiyoonseo/automoney/domain/assets/CategoryBudgetUsageTest.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/domain/transactions/EditTransactionUseCaseTest.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/domain/manual/SaveManualTransactionUseCaseTest.kt`

- [ ] 테스트: 자동 분류된 `Category.FOOD` 거래가 식비 예산에 즉시 반영
- [ ] 테스트: 검토함/수정으로 `Category.HOBBY`로 바꾸면 식비에서 빠지고 여가 예산에 반영
- [ ] 테스트: 사용자 정의 분류 변경도 해당 예산 합계에 반영
- [ ] 실행: `./gradlew.bat :app:testDebugUnitTest`
- [ ] 커밋: `test: cover category budget transaction updates`

### Task 5: 최종 검증·문서

- [ ] `docs/AI_COLLABORATION.md` Shared File Claims 해제
- [ ] `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
- [ ] 에뮬레이터: 예산·자동 알림·검토함 분류 수정 3경로 확인
- [ ] 계획서 `Status: complete`, 모든 태스크 `[x]`
- [ ] 작은 경로별 `git add <paths>`만 사용해 commit/push

## 범위 제외

- 고정지출 자동 실적 대조
- 예산 이월, 주간/일별 한도
- 예산 하나에 여러 카테고리 연결
- 거래 라벨 텍스트 기반 임의 매칭

## 실행 전 확인

- 고정지출을 예산에도 포함할지: 기본값 제외 유지
- legacy 월계획은 삭제 대신 분류 연결 요청 유지
- 코드 변경 전 `ui/AppRoot.kt` claim 등록
