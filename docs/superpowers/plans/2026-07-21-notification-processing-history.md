# Notification Processing History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

Status: in-progress
Owner: Codex logic → Claude UI

**Goal:** 최근 금융 알림의 처리 결과를 민감 원문 없이 설명하고 누락 거래를 직접 기록한다

**Architecture:** listener가 ingestion 결과를 privacy-safe `NotificationHistoryRecord`로 변환해 별도 Room table에 best-effort 저장한다. 30일·200건 제한은 insert transaction에서 정리한다. 누락 수동 기록은 기존 수동 저장 use case를 재사용하고 history에는 생성된 transaction ID만 연결한다

**Tech Stack:** Kotlin, Room, coroutines Flow, JUnit4, Truth, Compose UI handoff

## Global Constraints

- 환급 계획 완료 후 DB version `13 → 14`
- 저장 허용: package, 표시명, 수신 시각, status, 파싱 유형·금액, 고정 reason code, transaction ID
- 저장 금지: title, text, bigText, 계좌·카드·이름, raw exception, icon, notification key, PendingIntent
- 보관 30일·최대 200건, 전체 삭제 지원
- 이력 저장 실패가 거래 저장·다음 알림 처리를 막지 않음
- `IGNORED`·`ERROR`만 직접 기록 가능, 결과는 `SourceType.MANUAL`
- 차단 출처는 기존 metadata-only 관찰 저장소만 사용
- UI는 설정 내부 한 진입점만 추가
- shared contract·DI·AppRoot 편집 전 claim

---

## File Structure

- `domain/notificationhistory/NotificationHistoryModels.kt`: 공개 상태와 reason code
- `domain/notificationhistory/SafeNotificationAmountExtractor.kt`: 단일 안전 금액 prefill
- `data/local/entity/NotificationHistoryEntity.kt`: privacy-safe persistence
- `data/local/dao/NotificationHistoryDao.kt`: 최근 목록·retention·clear
- `data/repository/NotificationHistoryRepository.kt`: 저장 계약
- `data/repository/RoomNotificationHistoryRepository.kt`: best-effort 경계 밖 원자 retention
- `notification/NotificationHistoryRecorder.kt`: ingestion/error → history 변환
- `notification/MoneyNotificationListenerService.kt`: 성공·실패 기록 호출
- `domain/manual/SaveMissedNotificationTransactionUseCase.kt`: 수동 저장 후 history 연결
- `ui/model/NotificationHistoryUi.kt`: Claude가 소비할 최소 계약

### Task 1: privacy-safe 모델과 금액 추출

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/notificationhistory/NotificationHistoryModels.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/notificationhistory/SafeNotificationAmountExtractor.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/notificationhistory/SafeNotificationAmountExtractorTest.kt`

**Interfaces:**
- Consumes: `NotificationSnapshot`, `TransactionType`
- Produces: `NotificationHistoryStatus`, `NotificationHistoryReason`, `NotificationHistoryRecord`, `SafeNotificationAmountExtractor.extract`

- [ ] **Step 1: 실패 테스트 작성**

```kotlin
@Test fun extractsOnlyOneActionAmount() {
    assertThat(extractor.extract(snapshot("스타벅스 6,000원 결제"))).isEqualTo(6_000)
}

@Test fun rejectsAmbiguousAndBalanceAmounts() {
    assertThat(extractor.extract(snapshot("6,000원 또는 6원 결제"))).isNull()
    assertThat(extractor.extract(snapshot("잔액 6,000원"))).isNull()
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*SafeNotificationAmountExtractorTest'`

Expected: FAIL — extractor 미정의

- [ ] **Step 3: 모델과 extractor 구현**

```kotlin
enum class NotificationHistoryStatus {
    SAVED, REVIEW, IGNORED, DUPLICATE, ERROR, RESOLVED_MANUALLY
}

enum class NotificationHistoryReason {
    SAVED_AUTOMATICALLY, REVIEW_REQUIRED, PARSER_IGNORED, DUPLICATE_EVENT,
    PROCESSING_ERROR, MANUAL_RECORD_CREATED
}

data class NotificationHistoryRecord(
    val id: Long = 0,
    val packageName: String,
    val sourceLabel: String?,
    val receivedAt: Instant,
    val status: NotificationHistoryStatus,
    val transactionType: TransactionType?,
    val amountWon: Long?,
    val reason: NotificationHistoryReason,
    val linkedTransactionId: Long?
)
```

```kotlin
class SafeNotificationAmountExtractor {
    fun extract(snapshot: NotificationSnapshot): Long? {
        val candidates = snapshot.combinedText.lineSequence()
            .filter { line -> ACTIONS.any { line.contains(it) } && !line.contains("잔액") && !line.contains("잔고") }
            .flatMap { line -> AMOUNT.findAll(line).map { it.groupValues[1].replace(",", "").toLong() } }
            .distinct().toList()
        return candidates.singleOrNull()?.takeIf { it > 0 }
    }
}

private val ACTIONS = listOf("결제", "승인", "사용", "입금", "출금", "이체", "취소", "환불", "환급", "예수금")
private val AMOUNT = Regex("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*원")
```

- [ ] **Step 4: 테스트·커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*SafeNotificationAmountExtractorTest'`

Expected: PASS

```bash
git add app/src/main/java/com/choiyoonseo/automoney/domain/notificationhistory/NotificationHistoryModels.kt app/src/main/java/com/choiyoonseo/automoney/domain/notificationhistory/SafeNotificationAmountExtractor.kt app/src/test/java/com/choiyoonseo/automoney/domain/notificationhistory/SafeNotificationAmountExtractorTest.kt
git commit -m "feat: define private notification history model"
```

### Task 2: Room 13→14와 retention

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/local/entity/NotificationHistoryEntity.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/local/dao/NotificationHistoryDao.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/repository/NotificationHistoryRepository.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomNotificationHistoryRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt`
- Test: `app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt`
- Create: `app/src/androidTest/java/com/choiyoonseo/automoney/data/repository/RoomNotificationHistoryRepositoryTest.kt`
- Create: `app/schemas/com.choiyoonseo.automoney.data.local.AppDatabase/14.json`

**Interfaces:**
- Consumes: Task 1 model
- Produces: `observeRecent`, `recordAndPrune`, `clear`, `markResolvedManually`

- [ ] **Step 1: 실패 테스트 작성**

```kotlin
@Test fun recordPrunesOlderThan30DaysAndKeepsNewest200() = runTest {
    repeat(205) { index -> repository.recordAndPrune(record(id = 0, receivedAt = NOW.minusSeconds(index.toLong()))) }
    repository.recordAndPrune(record(id = 0, receivedAt = NOW.minus(31, ChronoUnit.DAYS)))
    val rows = repository.observeRecent().first()
    assertThat(rows).hasSize(200)
    assertThat(rows.minOf { it.receivedAt }).isAtLeast(NOW.minus(30, ChronoUnit.DAYS))
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`

Expected: FAIL — entity와 repository 미정의

- [ ] **Step 3: entity·DAO 구현**

```kotlin
@Entity(
    tableName = "notification_history",
    indices = [Index("receivedAt"), Index("linkedTransactionId")]
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val sourceLabel: String?,
    val receivedAt: Instant,
    val status: NotificationHistoryStatus,
    val transactionType: TransactionType?,
    val amountWon: Long?,
    val reason: NotificationHistoryReason,
    val linkedTransactionId: Long?
)
```

```kotlin
@Query("SELECT * FROM notification_history ORDER BY receivedAt DESC, id DESC")
fun observeRecent(): Flow<List<NotificationHistoryEntity>>

@Insert suspend fun insert(entity: NotificationHistoryEntity): Long

@Query("DELETE FROM notification_history WHERE receivedAt < :cutoff")
suspend fun deleteOlderThan(cutoff: Instant)

@Query("DELETE FROM notification_history WHERE id NOT IN (SELECT id FROM notification_history ORDER BY receivedAt DESC, id DESC LIMIT :limit)")
suspend fun keepNewest(limit: Int)

@Query("DELETE FROM notification_history")
suspend fun clear()

@Query("UPDATE notification_history SET status = 'RESOLVED_MANUALLY', reason = 'MANUAL_RECORD_CREATED', linkedTransactionId = :transactionId WHERE id = :historyId AND status IN ('IGNORED', 'ERROR')")
suspend fun markResolvedManually(historyId: Long, transactionId: Long): Int
```

`recordAndPrune`는 `db.withTransaction`에서 insert → `deleteOlderThan(receivedAt.minus(30 days))` → `keepNewest(200)` 순서

- [ ] **Step 4: migration과 schema**

`@Database` entities에 `NotificationHistoryEntity`, abstract DAO를 추가하고 version 14에서 다음 table/index 생성:

```sql
CREATE TABLE IF NOT EXISTS notification_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    packageName TEXT NOT NULL,
    sourceLabel TEXT,
    receivedAt TEXT NOT NULL,
    status TEXT NOT NULL,
    transactionType TEXT,
    amountWon INTEGER,
    reason TEXT NOT NULL,
    linkedTransactionId INTEGER
)
```

Run: `./gradlew :app:kspDebugKotlin :app:compileDebugAndroidTestKotlin`

Expected: PASS와 `14.json` 생성

- [ ] **Step 5: instrumented test·assemble·커밋**

기기 연결 시 Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.repository.RoomNotificationHistoryRepositoryTest`

기기 없으면 `:app:compileDebugAndroidTestKotlin`만 확인했다고 기록

Run: `./gradlew :app:assembleDebug`

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/local/entity/NotificationHistoryEntity.kt app/src/main/java/com/choiyoonseo/automoney/data/local/dao/NotificationHistoryDao.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/NotificationHistoryRepository.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomNotificationHistoryRepository.kt app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/repository/RoomNotificationHistoryRepositoryTest.kt app/schemas/com.choiyoonseo.automoney.data.local.AppDatabase/14.json
git commit -m "feat: persist bounded notification history"
```

### Task 3: listener best-effort 기록

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationHistoryRecorder.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDispatchCoordinator.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDispatchCoordinatorTest.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationHistoryRecorderTest.kt`

**Interfaces:**
- Consumes: `PreparedNotification`, `IngestionResult`, history repository
- Produces: `PreparedNotification.sourceLabel`, `NotificationHistoryRecorder.recordResult`, `recordError`

- [ ] **Step 1: privacy와 failure isolation 테스트 작성**

```kotlin
@Test fun recordContainsNoSnapshotTextOrRawError() = runTest {
    recorder.recordError(prepared(text = "계좌 123-456 홍길동", label = "케이뱅크"), IllegalStateException("secret"))
    val row = fakeRepository.saved.single()
    assertThat(row.sourceLabel).isEqualTo("케이뱅크")
    assertThat(row.reason).isEqualTo(NotificationHistoryReason.PROCESSING_ERROR)
    val entitySource = File("src/main/java/com/choiyoonseo/automoney/data/local/entity/NotificationHistoryEntity.kt").readText()
    assertThat(entitySource).doesNotContain("val title:")
    assertThat(entitySource).doesNotContain("val text:")
    assertThat(entitySource).doesNotContain("val bigText:")
    assertThat(entitySource).doesNotContain("val errorMessage:")
    assertThat(entitySource).doesNotContain("val notificationKey:")
}

@Test fun historyFailureDoesNotReplaceSavedResult() = runTest {
    val result = recorder.bestEffort { throw IllegalStateException("db") }
    assertThat(result).isFalse()
}
```

- [ ] **Step 2: 구현**

`PreparedNotification`에 `sourceLabel: String?`을 추가하고 coordinator 생성자에 `resolveInstalledLabel: (String) -> String?`를 주입한다. 차단 출처는 content를 읽기 전에 기존처럼 `null` 반환

```kotlin
suspend fun recordResult(prepared: PreparedNotification, result: IngestionResult) = bestEffort {
    repository.recordAndPrune(result.toHistoryRecord(prepared, amountExtractor))
}

suspend fun recordError(prepared: PreparedNotification) = bestEffort {
    repository.recordAndPrune(
        NotificationHistoryRecord(
            packageName = prepared.snapshot.packageName,
            sourceLabel = prepared.sourceLabel,
            receivedAt = clock.instant(),
            status = NotificationHistoryStatus.ERROR,
            transactionType = null,
            amountWon = amountExtractor.extract(prepared.snapshot),
            reason = NotificationHistoryReason.PROCESSING_ERROR,
            linkedTransactionId = null
        )
    )
}

internal suspend fun bestEffort(block: suspend () -> Unit): Boolean =
    try { block(); true } catch (_: RuntimeException) { false }
```

listener는 ingestion 뒤 history를 기록한 후 기존 diagnostics·feedback을 호출한다. `recordFailure`를 `private suspend fun recordFailure(...)`로 바꾸고 history error 저장과 diagnostics 저장을 각각 독립 `try`로 보호한다

`IngestionResult.Saved`는 `reviewReason == null`이면 `SAVED`, 아니면 `REVIEW`; `Duplicate`는 `DUPLICATE`; `Ignored`는 `IGNORED`로 변환한다. reason 문자열과 exception message는 저장하지 않고 위 enum reason만 사용한다

- [ ] **Step 3: DI claim·검증·커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*NotificationHistoryRecorderTest' --tests '*NotificationDispatchCoordinatorTest' :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add docs/AI_COLLABORATION.md app/src/main/java/com/choiyoonseo/automoney/notification/NotificationHistoryRecorder.kt app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDispatchCoordinator.kt app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDispatchCoordinatorTest.kt app/src/test/java/com/choiyoonseo/automoney/notification/NotificationHistoryRecorderTest.kt
git commit -m "feat: record notification processing outcomes"
```

완료 커밋에서 claim 제거

### Task 4: 누락 거래 수동 기록 use case

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/manual/SaveMissedNotificationTransactionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/manual/SaveManualTransactionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/MoneyRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepository.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/manual/SaveMissedNotificationTransactionUseCaseTest.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`

**Interfaces:**
- Consumes: `SaveManualTransactionUseCase.createTransaction`, `MoneyRepository.saveManualTransactionFromHistory`
- Produces: `save(historyId, type, amountWon, categoryText, memo, occurredAt): Long`

- [ ] **Step 1: 실패 테스트**

```kotlin
@Test fun savesManualTransactionThenLinksHistory() = runTest {
    val id = useCase.save(7, ManualEntryType.EXPENSE, 6_000, "식비", "스타벅스", OCCURRED_AT)
    assertThat(id).isEqualTo(42)
    assertThat(repository.manualHistoryLinks).containsExactly(7L to 42L)
}

@Test fun nonRecordableHistoryRollsBackTransaction() = runTest {
    repository.recordableHistoryIds.clear()
    assertThrows<IllegalStateException> {
        useCase.save(7, ManualEntryType.EXPENSE, 6_000, "식비", "스타벅스", OCCURRED_AT)
    }
    assertThat(repository.savedTransactions).isEmpty()
}
```

- [ ] **Step 2: 구현**

```kotlin
class SaveMissedNotificationTransactionUseCase(
    private val saveManual: SaveManualTransactionUseCase,
    private val repository: MoneyRepository
) {
    suspend fun save(
        historyId: Long,
        type: ManualEntryType,
        amountWon: Long,
        categoryText: String,
        memo: String,
        occurredAt: Instant
    ): Long {
        require(historyId > 0)
        val transaction = saveManual.createTransaction(
            type = type,
            amountWon = amountWon,
            categoryText = categoryText,
            memo = memo,
            occurredAt = occurredAt
        )
        return repository.saveManualTransactionFromHistory(historyId, transaction)
    }
}
```

기존 `SaveManualTransactionUseCase.save`는 `createTransaction` 결과를 기존처럼 저장하도록 refactor한다. Room repository의 새 API는 한 `db.withTransaction`에서 history status가 `IGNORED`·`ERROR`인지 확인 → 수동 거래 insert → history update count 1 확인을 수행해 어느 단계든 실패하면 둘 다 rollback

```kotlin
suspend fun saveManualTransactionFromHistory(
    historyId: Long,
    transaction: MoneyTransaction
): Long
```

- [ ] **Step 3: 검증·커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*SaveMissedNotificationTransactionUseCaseTest' :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add docs/AI_COLLABORATION.md app/src/main/java/com/choiyoonseo/automoney/domain/manual/SaveMissedNotificationTransactionUseCase.kt app/src/main/java/com/choiyoonseo/automoney/domain/manual/SaveManualTransactionUseCase.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/MoneyRepository.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepository.kt app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt app/src/test/java/com/choiyoonseo/automoney/domain/manual/SaveMissedNotificationTransactionUseCaseTest.kt
git commit -m "feat: record missed notifications manually"
```

### Task 5: mapper 계약과 Claude 최소 UI

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/model/NotificationHistoryUi.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/ui/model/NotificationHistoryUiTest.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`

**Interfaces:**
- Consumes: history Flow, clear, missed save use case
- Produces: `NotificationHistoryRowUi`, 설정 내 목록·직접 기록 action

- [ ] **Step 1: Codex 계약 mapper 작성**

```kotlin
data class NotificationHistoryRowUi(
    val id: Long,
    val sourceLabel: String,
    val receivedAt: Instant,
    val resultLabel: String,
    val amountWon: Long?,
    val canRecordManually: Boolean
)

fun NotificationHistoryRecord.toUi(): NotificationHistoryRowUi =
    NotificationHistoryRowUi(
        id = id,
        sourceLabel = sourceLabel ?: packageName,
        receivedAt = receivedAt,
        resultLabel = status.toKoreanLabel(),
        amountWon = amountWon,
        canRecordManually = status == NotificationHistoryStatus.IGNORED || status == NotificationHistoryStatus.ERROR
    )
```

mapper test에서 status 6개 label과 manual action 조건을 고정

- [ ] **Step 2: contract commit·push 후 Claude handoff**

Run: `./gradlew :app:testDebugUnitTest --tests '*NotificationHistoryUiTest' :app:assembleDebug`

```bash
git add docs/AI_COLLABORATION.md app/src/main/java/com/choiyoonseo/automoney/ui/model/NotificationHistoryUi.kt app/src/test/java/com/choiyoonseo/automoney/ui/model/NotificationHistoryUiTest.kt
git commit -m "feat: expose notification history UI model"
git push origin main
```

- [ ] **Step 3: Claude 설정 UI**

설정에 `알림 처리 내역` 한 행만 추가. 진입 후 최근순 목록, `전체 삭제`, `직접 기록`만 노출. 원문 preview와 진단 message는 표시하지 않음. 직접 기록은 기존 수동 입력 UI를 재사용하고 안전 금액만 initial amount로 전달

- [ ] **Step 4: 최종 검증·커밋**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt docs/AI_COLLABORATION.md
git commit -m "feat(ui): add notification processing history"
```

## 이어받기 바통

Codex tasks 1–5의 계약 commit까지 push 후 Claude가 설정 UI를 이어받음. 차단 출처 content 접근 여부와 entity 필드 privacy test를 회귀 기준으로 유지
