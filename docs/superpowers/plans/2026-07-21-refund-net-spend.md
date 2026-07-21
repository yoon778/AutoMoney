# Refund Linking and Net Spend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

Status: in-progress
Owner: Codex logic → Claude UI

**Goal:** 결제와 환급을 안전하게 연결하고 모든 계획·보고서에서 순사용액을 계산한다

**Architecture:** `refundParentTransactionId` self FK로 원결제 관계를 보존한다. 순수 matcher가 단일 후보만 고르고 Room repository가 연결·검토 해제를 원자 처리한다. 화면별 합산 대신 domain의 순사용 contribution을 공통 사용한다

**Tech Stack:** Kotlin 2.2.21, Room 2.8.4, coroutines, JUnit4, Truth, Android instrumented tests

## Global Constraints

- 구현 순서 1번이며 DB version `12 → 13`
- 같은 금융 앱, 결제 후 30일 이내, 남은 미환급액 이하, 단일 후보만 자동 연결
- 결제·환급 원장은 각각 보존하고 환급을 수입으로 합산하지 않음
- 검토 중·제외 환급은 차감하지 않음
- 원결제 삭제 시 환급 연결 해제와 `NEEDS_REVIEW` 전환을 한 Room transaction에서 처리
- UI 화면·비주얼은 Claude 소유, `ui/model/**` mapper는 Codex 소유
- `di/AppContainer.kt`와 `ui/model/**` 편집 전 `docs/AI_COLLABORATION.md`에 claim 추가
- 매 task마다 관련 테스트와 `:app:assembleDebug`; migration task는 `:app:compileDebugAndroidTestKotlin` 추가

---

## File Structure

- `domain/model/MoneyModels.kt`: 환급 부모 ID 계약
- `domain/refund/RefundLinkMatcher.kt`: 자동 연결 후보 판정
- `domain/refund/LinkRefundUseCase.kt`: 자동·수동 연결 orchestration
- `domain/report/TransactionReportRules.kt`: 순사용 contribution 단일 출처
- `data/local/entity/Entities.kt`: self FK와 index
- `data/local/dao/TransactionDao.kt`: 후보 조회와 연결 갱신
- `data/local/dao/ReviewItemDao.kt`: 거래 기준 검토 해제·재생성
- `data/local/AppDatabase.kt`: migration 12→13
- `data/repository/MoneyRepository.kt`: 환급 연결 persistence 계약
- `data/repository/RoomMoneyRepository.kt`: 원자 연결·삭제 구현
- `notification/NotificationIngestionUseCase.kt`: 저장된 환급 자동 연결 호출
- `domain/assets/AssetModels.kt`: 예산·투자 순사용액
- `ui/model/MonthlySummaryMapper.kt`, `ui/model/CalendarMapper.kt`: 보고서·달력 순액 변환
- Claude UI 파일 4개: 공통 순사용 API 소비와 수동 환급 후보 선택

### Task 1: 환급 관계 도메인과 matcher

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/model/MoneyModels.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/refund/RefundLinkMatcher.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/refund/RefundLinkMatcherTest.kt`

**Interfaces:**
- Consumes: `MoneyTransaction`, `TransactionType`, `TransactionStatus`
- Produces: `MoneyTransaction.refundParentTransactionId: Long?`, `RefundMatchDecision`, `RefundLinkMatcher.eligibleCandidates`, `match`

- [x] **Step 1: 실패 테스트 작성**

```kotlin
class RefundLinkMatcherTest {
    private val matcher = RefundLinkMatcher()

    @Test fun linksKbankCashbackToOnlyRecentPayment() {
        val payment = expense(id = 1, won = 6_000, at = "2026-07-21T01:00:00Z")
        val refund = refund(id = 2, won = 6, at = "2026-07-21T01:00:03Z")
        assertThat(matcher.match(refund, listOf(payment), emptyList()))
            .isEqualTo(RefundMatchDecision.Match(1))
    }

    @Test fun ambiguousCandidatesStayUnlinked() {
        val refund = refund(id = 3, won = 6, at = "2026-07-21T01:00:03Z")
        val candidates = listOf(
            expense(id = 1, won = 6_000, at = "2026-07-21T01:00:00Z"),
            expense(id = 2, won = 8_000, at = "2026-07-21T00:59:00Z")
        )
        assertThat(matcher.match(refund, candidates, emptyList()))
            .isEqualTo(RefundMatchDecision.Ambiguous)
    }

    @Test fun rejectsRefundAboveRemainingAmount() {
        val payment = expense(id = 1, won = 10_000, at = "2026-07-01T00:00:00Z")
        val prior = refund(id = 2, won = 9_000, at = "2026-07-02T00:00:00Z", parentId = 1)
        val incoming = refund(id = 3, won = 2_000, at = "2026-07-03T00:00:00Z")
        assertThat(matcher.match(incoming, listOf(payment), listOf(prior)))
            .isEqualTo(RefundMatchDecision.NoMatch)
    }
}
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*RefundLinkMatcherTest'`

Expected: FAIL — `RefundLinkMatcher`와 `refundParentTransactionId` 미정의

- [x] **Step 3: 최소 구현**

`MoneyTransaction` 마지막 필드에 추가:

```kotlin
val refundParentTransactionId: Long? = null
```

새 matcher:

```kotlin
sealed interface RefundMatchDecision {
    data class Match(val paymentId: Long) : RefundMatchDecision
    data object NoMatch : RefundMatchDecision
    data object Ambiguous : RefundMatchDecision
}

class RefundLinkMatcher {
    fun eligibleCandidates(
        refund: MoneyTransaction,
        candidates: List<MoneyTransaction>,
        linkedRefunds: List<MoneyTransaction>
    ): List<MoneyTransaction> {
        require(refund.type == TransactionType.REFUND)
        val refundedByPayment = linkedRefunds
            .filter { it.type == TransactionType.REFUND && it.isReportableTransaction() }
            .groupBy { it.refundParentTransactionId }
            .mapValues { (_, rows) -> rows.sumOf { it.amount.won } }
        return candidates.filter { payment ->
            payment.id > 0 && payment.type in REFUNDABLE_TYPES &&
                payment.isReportableTransaction() && payment.sourceApp == refund.sourceApp &&
                !payment.occurredAt.isAfter(refund.occurredAt) &&
                payment.occurredAt >= refund.occurredAt.minus(30, ChronoUnit.DAYS) &&
                refund.amount.won <= payment.amount.won - (refundedByPayment[payment.id] ?: 0L)
        }
    }

    fun match(
        refund: MoneyTransaction,
        candidates: List<MoneyTransaction>,
        linkedRefunds: List<MoneyTransaction>
    ): RefundMatchDecision {
        val eligible = eligibleCandidates(refund, candidates, linkedRefunds)
        val merchantMatches = eligible.filter { normalizeMerchant(it.merchant) == normalizeMerchant(refund.merchant) }
            .takeIf { refund.merchant?.isNotBlank() == true && it.isNotEmpty() }
        val immediateMatches = eligible.filter {
            Duration.between(it.occurredAt, refund.occurredAt) <= Duration.ofMinutes(2)
        }.takeIf { it.size == 1 }
        val narrowed = merchantMatches ?: immediateMatches ?: eligible
        return when (narrowed.size) {
            0 -> RefundMatchDecision.NoMatch
            1 -> RefundMatchDecision.Match(narrowed.single().id)
            else -> RefundMatchDecision.Ambiguous
        }
    }
}

private val REFUNDABLE_TYPES = setOf(
    TransactionType.EXPENSE,
    TransactionType.FIXED_EXPENSE,
    TransactionType.WALLET_SPEND,
    TransactionType.SAVING,
    TransactionType.INVESTMENT
)

private fun normalizeMerchant(value: String?): String =
    value.orEmpty().lowercase().replace(Regex("[^가-힣a-z0-9]"), "")
```

- [x] **Step 4: 테스트 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*RefundLinkMatcherTest'`

Expected: PASS

- [x] **Step 5: 커밋**

```bash
git add app/src/main/java/com/choiyoonseo/automoney/domain/model/MoneyModels.kt app/src/main/java/com/choiyoonseo/automoney/domain/refund/RefundLinkMatcher.kt app/src/test/java/com/choiyoonseo/automoney/domain/refund/RefundLinkMatcherTest.kt
git commit -m "feat: add conservative refund matcher"
```

### Task 2: Room 12→13과 원자 연결

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/dao/TransactionDao.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/dao/ReviewItemDao.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/MoneyRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepository.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/test/java/com/choiyoonseo/automoney/data/local/DatabaseIntegritySchemaTest.kt`
- Test: `app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt`
- Test: `app/src/androidTest/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepositoryReviewItemTest.kt`
- Create: `app/schemas/com.choiyoonseo.automoney.data.local.AppDatabase/13.json`

**Interfaces:**
- Consumes: Task 1의 `refundParentTransactionId`
- Produces: `findTransaction`, `refundMatchWindow`, `linkRefundAndResolve`, 삭제 시 자동 재검토

- [x] **Step 1: migration과 repository 실패 테스트 작성**

```kotlin
@Test fun migration12To13AddsRefundParentSelfReference() {
    helper.createDatabase(SEVENTH_TEST_DB, 12).close()
    val db = helper.runMigrationsAndValidate(
        SEVENTH_TEST_DB, 13, true, AppDatabase.MIGRATION_12_13
    )
    assertThat(db.singleLong("SELECT COUNT(*) FROM pragma_foreign_key_list('transactions') WHERE \"from\" = 'refundParentTransactionId' AND on_delete = 'SET NULL'"))
        .isEqualTo(1)
    db.close()
}

@Test fun deletingPaymentUnlinksRefundAndCreatesReview() = runTest {
    val paymentId = repository.saveTransaction(expenseTransaction())
    val refundId = repository.saveTransaction(refundTransaction())
    repository.linkRefundAndResolve(refundId, paymentId, userConfirmed = true)
    repository.deleteTransaction(paymentId)
    val refund = repository.findTransaction(refundId)
    assertThat(refund?.refundParentTransactionId).isNull()
    assertThat(refund?.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
    assertThat(repository.observeOpenReviewItems().first().single().reason)
        .isEqualTo(ReviewReason.REFUND_OR_CANCEL)
}
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`

Expected: FAIL — migration과 repository API 미정의

- [x] **Step 3: entity·DAO·repository 구현**

`TransactionEntity`에 self FK와 index를 추가하고 mapper 양방향에 필드를 전달:

```kotlin
foreignKeys = [
    ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["id"],
        childColumns = ["refundParentTransactionId"],
        onDelete = ForeignKey.SET_NULL
    )
]
```

```kotlin
Index(value = ["refundParentTransactionId"])
```

DAO API:

```kotlin
@Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
suspend fun byId(id: Long): TransactionEntity?

@Query("SELECT * FROM transactions WHERE sourceApp = :sourceApp AND occurredAt BETWEEN :from AND :to ORDER BY occurredAt DESC")
suspend fun refundMatchWindow(sourceApp: String, from: Instant, to: Instant): List<TransactionEntity>

@Query("UPDATE transactions SET refundParentTransactionId = :parentId, status = :status WHERE id = :refundId")
suspend fun updateRefundLink(refundId: Long, parentId: Long?, status: TransactionStatus)

@Query("SELECT * FROM transactions WHERE refundParentTransactionId = :parentId")
suspend fun refundsForParent(parentId: Long): List<TransactionEntity>

@Query("""
    SELECT * FROM transactions
    WHERE monthKey = :monthKey
       OR refundParentTransactionId IN (SELECT id FROM transactions WHERE monthKey = :monthKey)
    ORDER BY occurredAt DESC
""")
fun observeTransactionsForMonth(monthKey: String): Flow<List<TransactionEntity>>
```

```kotlin
@Query("UPDATE review_items SET resolvedAt = :resolvedAt WHERE transactionId = :transactionId AND resolvedAt IS NULL")
suspend fun resolveByTransactionId(transactionId: Long, resolvedAt: Instant)
```

Repository 계약:

```kotlin
suspend fun findTransaction(id: Long): MoneyTransaction?
suspend fun refundMatchWindow(sourceApp: String, from: Instant, to: Instant): List<MoneyTransaction>
suspend fun linkRefundAndResolve(refundId: Long, paymentId: Long, userConfirmed: Boolean)
```

Room 구현에서 `linkRefundAndResolve`는 양쪽 존재, 타입, 시간, source, 잔여액을 transaction 안에서 다시 검증한다. 자동 연결 status는 `AUTO_CONFIRMED`, 사용자 연결은 `USER_EDITED`로 저장하고 open review를 해제한다. 월별 observe는 다음 달에 들어온 환급도 원결제 월 집계가 갱신되도록 부모가 해당 월인 환급을 함께 내보낸다. `deleteTransaction`은 `refundsForParent`를 먼저 읽고 삭제 후 각 환급을 `NEEDS_REVIEW`로 바꾸고 `REFUND_OR_CANCEL` review row를 삽입한다

- [x] **Step 4: migration 구현과 schema 생성**

`@Database(version = 13)`으로 올리고 nullable FK column은 SQLite `ALTER TABLE ADD COLUMN`으로 안전하게 추가한다:

```kotlin
db.execSQL(
    "ALTER TABLE transactions ADD COLUMN refundParentTransactionId INTEGER " +
        "REFERENCES transactions(id) ON DELETE SET NULL"
)
db.execSQL(
    "CREATE INDEX IF NOT EXISTS index_transactions_refundParentTransactionId " +
        "ON transactions(refundParentTransactionId)"
)
```

Run: `./gradlew :app:kspDebugKotlin`

Expected: PASS와 `13.json` 생성

- [x] **Step 5: 검증**

Run: `./gradlew :app:compileDebugAndroidTestKotlin :app:assembleDebug`

Expected: BUILD SUCCESSFUL

검증 결과: unit 295개와 assemble 통과. `adb devices -l`에 연결 기기가 없어 instrumented test는 실행하지 못하고 컴파일만 확인

- [x] **Step 6: 커밋**

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt app/src/main/java/com/choiyoonseo/automoney/data/local/dao/TransactionDao.kt app/src/main/java/com/choiyoonseo/automoney/data/local/dao/ReviewItemDao.kt app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/MoneyRepository.kt app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepository.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/local/AppDatabaseMigrationTest.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepositoryReviewItemTest.kt app/schemas/com.choiyoonseo.automoney.data.local.AppDatabase/13.json
git commit -m "feat: persist refund payment links"
```

### Task 3: 자동·수동 연결 use case와 ingestion

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/refund/LinkRefundUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/refund/LinkRefundUseCaseTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationIngestionAtomicityTest.kt`

**Interfaces:**
- Consumes: Tasks 1–2 matcher와 repository
- Produces: `LinkRefundUseCase.autoLink(refundId)`, `candidates(refundId)`, `linkConfirmed(refundId, paymentId)`, `IngestionResult.Saved.transactionId`

- [ ] **Step 1: 실패 테스트 작성**

```kotlin
@Test fun autoLinkResolvesOnlySingleCandidate() = runTest {
    val decision = useCase.autoLink(refundId = 2)
    assertThat(decision).isEqualTo(RefundMatchDecision.Match(1))
    assertThat(repository.links).containsExactly(RefundLinkCall(2, 1, userConfirmed = false))
}

@Test fun savedRefundRunsAutoLink() = runTest {
    val result = ingestion.ingest(kbankRefundSnapshot(), NotificationSourceAccess.TRUSTED)
    assertThat((result as IngestionResult.Saved).transactionId).isEqualTo(2)
    assertThat(autoLinkedIds).containsExactly(2L)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*LinkRefundUseCaseTest' --tests '*NotificationIngestionAtomicityTest'`

Expected: FAIL — use case와 saved ID 미정의

- [ ] **Step 3: 구현**

```kotlin
class LinkRefundUseCase(
    private val repository: MoneyRepository,
    private val matcher: RefundLinkMatcher = RefundLinkMatcher()
) {
    suspend fun autoLink(refundId: Long): RefundMatchDecision {
        val refund = requireNotNull(repository.findTransaction(refundId))
        val source = refund.sourceApp ?: return RefundMatchDecision.NoMatch
        val rows = repository.refundMatchWindow(source, refund.occurredAt.minus(30, ChronoUnit.DAYS), refund.occurredAt)
        val decision = matcher.match(refund, rows.filter { it.type != TransactionType.REFUND }, rows.filter { it.type == TransactionType.REFUND })
        if (decision is RefundMatchDecision.Match) {
            repository.linkRefundAndResolve(refundId, decision.paymentId, userConfirmed = false)
        }
        return decision
    }

    suspend fun linkConfirmed(refundId: Long, paymentId: Long) {
        repository.linkRefundAndResolve(refundId, paymentId, userConfirmed = true)
    }

    suspend fun candidates(refundId: Long): List<MoneyTransaction> {
        val refund = requireNotNull(repository.findTransaction(refundId))
        val source = requireNotNull(refund.sourceApp)
        val rows = repository.refundMatchWindow(source, refund.occurredAt.minus(30, ChronoUnit.DAYS), refund.occurredAt)
        return matcher.eligibleCandidates(
            refund,
            rows.filter { it.type != TransactionType.REFUND },
            rows.filter { it.type == TransactionType.REFUND }
        )
    }
}
```

`IngestionResult.Saved`에 `transactionId: Long`을 추가한다. 저장 결과가 환급이면 `autoLink`를 호출하고 `Match`일 때 반환 `reviewReason`을 null로 바꾼다. 연결 runtime 실패는 기존 `NEEDS_REVIEW` 거래를 보존하고 Saved 결과를 반환하며 `CancellationException`만 다시 던진다

- [ ] **Step 4: DI claim 후 wiring**

`docs/AI_COLLABORATION.md`에 claim을 추가한 커밋 후 `AppContainer`에 다음을 연결:

```kotlin
val linkRefundUseCase = LinkRefundUseCase(repository)
```

```kotlin
refundAutoLink = linkRefundUseCase::autoLink
```

- [ ] **Step 5: 검증과 커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*LinkRefundUseCaseTest' --tests '*NotificationIngestionAtomicityTest' :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add docs/AI_COLLABORATION.md app/src/main/java/com/choiyoonseo/automoney/domain/refund/LinkRefundUseCase.kt app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt app/src/test/java/com/choiyoonseo/automoney/domain/refund/LinkRefundUseCaseTest.kt app/src/test/java/com/choiyoonseo/automoney/notification/NotificationIngestionAtomicityTest.kt
git commit -m "feat: auto-link unambiguous refunds"
```

완료 커밋에서 claim 줄 제거

### Task 4: 순사용액 단일 계산 경로

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/report/TransactionReportRules.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/assets/AssetModels.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapper.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/model/CalendarMapper.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/domain/report/TransactionReportRulesTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/assets/CategoryBudgetUsageTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapperTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/ui/model/CalendarMapperTest.kt`

**Interfaces:**
- Consumes: linked refund relation
- Produces: `plannedUseContributions(transactions): List<PlannedUseContribution>`

- [ ] **Step 1: 순액 실패 테스트 작성**

```kotlin
@Test fun kbankPaymentMinusCashbackEquals5994() {
    val payment = expense(id = 1, won = 6_000)
    val cashback = refund(id = 2, won = 6, parentId = 1, status = TransactionStatus.AUTO_CONFIRMED)
    assertThat(plannedUseContributions(listOf(payment, cashback)).single().amountWon)
        .isEqualTo(5_994)
}

@Test fun reviewRefundDoesNotReducePlanUsage() {
    val payment = expense(id = 1, won = 6_000)
    val cashback = refund(id = 2, won = 6, parentId = 1, status = TransactionStatus.NEEDS_REVIEW)
    assertThat(plannedUseContributions(listOf(payment, cashback)).single().amountWon)
        .isEqualTo(6_000)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*TransactionReportRulesTest' --tests '*CategoryBudgetUsageTest' --tests '*MonthlySummaryMapperTest' --tests '*CalendarMapperTest'`

Expected: FAIL — contribution API 미정의

- [ ] **Step 3: domain 구현**

```kotlin
data class PlannedUseContribution(
    val transaction: MoneyTransaction,
    val amountWon: Long
)

fun plannedUseContributions(transactions: List<MoneyTransaction>): List<PlannedUseContribution> {
    val validRefunds = transactions
        .filter { it.type == TransactionType.REFUND && it.isReportableTransaction() }
        .mapNotNull { refund -> refund.refundParentTransactionId?.let { it to refund.amount.won } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, amounts) -> amounts.sum() }
    return transactions.filter { it.countsAsPlannedUse() }.map { transaction ->
        PlannedUseContribution(
            transaction = transaction,
            amountWon = (transaction.effectiveExpenseWon() - (validRefunds[transaction.id] ?: 0L)).coerceAtLeast(0)
        )
    }
}
```

`AssetModels`, 두 mapper는 원거래 filter·sum 대신 contribution의 `transaction`과 `amountWon`을 사용한다. category bar와 calendar day도 동일 contribution 목록에서 계산한다

월 mapper는 전달받은 전체 목록에 다음 순서를 사용해 다른 달에 발생한 연결 환급을 놓치지 않는다:

```kotlin
val expenseContributions = plannedUseContributions(transactions).filter {
    it.transaction.monthKey == month && it.transaction.countsAsActualExpense()
}
val expenseWon = expenseContributions.sumOf(PlannedUseContribution::amountWon)
```

- [ ] **Step 4: 검증과 커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*TransactionReportRulesTest' --tests '*CategoryBudgetUsageTest' --tests '*MonthlySummaryMapperTest' --tests '*CalendarMapperTest' :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add app/src/main/java/com/choiyoonseo/automoney/domain/report/TransactionReportRules.kt app/src/main/java/com/choiyoonseo/automoney/domain/assets/AssetModels.kt app/src/main/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapper.kt app/src/main/java/com/choiyoonseo/automoney/ui/model/CalendarMapper.kt app/src/test/java/com/choiyoonseo/automoney/domain/report/TransactionReportRulesTest.kt app/src/test/java/com/choiyoonseo/automoney/domain/assets/CategoryBudgetUsageTest.kt app/src/test/java/com/choiyoonseo/automoney/ui/model/MonthlySummaryMapperTest.kt app/src/test/java/com/choiyoonseo/automoney/ui/model/CalendarMapperTest.kt docs/AI_COLLABORATION.md
git commit -m "feat: calculate reports from net spending"
```

### Task 5: Claude UI 소비와 수동 연결

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/report/MonthlyReportScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/review/ReviewScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/assets/AssetsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`

**Interfaces:**
- Consumes: `plannedUseContributions`, `LinkRefundUseCase.candidates`, `linkConfirmed`
- Produces: 환급 검토 카드의 원결제 선택과 모든 화면의 같은 순액 표시

- [ ] **Step 1: shared file claim**

`ui/AppRoot.kt`, `MainActivity.kt` claim을 `docs/AI_COLLABORATION.md`에 추가하고 main에 push

- [ ] **Step 2: 원결제 후보 UI 연결**

환급 카드에서 최근 30일 후보를 금액·상호·시각으로 표시한다. 한 항목 선택 후 다음 callback만 호출:

```kotlin
scope.launch {
    linkRefundUseCase.linkConfirmed(
        refundId = card.sourceTransaction.id,
        paymentId = selectedPayment.id
    )
}
```

- [ ] **Step 3: 화면별 직접 합산 제거**

`HomeScreen`과 `MonthlyReportScreen`의 `filter { countsAsActualExpense() }.sumOf { effectiveExpenseWon() }`를 다음 형태로 교체:

```kotlin
plannedUseContributions(transactions)
    .filter { it.transaction.countsAsActualExpense() }
    .sumOf(PlannedUseContribution::amountWon)
```

`AssetsScreen`은 `buildAssetOverview`와 `buildCategoryBudgetUsages`의 이미 순액화된 결과만 표시한다

- [ ] **Step 4: 전체 검증과 커밋**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: BUILD SUCCESSFUL

수동 점검: `6,000원 결제 + 6원 환급`에서 홈·예산·보고서 모두 `5,994원`

```bash
git add app/src/main/java/com/choiyoonseo/automoney/ui/home/HomeScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/report/MonthlyReportScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/review/ReviewScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/assets/AssetsScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt docs/AI_COLLABORATION.md
git commit -m "feat(ui): show linked refund net spending"
```

## 이어받기 바통

`main` pull 후 첫 미체크 task부터 진행. Codex tasks 1–4 완료·push 후 Claude가 task 5 수행. 각 task마다 계획 체크박스와 `Status` 갱신
