# Room Migration Emulator CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

Status: in-progress
Owner: Codex

**Goal:** production과 test가 같은 migration 목록을 사용하고 GitHub emulator에서 보존 schema부터 현재 version까지 실제 migration을 필수 검증한다

**Architecture:** `AppDatabase.ALL_MIGRATIONS`를 단일 production registry로 만들고 DI와 `MigrationTestHelper`가 공유한다. JVM source `contains()` 검사는 registry 연속성·schema 파일 규칙 검사로 교체한다. 전용 GitHub Actions job이 실제 `AppDatabaseMigrationTest`만 emulator에서 실행한다

**Tech Stack:** Room MigrationTestHelper 2.8.4, Android emulator API 35, GitHub Actions, Gradle 9.6.1, JDK 17

## Global Constraints

- 기능 계획 완료 후 current DB version `15`
- 신뢰 시작점은 저장소에 원본 schema가 있는 v2, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14
- `1.json`, `3.json`은 원본 부재로 생성·역산 금지
- 새 DB version은 새 schema JSON, 연속 migration, migration test가 함께 있어야 함
- AppDatabase migration 목록과 AppContainer 등록 목록 중복 금지
- CI는 migration 동작의 최종 판정, JVM 검사는 빠른 구조 gate
- workflow action 기준: [checkout v6](https://github.com/actions/checkout), [setup-java v5](https://github.com/actions/setup-java), [setup-gradle v6](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md), [android-emulator-runner v2](https://github.com/ReactiveCircus/android-emulator-runner)

---

## File Structure

- `data/local/AppDatabase.kt`: version 상수와 `ALL_MIGRATIONS`
- `di/AppContainer.kt`: registry spread 등록
- `data/local/DatabaseIntegritySchemaTest.kt`: 연속성·schema gate
- `data/local/AppDatabaseMigrationTest.kt`: v2와 보존 시작점→current 실제 검증
- `.github/workflows/android-migration.yml`: emulator CI

### Task 1: production migration registry 단일화

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/data/local/DatabaseIntegritySchemaTest.kt`

**Interfaces:**
- Produces: `APP_DATABASE_VERSION`, `AppDatabase.ALL_MIGRATIONS`

- [ ] **Step 1: registry 실패 테스트 작성**

```kotlin
@Test fun productionMigrationsAreContiguousToCurrentVersion() {
    val migrations = AppDatabase.ALL_MIGRATIONS
    assertThat(migrations.first().startVersion).isEqualTo(1)
    migrations.zipWithNext().forEach { (left, right) ->
        assertThat(left.endVersion).isEqualTo(right.startVersion)
    }
    assertThat(migrations.last().endVersion).isEqualTo(APP_DATABASE_VERSION)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*DatabaseIntegritySchemaTest.productionMigrationsAreContiguousToCurrentVersion'`

Expected: FAIL — registry 미정의

- [ ] **Step 3: registry 구현**

`AppDatabase.kt` top-level:

```kotlin
const val APP_DATABASE_VERSION = 15
```

annotation의 기존 entities 배열은 그대로 두고 다음 두 argument만 변경:

```kotlin
version = APP_DATABASE_VERSION,
exportSchema = true
```

companion 마지막:

```kotlin
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13,
    MIGRATION_13_14,
    MIGRATION_14_15
)
```

`AppContainer`:

```kotlin
).addMigrations(*AppDatabase.ALL_MIGRATIONS).build()
```

- [ ] **Step 4: 기존 source contains 검사 제거와 구조 gate 실행**

`databaseVersionBumpsForIntegrityMigration`의 source text migration 나열 assertions를 제거하고 registry test로 대체. build file의 schema export assertions는 유지

Run: `./gradlew :app:testDebugUnitTest --tests '*DatabaseIntegritySchemaTest' :app:assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: claim 제거·커밋**

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt app/src/test/java/com/choiyoonseo/automoney/data/local/DatabaseIntegritySchemaTest.kt docs/AI_COLLABORATION.md
git commit -m "refactor: centralize Room migrations"
```

### Task 2: 보존 schema→current 실제 migration test

**Files:**
- Modify: `app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/data/local/DatabaseIntegritySchemaTest.kt`

**Interfaces:**
- Consumes: `ALL_MIGRATIONS`, `APP_DATABASE_VERSION`
- Produces: v2→current 보존 검증과 schema inventory gate

- [ ] **Step 1: v2→current test 작성**

```kotlin
@Test fun migration2ToCurrentPreservesLegacyRowsAndValidatesSchema() {
    helper.createDatabase(TEST_DB, 2).apply {
        insertTransaction(id = 10, sourceHash = "legacy-hash")
        insertReviewItem(id = 20, transactionId = 10)
        close()
    }
    val db = helper.runMigrationsAndValidate(
        TEST_DB,
        APP_DATABASE_VERSION,
        true,
        *AppDatabase.ALL_MIGRATIONS
    )
    assertThat(db.singleLong("SELECT COUNT(*) FROM transactions WHERE id = 10")).isEqualTo(1)
    assertThat(db.singleLong("SELECT COUNT(*) FROM review_items WHERE transactionId = 10")).isEqualTo(1)
    db.close()
}
```

- [ ] **Step 2: 각 신규 migration 단독 test가 registry version과 일치하도록 수정**

기존 4→current wallet test는 개별 migration 나열을 `*AppDatabase.ALL_MIGRATIONS`로 교체. 7→8, 8→9처럼 단일 단계의 SQL 보존 test는 해당 migration 직접 전달을 유지해 결함 위치를 좁힘

- [ ] **Step 3: schema inventory JVM gate**

```kotlin
@Test fun everyPreservedVersionAndCurrentSchemaExists() {
    val versions = schemaDir.listFiles { file -> file.extension == "json" }
        .orEmpty().map { it.nameWithoutExtension.toInt() }.sorted()
    assertThat(versions).containsAtLeast(2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, APP_DATABASE_VERSION)
    assertThat(versions).containsNoneOf(1, 3)
}
```

- [ ] **Step 4: 컴파일·로컬 실행 가능 범위 검증**

Run: `./gradlew :app:testDebugUnitTest --tests '*DatabaseIntegritySchemaTest' :app:compileDebugAndroidTestKotlin :app:assembleDebug`

Expected: BUILD SUCCESSFUL

기기 연결 시 Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.local.AppDatabaseMigrationTest`

- [ ] **Step 5: 커밋**

```bash
git add app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt app/src/test/java/com/choiyoonseo/automoney/data/local/DatabaseIntegritySchemaTest.kt
git commit -m "test: validate Room migrations to current schema"
```

### Task 3: GitHub emulator migration workflow

**Files:**
- Create: `.github/workflows/android-migration.yml`

**Interfaces:**
- Consumes: Task 2 instrumented class
- Produces: PR·main push migration gate

- [ ] **Step 1: workflow 작성**

```yaml
name: Android Migration

on:
  pull_request:
  push:
    branches: [main]

permissions:
  contents: read

concurrency:
  group: android-migration-${{ github.ref }}
  cancel-in-progress: true

jobs:
  migration:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v6
      - name: Unit test and assemble
        run: ./gradlew :app:testDebugUnitTest :app:assembleDebug --stacktrace
      - name: Enable KVM
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules
          sudo udevadm trigger --name-match=kvm
      - name: Run Room migration tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 35
          arch: x86_64
          target: google_apis
          profile: pixel_6
          disable-animations: true
          emulator-options: -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back none
          script: ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.local.AppDatabaseMigrationTest --stacktrace
      - name: Verify generated schemas are committed
        run: |
          ./gradlew :app:kspDebugKotlin
          git diff --exit-code -- app/schemas
```

- [ ] **Step 2: workflow 정적 검사**

Run: `git diff --check -- .github/workflows/android-migration.yml`

Expected: 출력 없음

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋·push 후 Actions 확인**

```bash
git add .github/workflows/android-migration.yml
git commit -m "ci: run Room migrations on emulator"
git push origin main
```

GitHub Actions `Android Migration` job이 green인지 확인. 실패하면 job log의 첫 Gradle/ADB 오류를 기준으로 수정하고 동일 workflow commit을 추가

### Task 4: 연결 실기기 설치와 통합 smoke

**Files:**
- Modify: `docs/superpowers/plans/2026-07-21-room-migration-emulator-ci.md`

- [ ] **Step 1: 연결 기기 식별**

Run: `adb devices -l`

Expected: 대상 Galaxy 한 대가 `device` 상태. `offline`·`unauthorized`면 설치 중단 후 USB debugging 승인

- [ ] **Step 2: 기존 데이터 보존 설치**

Run: `./gradlew :app:installDebug`

Expected: `Installed on 1 device`와 BUILD SUCCESSFUL

`adb uninstall`, `pm clear`, 앱 데이터 삭제는 실행하지 않음. 기존 DB에서 migration이 실제 실행되는 경로 유지

- [ ] **Step 3: 앱 시작·crash 확인**

Run: `adb shell am force-stop com.choiyoonseo.automoney`

Run: `adb shell monkey -p com.choiyoonseo.automoney -c android.intent.category.LAUNCHER 1`

Run: `adb logcat -d -t 500 AndroidRuntime:E AutoMoney:E '*:S'`

Expected: 앱 시작 성공, `FATAL EXCEPTION`·Room migration 오류 없음

- [ ] **Step 4: 사용자 흐름 수동 smoke**

- 기존 거래·월 계획이 유지됨
- `6,000원 결제 + 6원 환급` fixture가 순사용 `5,994원`
- 알림 처리 내역에 원문 없이 결과만 표시
- 지난달 계획 복사를 두 번 실행해 두 번째 추가 건수 0
- 암호화 백업 생성 → 거래 1건 추가 → 전체 복원 → 추가 거래만 사라지고 기존 관계 복구
- 설정 외 홈 카드·그래프 증가 없음

각 항목 결과를 계획 하단에 기기 model·Android version·검증 시각과 함께 기록

### Task 5: 문서 제한과 완료 상태

**Files:**
- Modify: `docs/AI_COLLABORATION.md`
- Modify: `docs/superpowers/plans/2026-07-21-room-migration-emulator-ci.md`

- [ ] **Step 1: 알려진 제한 유지 확인**

`1.json`, `3.json` 복구 불가 설명은 삭제하지 않음. source `contains()` 개선 제안은 완료 결과로 변경:

```markdown
- migration 등록은 `AppDatabase.ALL_MIGRATIONS` 단일 registry 사용
- 실제 동작은 GitHub `Android Migration` emulator job이 v2→current로 검증
- v1·v3 원본 schema 부재 제한은 유지
```

- [ ] **Step 2: 최종 검증·상태 완료**

Run: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug`

Expected: BUILD SUCCESSFUL

계획 `Status: complete`, 모든 checkbox 완료, 실제 CI run URL 기록

- [ ] **Step 3: 문서 커밋**

```bash
git add docs/AI_COLLABORATION.md docs/superpowers/plans/2026-07-21-room-migration-emulator-ci.md
git commit -m "docs: record migration CI completion"
git push origin main
```

## 이어받기 바통

첫 미체크 task부터 진행. CI green과 연결 실기기 smoke가 모두 끝나기 전 완료 처리 금지. v1·v3 schema를 추정 생성하지 않음
