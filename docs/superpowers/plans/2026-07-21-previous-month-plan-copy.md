# Previous Month Plan Copy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

Status: in-progress
Owner: Codex logic → Claude UI

**Goal:** 월 계획을 실제 월별 데이터로 바로잡고 지난달의 누락 항목만 중복 없이 복사한다

**Architecture:** `monthly_plan_items`에 `monthKey`와 deterministic `identityKey`를 추가한다. `(monthKey, identityKey)` unique index가 최종 중복 방어선이며 `CopyPreviousMonthPlansUseCase`가 preview와 `OnConflictStrategy.IGNORE` list insert를 제공한다

**Tech Stack:** Kotlin, Room, YearMonth, coroutines Flow, JUnit4, Truth

## Global Constraints

- 알림 이력 계획 완료 후 DB version `14 → 15`
- 복사 대상: 수입, 생활비 예산, 저축, 투자 계획
- 고정지출·실제 거래·사용액은 복사 금지
- 현재 달 중복은 skip, 누락 항목만 한 Room transaction에서 복사
- 같은 달에 재실행해도 중복 없음
- 중복 key는 `type + built-in/custom category + normalized label`
- 기존 월 구분 없는 계획은 migration 실행 시 앱 기준 현재 달로 귀속
- 화면에는 `지난달 계획 불러오기` CTA와 확인 dialog만 추가

---

## File Structure

- `domain/assets/AssetModels.kt`: `MonthlyPlanItem.monthKey`, identity 정규화, copy preview
- `data/local/entity/Entities.kt`: monthKey·identityKey와 unique index
- `data/local/dao/AssetDao.kt`: 월별 observe, preview count, copy SQL
- `data/repository/AssetRepository.kt`: 월별 계약
- `data/repository/RoomAssetRepository.kt`: mapping과 원자 copy
- `domain/assets/CopyPreviousMonthPlansUseCase.kt`: 이전 달 계산과 결과
- `data/local/AppDatabase.kt`: migration 14→15
- Claude `AssetsScreen.kt`: CTA·dialog와 선택 월 Flow

### Task 1: 월별 계획 도메인 계약

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/assets/AssetModels.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/domain/assets/MonthlyPlanIdentityTest.kt`

**Interfaces:**
- Produces: `MonthlyPlanItem.monthKey`, `monthlyPlanIdentityKey(item)`, `PreviousMonthPlanCopyPreview`

- [ ] **Step 1: 실패 테스트 작성**

```kotlin
@Test fun identityNormalizesLabelButSeparatesCategoryKinds() {
    val food = item(label = "  식 비 ", category = Category.FOOD)
    val custom = item(label = "식 비", category = Category.OTHER, customCategoryId = 7)
    assertThat(monthlyPlanIdentityKey(food)).isEqualTo("BUDGET|BUILTIN:FOOD|식비")
    assertThat(monthlyPlanIdentityKey(custom)).isEqualTo("BUDGET|CUSTOM:7|식비")
}

@Test fun investmentAndSavingRemainDistinct() {
    assertThat(monthlyPlanIdentityKey(item(label = "월 계획", category = Category.STOCK)))
        .isNotEqualTo(monthlyPlanIdentityKey(item(label = "월 계획", category = Category.SAVING)))
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*MonthlyPlanIdentityTest'`

Expected: FAIL — monthKey와 identity 함수 미정의

- [ ] **Step 3: 도메인 구현**

```kotlin
data class MonthlyPlanItem(
    val id: Long = 0,
    val label: String,
    val amountWon: Long,
    val type: MonthlyPlanItemType,
    val monthKey: YearMonth = YearMonth.now(AppDateZoneId),
    val category: Category? = null,
    val customCategoryId: Long? = null,
    val customCategoryName: String? = null
)

data class PreviousMonthPlanCopyPreview(
    val sourceMonth: YearMonth,
    val targetMonth: YearMonth,
    val copyCount: Int,
    val skipCount: Int
)

fun monthlyPlanIdentityKey(item: MonthlyPlanItem): String {
    val categoryKey = when {
        item.customCategoryId != null -> "CUSTOM:${item.customCategoryId}"
        item.category != null -> "BUILTIN:${item.category.name}"
        else -> "NONE"
    }
    val normalizedLabel = item.label.trim().lowercase().replace(" ", "")
    return "${item.type.name}|$categoryKey|$normalizedLabel"
}
```

저장 validation에 `amountWon >= 0`, label nonblank, `customCategoryId`와 built-in category 모순 금지를 추가

- [ ] **Step 4: 테스트·커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*MonthlyPlanIdentityTest'`

```bash
git add app/src/main/java/com/choiyoonseo/automoney/domain/assets/AssetModels.kt app/src/test/java/com/choiyoonseo/automoney/domain/assets/MonthlyPlanIdentityTest.kt
git commit -m "feat: define month-scoped plan identity"
```

### Task 2: Room 14→15 월별 migration

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/dao/AssetDao.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/AssetRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomAssetRepository.kt`
- Modify: `app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt`
- Create: `app/schemas/com.choiyoonseo.automoney.data.local.AppDatabase/15.json`

**Interfaces:**
- Consumes: Task 1 identity format
- Produces: 월별 query와 DB unique constraint

- [ ] **Step 1: migration 실패 테스트 작성**

```kotlin
@Test fun migration14To15ScopesExistingPlansAndCreatesUniqueIdentity() {
    helper.createDatabase(EIGHTH_TEST_DB, 14).apply {
        execSQL("INSERT INTO monthly_plan_items(id,label,amountWon,type,category) VALUES(1,'식비',300000,'BUDGET','FOOD')")
        execSQL("INSERT INTO monthly_plan_items(id,label,amountWon,type,category) VALUES(2,'식비',300000,'BUDGET','FOOD')")
        close()
    }
    val db = helper.runMigrationsAndValidate(EIGHTH_TEST_DB, 15, true, AppDatabase.MIGRATION_14_15)
    assertThat(db.singleString("SELECT identityKey FROM monthly_plan_items WHERE id=1"))
        .isEqualTo("BUDGET|BUILTIN:FOOD|식비")
    assertThat(db.singleString("SELECT monthKey FROM monthly_plan_items WHERE id=1"))
        .matches("[0-9]{4}-[0-9]{2}")
    assertThat(db.singleString("SELECT identityKey FROM monthly_plan_items WHERE id=2"))
        .isEqualTo("BUDGET|BUILTIN:FOOD|식비|LEGACY:2")
    db.close()
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`

Expected: FAIL — migration 14→15 미정의

- [ ] **Step 3: entity·DAO 구현**

```kotlin
@Entity(
    tableName = "monthly_plan_items",
    indices = [Index(value = ["monthKey", "identityKey"], unique = true)]
)
data class MonthlyPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amountWon: Long,
    val type: MonthlyPlanItemType,
    val monthKey: String,
    val identityKey: String,
    val category: Category? = null,
    val customCategoryId: Long? = null,
    val customCategoryName: String? = null
)
```

```kotlin
@Query("SELECT * FROM monthly_plan_items WHERE monthKey = :monthKey ORDER BY type ASC, amountWon DESC")
fun observeMonthlyPlanItems(monthKey: String): Flow<List<MonthlyPlanItemEntity>>

@Query("SELECT * FROM monthly_plan_items WHERE monthKey = :monthKey ORDER BY id ASC")
suspend fun monthlyPlanItems(monthKey: String): List<MonthlyPlanItemEntity>

@Query("SELECT * FROM monthly_plan_items WHERE monthKey = :monthKey AND identityKey = :identityKey LIMIT 1")
suspend fun monthlyPlanByIdentity(monthKey: String, identityKey: String): MonthlyPlanItemEntity?

@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertMonthlyPlanItems(items: List<MonthlyPlanItemEntity>): List<Long>
```

repository mapper는 `monthKey = YearMonth.parse(entity.monthKey)`와 `identityKey = monthlyPlanIdentityKey(item)`을 양방향으로 적용한다. 기존 UI가 Task 4까지 컴파일되도록 일시 호환 overload를 유지한다:

```kotlin
fun observeMonthlyPlanItems(month: YearMonth): Flow<List<MonthlyPlanItem>>

fun observeMonthlyPlanItems(): Flow<List<MonthlyPlanItem>> =
    observeMonthlyPlanItems(YearMonth.now(AppDateZoneId))
```

`saveMonthlyPlanItem`은 새 항목의 같은 month/identity가 이미 있으면 기존 ID를 사용해 replace하여 `budgetPlanId` 참조를 보존한다

- [ ] **Step 4: migration 구현**

version 15의 `monthly_plan_items_new`를 만들고 기존 행을 현재 local month로 옮긴다:

```sql
INSERT INTO monthly_plan_items_new(
    id,label,amountWon,type,monthKey,identityKey,category,customCategoryId,customCategoryName
)
SELECT id,label,amountWon,type,
       strftime('%Y-%m','now','localtime'),
       type || '|' ||
       CASE
         WHEN customCategoryId IS NOT NULL THEN 'CUSTOM:' || customCategoryId
         WHEN category IS NOT NULL THEN 'BUILTIN:' || category
         ELSE 'NONE'
       END || '|' || lower(replace(trim(label),' ','')) || '|LEGACY:' || id,
       category,customCategoryId,customCategoryName
FROM monthly_plan_items
```

그 다음 identity별 최소 ID만 suffix를 제거:

```sql
UPDATE monthly_plan_items_new
SET identityKey = substr(identityKey, 1, instr(identityKey, '|LEGACY:') - 1)
WHERE id IN (
    SELECT MIN(id)
    FROM monthly_plan_items_new
    GROUP BY monthKey, substr(identityKey, 1, instr(identityKey, '|LEGACY:') - 1)
)
```

기존 DB에 같은 identity가 여러 개면 행과 `budgetPlanId` 참조를 보존하기 위해 최소 ID만 base identity를 쓰고 나머지는 `baseIdentity|LEGACY:<id>`를 유지한다. 위 SQL은 API 26 SQLite 기본 함수만 사용한다. table 교체 후 unique index 생성. `@Database(version = 15)`로 변경

- [ ] **Step 5: schema·검증·커밋**

Run: `./gradlew :app:kspDebugKotlin :app:compileDebugAndroidTestKotlin :app:assembleDebug`

Expected: BUILD SUCCESSFUL과 `15.json` 생성

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt app/src/main/java/com/choiyoonseo/automoney/data/local/dao/AssetDao.kt app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/AssetRepository.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomAssetRepository.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt app/schemas/com.choiyoonseo.automoney.data.local.AppDatabase/15.json
git commit -m "feat: scope monthly plans by month"
```

### Task 3: repository와 idempotent copy use case

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/AssetRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomAssetRepository.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/assets/CopyPreviousMonthPlansUseCase.kt`
- Modify: `app/src/androidTest/java/com/choiyoonseo/automoney/data/repository/RoomAssetRepositoryTest.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/domain/assets/CopyPreviousMonthPlansUseCaseTest.kt`

**Interfaces:**
- Produces: `observeMonthlyPlanItems(month)`, `previewPreviousMonthCopy(targetMonth)`, `copyPreviousMonthPlans(targetMonth)`

- [ ] **Step 1: 실패 테스트 작성**

```kotlin
@Test fun copiesOnlyMissingItemsAndIsIdempotent() = runTest {
    repository.saveMonthlyPlanItem(item(month = MAY, label = "식비", category = Category.FOOD))
    repository.saveMonthlyPlanItem(item(month = MAY, label = "투자", category = Category.STOCK))
    repository.saveMonthlyPlanItem(item(month = JUNE, label = "식비", category = Category.FOOD))

    assertThat(useCase.preview(JUNE)).isEqualTo(PreviousMonthPlanCopyPreview(MAY, JUNE, 1, 1))
    assertThat(useCase.copy(JUNE)).isEqualTo(1)
    assertThat(useCase.copy(JUNE)).isEqualTo(0)
    assertThat(repository.observeMonthlyPlanItems(JUNE).first()).hasSize(2)
}
```

- [ ] **Step 2: repository 계약 구현**

```kotlin
interface AssetRepository {
    fun observeMonthlyPlanItems(month: YearMonth): Flow<List<MonthlyPlanItem>>
    suspend fun monthlyPlanItems(month: YearMonth): List<MonthlyPlanItem>
    suspend fun saveMonthlyPlanItem(item: MonthlyPlanItem): Long
    suspend fun copyMissingMonthlyPlanItems(source: YearMonth, target: YearMonth): Int
}
```

Room copy는 `db.withTransaction`에서 source를 `distinctBy(::monthlyPlanIdentityKey)`로 정리하고 target identity set에 없는 행만 `id = 0`, `monthKey = target`으로 copy한 뒤 IGNORE insert의 양수 ID 개수를 반환

- [ ] **Step 3: use case 구현**

```kotlin
class CopyPreviousMonthPlansUseCase(private val repository: AssetRepository) {
    suspend fun preview(target: YearMonth): PreviousMonthPlanCopyPreview {
        val source = target.minusMonths(1)
        val sourceItems = repository.monthlyPlanItems(source)
        val distinctSource = sourceItems.distinctBy(::monthlyPlanIdentityKey)
        val targetKeys = repository.monthlyPlanItems(target).map(::monthlyPlanIdentityKey).toSet()
        val copyCount = distinctSource.count { monthlyPlanIdentityKey(it) !in targetKeys }
        return PreviousMonthPlanCopyPreview(source, target, copyCount, sourceItems.size - copyCount)
    }

    suspend fun copy(target: YearMonth): Int =
        repository.copyMissingMonthlyPlanItems(target.minusMonths(1), target)
}
```

- [ ] **Step 4: 테스트·커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*CopyPreviousMonthPlansUseCaseTest' :app:compileDebugAndroidTestKotlin :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/repository/AssetRepository.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomAssetRepository.kt app/src/main/java/com/choiyoonseo/automoney/domain/assets/CopyPreviousMonthPlansUseCase.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/repository/RoomAssetRepositoryTest.kt app/src/test/java/com/choiyoonseo/automoney/domain/assets/CopyPreviousMonthPlansUseCaseTest.kt
git commit -m "feat: copy missing plans from previous month"
```

### Task 4: 호출부 월 범위 전환과 DI 계약

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/assets/PlanCategoryOptions.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/assets/AssetsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/review/ReviewScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/ui/assets/PlanCategoryOptionsTest.kt`

**Interfaces:**
- Consumes: month-scoped repository와 copy use case
- Produces: 현재 월 계획만 보는 기존 화면, copy CTA

- [ ] **Step 1: Codex DI claim과 wiring**

```kotlin
val copyPreviousMonthPlansUseCase = CopyPreviousMonthPlansUseCase(assetRepository)
```

claim 추가 → test/assemble → 작은 commit → push → claim 제거

- [ ] **Step 2: Claude 월별 호출 전환**

각 화면은 기존 무인자 observe를 다음으로 변경:

```kotlin
val month = remember { YearMonth.now(AppDateZoneId) }
val monthlyPlans by remember(assetRepository, month) {
    assetRepository?.observeMonthlyPlanItems(month) ?: flowOf(emptyList())
}.collectAsState(initial = emptyList())
```

새 계획 생성 시 `monthKey = month`를 반드시 전달

모든 호출부 전환 후 `AssetRepository.observeMonthlyPlanItems()` 무인자 호환 overload를 제거

- [ ] **Step 3: 저축·투자 계획 분류 고정**

```kotlin
val planCategoryPool: List<Category> =
    expenseCategoryPool.filterNot { it == Category.OTHER } +
        listOf(Category.SAVING, Category.STOCK, Category.OTHER)

fun planCategoryLabel(category: Category): String = when (category) {
    Category.STOCK -> "투자"
    Category.SAVING -> "저축"
    else -> category.displayName
}
```

저축·투자는 생활비 카드와 분리하되 동일 `BUDGET` 계획·순사용 계산을 재사용

- [ ] **Step 4: CTA와 확인 dialog**

이전 달 source item이 1개 이상일 때만 `지난달 계획 불러오기` 표시. 클릭 시 preview의 `copyCount`, `skipCount`를 보여주고 확인 후 `copy(targetMonth)` 호출. copyCount 0이면 `이미 모두 불러왔어요`만 표시

- [ ] **Step 5: 전체 검증·커밋**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add app/src/main/java/com/choiyoonseo/automoney/ui/assets/PlanCategoryOptions.kt app/src/main/java/com/choiyoonseo/automoney/ui/assets/AssetsScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/review/ReviewScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt app/src/test/java/com/choiyoonseo/automoney/ui/assets/PlanCategoryOptionsTest.kt docs/AI_COLLABORATION.md
git commit -m "feat(ui): copy previous month plans"
```

## 이어받기 바통

Codex tasks 1–3와 DI contract를 push한 뒤 Claude가 task 4 UI를 수행. migration 전 기존 계획은 설치 기기에서 migration 시점의 현재 달에 한 번만 귀속됨
