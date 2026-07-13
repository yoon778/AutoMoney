# 사용자 선택 알림 앱 + 본문 게이트 Implementation Plan

Status: in-progress
Owner: Codex 로직(T1·T2·T3·T4·T5·T7·T8) + Claude UI(T6, T7 검증 결과 제공)
Branches: `codex/app-logic` / `claude/ui-polish`

> **For agentic workers:** 태스크별 TDD, 작은 경로별 커밋, `git add .` / `git add -A` 금지

## Goal

정적 패키지 allowlist의 안전성은 유지하면서, 사용자가 실제로 알림을 받는 은행·카드·페이 앱을 직접 선택할 수 있게 한다

핵심 결과:

- 기존 등록 금융 앱은 이전처럼 기본 허용
- 처음 보는 앱은 알림 본문을 읽지 않고 `packageName + 시각 + 횟수`만 감지 목록에 기록
- 사용자가 앱을 켠 뒤 들어오는 **다음 알림부터** 본문 분석
- 사용자 추가 앱은 금액 + 강한 거래행위 조합만 후보화하고 전부 검토함으로 전송
- 미허용 앱은 최근 금융 진단을 덮어쓰지 않음
- 전체 설치 앱 조회와 `QUERY_ALL_PACKAGES` 권한 추가 없음

## 결정 요약

### 채택

```text
NotificationListenerService
→ packageName/postTime만 확인
→ metadata-only 최근 감지 목록 갱신
→ 출처 접근 정책 확인
  → BLOCKED: 즉시 종료, extras 접근 0회
  → TRUSTED: 기존 전용/Common parser
  → SELECTED_UNVERIFIED: 보수적 Generic parser
→ 마스킹 진단
→ 저장 또는 검토함
```

### 미채택

- 모든 앱 본문을 매번 읽고 `계좌|돈|송금` 단일 키워드로 자동 저장
- package prefix/fuzzy match
- 사용자 선택 앱을 정적 `FinancialAppRegistry`의 trusted 앱으로 자동 승격
- 첫 차단 알림 본문 저장 후 나중에 재처리
- 전체 설치 앱 목록 조회

이유: 메신저·쇼핑·광고 오탐, 악성 앱의 가짜 금융 알림, 불필요한 개인정보 접근 방지

## 현재 코드 사실

- `MoneyNotificationListenerService`가 snapshot을 만든 뒤 allowlist 검사함
- 따라서 미지원 앱도 `title/text/bigText`를 읽고 `LastNotificationDiagnostic`에 마스킹 preview를 저장함
- 모든 미지원 앱이 단일 최근 진단을 덮어씀
- 실기기 최근 값이 `com.android.systemui / IGNORED / unsupported package`로 오염된 상태였음
- `FinancialAppRegistry`는 exact package 10개만 신뢰함
- 케이뱅크 `com.kbankwith.smartbank`, KB Pay, 하나Pay 등은 정적 목록 밖
- 기존 타 은행 parser 테스트는 대부분 합성 fixture이며 실제 알림 원문 검증 부족

## 불변 조건

1. 미허용 package에서는 `sbn.notification`, `extras`, `title`, `text`, `bigText`, `textLines`, `notificationKey` 접근·저장 금지
2. 미허용 출처 metadata에는 packageName, lastSeenAt, count만 허용
3. package 비교는 case-sensitive exact equality
4. 정적 registry와 사용자 선택 저장소 분리
5. 사용자 선택 unknown package는 trusted로 승격하지 않음
6. unknown 결과는 parser 결과와 무관하게 `NEEDS_REVIEW`
7. 금액만 또는 키워드만 존재하면 거래 생성 금지
8. 광고·예정·실패·거절 알림은 거래 생성 금지
9. full account number, 원문 notification text, raw exception message 저장/log 금지
10. 새 외부 라이브러리·Room migration 없음

## 소유권·작업 순서

| 단계 | Owner | 산출물 | 선행 조건 |
| --- | --- | --- | --- |
| T1 | Codex | 접근 정책·정적 catalog 계약 | 없음 |
| T2 | Codex | 선택/감지 SharedPreferences 저장소 | T1 |
| T3 | Codex | content-read gate·진단 개인정보 수정 | T2 |
| T4 | Codex | Generic parser·unverified review 강제 | T1·T2 |
| T5 | Codex | headless 배선·Claude 소비 계약 | T1·T2·T3·T4 |
| T6 | Claude | 설정의 감지 앱/토글 UI | T5 main 반영 후 |
| T7 | Codex | 통합·실기기 검증, Claude UI 결과 취합 | T5·T6 |
| T8 | Codex | 문서·claims·최종 검증 | T7 |

### Task 상태

- [ ] **T1 접근 정책·정적 catalog 계약**
- [ ] **T2 선택/감지 저장소**
- [ ] **T3 content-read gate·진단 개인정보**
- [ ] **T4 Generic parser·review 불변식**
- [ ] **T5 headless 배선·Claude 소비 계약**
- [ ] **T6 설정 UI**
- [ ] **T7 통합·Galaxy 검증**
- [ ] **T8 최종 회귀·문서 완료**

완료 커밋마다 해당 Task를 `[x]`로 바꾸고 `Last commit SHA` 갱신
각 Task commit은 해당 agent branch에만 push, main merge는 사용자 검토 gate에서만 수행

### 구현 착수 gate

Codex T1 시작 전 clean worktree와 최신 main 확인

```powershell
git fetch origin
git status --short --branch
git rebase origin/main
git merge-base --is-ancestor origin/main HEAD
```

마지막 명령이 성공한 `codex/*` branch에서만 구현 시작

### Shared File Claims

실제 편집 직전에 `docs/AI_COLLABORATION.md`에 claim 추가

- Codex: `di/AppContainer.kt` — T2 시작 전 claim, T5 main merge 확인 뒤 T8 cleanup commit에서 제거
- Claude: `ui/AppRoot.kt` — T6 시작 전 claim, T6 main merge 확인 뒤 T8 cleanup commit에서 제거
- Claude: `MainActivity.kt` — T6 시작 전 claim, T6 main merge 확인 뒤 T8 cleanup commit에서 제거
- `app/build.gradle.kts`는 의존성 추가가 없으므로 편집 금지

계획 작성만으로 claim 선점하지 않음

---

## 핵심 계약

### 출처 접근 상태

```kotlin
enum class NotificationSourceAccess {
    BLOCKED,
    TRUSTED,
    SELECTED_UNVERIFIED
}
```

판정:

- known registry + `defaultContentAccess=true` + 명시 차단 없음 → `TRUSTED`
- known registry + `defaultContentAccess=false` + 사용자 명시 허용 → `SELECTED_UNVERIFIED`
- known registry + 사용자 명시 차단 → `BLOCKED`
- unknown + 사용자가 직접 켬 → `SELECTED_UNVERIFIED`
- unknown + 선택하지 않음 → `BLOCKED`

`FinancialAppInfo.defaultContentAccess` 기본값은 `false`
현재 동작을 보존할 기존 10개만 `true`로 명시
향후 registry 추가만으로 본문 접근이 자동 확대되지 않게 함

### 사용자 선택 저장소

```kotlin
interface NotificationAppAccessStore {
    fun accessFor(packageName: String): NotificationSourceAccess
    fun setAllowed(packageName: String, allowed: Boolean): NotificationAccessUpdateResult
    fun explicitlyEnabledPackages(): Set<String>
}

enum class NotificationAccessUpdateResult {
    UPDATED,
    LIMIT_REACHED,
    INVALID_PACKAGE,
    DENIED_PACKAGE
}
```

SharedPreferences 값:

- `explicitly_disabled_packages`: 사용자가 끈 package
- `explicitly_enabled_packages`: 기본 차단 package를 사용자가 직접 켠 값

known 앱 목록을 preferences에 복사하지 않음
접근 상태는 registry 기본값 + 두 override set을 순수 resolver로 계산

### metadata-only 감지 저장소

```kotlin
data class ObservedNotificationSource(
    val packageName: String,
    val lastSeenAt: Instant,
    val count: Int
)

interface ObservedNotificationSourceStore {
    fun record(packageName: String, seenAt: Instant)
    fun load(): List<ObservedNotificationSource>
}
```

저장 규칙:

- 동일 package는 count 증가 + lastSeenAt 갱신
- 최근순 LRU hard cap 50개, TTL 없음
- enabled package가 LRU에서 빠져도 access store의 명시 허용 set과 UI에서 union
- 명시 허용 package도 최대 50개
- AutoMoney 자체, `android`, `com.android.systemui`만 exact 제외
- `FLAG_SYSTEM` 일괄 제외 금지 — Samsung Wallet 같은 OEM 금융 앱 보호
- PackageManager 조회 실패 시 packageName만 유지
- 앱 라벨은 표시 시 best-effort 조회, 실패 시 packageName fallback
- 본문·icon·notificationKey·PendingIntent 저장 금지

### Listener 테스트 seam

Android 객체 없이 content 접근 여부를 검증할 작은 coordinator 사용

```kotlin
internal class NotificationDispatchCoordinator(
    private val accessFor: (String) -> NotificationSourceAccess,
    private val recordObserved: (String, Instant) -> Unit,
    private val snapshotBuilder: NotificationSnapshotBuilder
) {
    fun prepare(
        packageName: String,
        postedAt: Instant,
        readContent: () -> NotificationContentFields
    ): PreparedNotification?
}
```

- `BLOCKED`면 `readContent` 호출 0회 + `null`
- 허용이면 `readContent` 정확히 1회 + `PreparedNotification(snapshot, access)` 반환
- Service는 `prepare()` 결과가 있을 때만 `ingest(snapshot, access)` 실행
- Service의 `notification`, `extras`, `key` 접근은 모두 `readContent` lambda 내부에만 존재

### Parser 계층

출처 상태별 parser dispatch:

- `TRUSTED` → 기존 `NotificationParserRouter(Toss, Common)`
- `SELECTED_UNVERIFIED` → 별도 `GenericFinanceNotificationParser` 직접 호출
- Generic parser를 trusted router의 fallback으로 넣지 않음
- `BLOCKED`에서는 어떤 parser도 호출하지 않음

접근 상태 전달은 명시 인자 사용:

```kotlin
suspend fun ingest(
    snapshot: NotificationSnapshot,
    sourceAccess: NotificationSourceAccess
): IngestionResult
```

- `TRUSTED` → Toss/Common router
- `SELECTED_UNVERIFIED` → Generic parser
- `BLOCKED` → `Ignored("blocked source")`
- `RunSampleNotificationScenarioUseCase`는 합성 fixture를 `TRUSTED`로 명시 호출

Generic 판정:

- 정확한 원화 금액 1개
- 같은 normalized line에 강한 거래행위 1개 이상
  - 지출: `결제|승인|사용`
  - 입금: `입금|받음|받았`
  - 이동: `이체|송금|출금|ATM`
  - 취소: `취소|환불`
  - 충전: `충전`
- 차단 우선어: `혜택|할인|이벤트|쿠폰|광고|최대|적립|예정|실패|거절|한도|잔액|잔고|이용가능`
- `돈`, `계좌` 단독 신호 사용 금지
- 금액 여러 개 또는 잔액 표시로 거래금액 특정 불가 시 `Ignored`
- provider/account 추론 금지, `bankAccountHint = null`
- merchant/counterparty/memo 원문 저장 금지, v1은 모두 `null`
- 모든 Parsed 결과 `NEEDS_REVIEW`
- 기존 의미별 reason 재사용
  - EXPENSE → `LOW_CONFIDENCE_CATEGORY`
  - INCOME → `INCOME_UNKNOWN`
  - TRANSFER → `TRANSFER_UNKNOWN`
  - REFUND → `REFUND_OR_CANCEL`
  - WALLET_TOPUP → `WALLET_TOPUP`

### Ingestion 방어선

`NotificationIngestionUseCase.ingest(snapshot, sourceAccess)`도 source access를 확인

- `SELECTED_UNVERIFIED` draft는 parser가 잘못 `AUTO_CONFIRMED`를 반환해도 강제로 `NEEDS_REVIEW` + type별 기존 review reason
- 해당 거래는 검토 완료 전 report/asset balance에 반영 금지
- `BLOCKED` 직접 호출은 `Ignored("blocked source")`

### 본문 크기 제한

Service의 `readContent` lambda에서 먼저 자르고 `NotificationSnapshotBuilder`에서 다시 제한

- title 최대 256자
- text 최대 1,024자
- expanded line 최대 10개
- combined expanded text 최대 4,096자
- 중복 line 제거 유지

### 진단 저장 정책

- `TRUSTED`: 기존 SensitiveTextMasker 적용 preview 유지
- `SELECTED_UNVERIFIED`: package/result/type/fixed reason code만 저장, title은 null, preview는 고정 문구 `사용자 선택 앱 · 원문 미저장`
- `BLOCKED`: 상세 diagnostics 생성 금지
- unknown toggle OFF 시 해당 package의 최근 상세 diagnostic 즉시 삭제
- legacy `unsupported package` 진단은 최초 load에서 preferences를 실제 clear
- raw exception message 대신 예외 class 또는 고정 error code만 저장

---

## Task 1: 출처 접근 정책·정적 catalog 분리

**Owner:** Codex

**Files:**

- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationSourceAccess.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationSourceAccessTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/FinancialAppRegistryTest.kt`

**RED:**

- [ ] known package 기본 `TRUSTED`
- [ ] future known package `defaultContentAccess=false`이면 기본 `BLOCKED`
- [ ] future known package를 직접 허용해도 `SELECTED_UNVERIFIED`
- [ ] known disabled package `BLOCKED`
- [ ] unknown 기본 `BLOCKED`
- [ ] unknown 직접 허용 `SELECTED_UNVERIFIED`
- [ ] `com.bank` 허용이 `com.bank.fake`에 전파되지 않음
- [ ] observed 여부만으로 허용되지 않음
- [ ] catalog 조회 결과가 기존 10개를 exact package로 반환

**GREEN:**

- [ ] registry는 정적 메타데이터만 담당
- [ ] `FinancialAppInfo.defaultContentAccess` 기본 false, 기존 10개만 true
- [ ] 사용자 override는 별도 access store가 담당
- [ ] 기존 10개 package 기본 동작 보존
- [ ] `allAppInfos()`는 외부에서 registry를 변형할 수 없는 snapshot 반환
- [ ] dynamic package를 registry map에 삽입하는 API 없음

**검증:**

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --tests "com.choiyoonseo.automoney.notification.NotificationSourceAccessTest" --tests "com.choiyoonseo.automoney.notification.FinancialAppRegistryTest" --no-daemon --console=plain
```

**Commit:** `feat: define notification source access policy`

---

## Task 2: 사용자 선택·metadata-only 감지 저장소

**Owner:** Codex

**Files:**

- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationAppAccessStore.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/ObservedNotificationSourceStore.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationAppAccessStoreTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/ObservedNotificationSourceStoreTest.kt`
- Android Test: `app/src/androidTest/java/com/choiyoonseo/automoney/notification/NotificationSourceStoresInstrumentedTest.kt`

**Persistence:** `SharedPreferences("notification_sources")`

**Claim:** `di/AppContainer.kt` 편집 전 claim, Codex T5 main merge까지 유지

Local JVM에서는 Android `SharedPreferences`를 직접 실행하지 않음
상태 reducer·Map/StringSet codec·LRU는 순수 함수 + fake map으로 단위 테스트
실제 adapter round-trip은 androidTest와 Galaxy 재시작 시나리오로 검증

**RED:**

- [ ] known disable/reenable round-trip
- [ ] custom enable/disable round-trip
- [ ] codec round-trip 후 상태 유지
- [ ] 동일 package count/lastSeenAt 갱신
- [ ] 51번째 package 기록 시 가장 오래된 미선택 항목 제거
- [ ] observed hard cap 50 유지
- [ ] LRU에서 빠진 enabled package가 access set union으로 UI 후보에 남음
- [ ] 명시 허용 51번째 요청 거부 또는 기존 선택 해제 요구
- [ ] self/`android`/`com.android.systemui` 미노출
- [ ] 직렬화 값에 title/text/bigText/key 문자열이 존재하지 않음

**GREEN:**

- [ ] defensive copy 후 `StringSet` 수정
- [ ] 관찰 목록 read-modify-write `@Synchronized`
- [ ] 손상된 항목은 skip하고 나머지 복원
- [ ] packageName validation 후 저장
- [ ] 새 라이브러리 없음
- [ ] AppContainer가 두 store의 단일 인스턴스를 제공

**검증:**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.choiyoonseo.automoney.notification.NotificationAppAccessStoreTest" --tests "com.choiyoonseo.automoney.notification.ObservedNotificationSourceStoreTest" --no-daemon --console=plain
.\gradlew.bat :app:compileDebugAndroidTestKotlin :app:assembleDebug --no-daemon --console=plain
```

**Commit:** `feat: persist selected notification apps`

---

## Task 3: 본문 접근 gate·진단 개인정보 수정

**Owner:** Codex

**Files:**

- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDispatchCoordinator.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationSnapshotBuilder.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDispatchCoordinatorTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerServiceTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationSnapshotBuilderTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt`
- Android Test: `app/src/androidTest/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreInstrumentedTest.kt`

**기존 잘못된 계약 제거:**

- [ ] `unsupportedPackagesStillWriteDiagnostic` 삭제/대체
- [ ] `LastNotificationDiagnostic.fromUnsupportedPackage(snapshot)` 삭제
- [ ] listener의 snapshot-before-gate 순서 제거

**RED:**

- [ ] `BLOCKED` source에서 throwing `readContent` lambda가 0회
- [ ] `TRUSTED` / `SELECTED_UNVERIFIED`에서 lambda 정확히 1회
- [ ] blocked source prepare 결과 null, Service가 ingestion coroutine을 시작하지 않음
- [ ] blocked source가 기존 last financial diagnostic을 덮어쓰지 않음
- [ ] listener source에서 모든 `notification/extras/key` 접근이 lazy content lambda 내부에만 위치
- [ ] oversized title/text/expanded lines가 제한됨
- [ ] diagnostic error에 raw `throwable.message` 저장하지 않음
- [ ] legacy `message=unsupported package` 진단 load 시 null 처리

**GREEN:**

- [ ] blocked source는 observed metadata만 저장
- [ ] 허용 source만 snapshot/ingestion/상세 diagnostic 생성
- [ ] `readContent`에서 textLines `take(10)` 및 문자열 길이 제한 후 snapshot builder 전달
- [ ] CharSequence는 전체 `.toString()` 전에 `subSequence(0, min(length, limit))`로 제한
- [ ] textLines는 전체 `.map` 전에 `asSequence().take(10)` 적용
- [ ] error diagnostic은 예외 class 또는 고정 error code만 저장
- [ ] 마스킹 preview 기존 동작 유지
- [ ] unverified diagnostic은 raw title/text preview 미저장
- [ ] legacy unsupported preference는 load 시 실제 clear
- [ ] AppContainer가 coordinator 단일 인스턴스를 제공
- [ ] `onDestroy()` scope cancel 유지

**검증:**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.choiyoonseo.automoney.notification.NotificationDispatchCoordinatorTest" --tests "com.choiyoonseo.automoney.notification.MoneyNotificationListenerServiceTest" --tests "com.choiyoonseo.automoney.notification.NotificationSnapshotBuilderTest" --tests "com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest" --no-daemon --console=plain
.\gradlew.bat :app:compileDebugAndroidTestKotlin :app:assembleDebug --no-daemon --console=plain
```

**Commit:** `fix: gate notification content before snapshot`

---

## Task 4: Generic parser·unverified 검토 강제

**Owner:** Codex

**Files:**

- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/GenericFinanceNotificationParser.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/RunSampleNotificationScenarioUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/parser/GenericFinanceNotificationParserTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationIngestionAtomicityTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/RunSampleNotificationScenarioUseCaseTest.kt`

**양성 fixture:**

- [ ] `스타벅스 6,100원 결제 완료` → EXPENSE + review
- [ ] `10,000원 입금` → INCOME + review
- [ ] `10,000원 출금` → TRANSFER/neutral + review
- [ ] `12,000원 결제 취소` → REFUND + review
- [ ] `카카오페이 30,000원 충전` → WALLET_TOPUP + review

**음성 fixture:**

- [ ] `지금 결제하면 10,000원 혜택`
- [ ] `계좌이체 시 5,000원 할인`
- [ ] `송금 이벤트 최대 10,000원`
- [ ] `10,000원 결제 실패`
- [ ] `오늘 10,000원 송금 예정`
- [ ] 금액만 존재
- [ ] 행위 단어만 존재
- [ ] 거래금액과 잔액 등 복수 금액
- [ ] title=`송금 이벤트`, body=`최대 10,000원`

**방어선 테스트:**

- [ ] fake parser가 AUTO_CONFIRMED draft를 반환해도 unverified source 저장값은 NEEDS_REVIEW
- [ ] review reason은 type별 기존 reason으로 강제
- [ ] blocked source 직접 ingest는 ignored
- [ ] unverified 거래가 검토 전 report/asset balance에 반영되지 않음
- [ ] unverified transaction의 merchant/counterparty/memo는 null
- [ ] `RunSampleNotificationScenarioUseCase`는 합성 snapshot을 `TRUSTED`로 명시 호출
- [ ] `ingest(snapshot, sourceAccess)` 변경 후 method reference 컴파일 정상
- [ ] Toss·Common parser 기존 테스트 전부 유지
- [ ] AppContainer가 trusted router와 Generic parser를 별도 주입

**검증:**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --tests "com.choiyoonseo.automoney.domain.parser.GenericFinanceNotificationParserTest" --tests "com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest" --tests "com.choiyoonseo.automoney.domain.parser.TossNotificationParserTest" --tests "com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest" --tests "com.choiyoonseo.automoney.notification.NotificationIngestionAtomicityTest" --tests "com.choiyoonseo.automoney.notification.RunSampleNotificationScenarioUseCaseTest" --no-daemon --console=plain
```

**Commit:** `feat: parse selected notification apps conservatively`

---

## Task 5: Claude 소비 계약·headless 배선

**Owner:** Codex

**Files:**

- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationSourceSettingsService.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationSourceSettingsServiceTest.kt`

**Claude 제공 모델/API:**

```kotlin
data class NotificationSourceOption(
    val packageName: String,
    val displayName: String,
    val isRegistered: Boolean,
    val isDefaultTrusted: Boolean,
    val access: NotificationSourceAccess,
    val lastSeenAt: Instant?,
    val count: Int
)

class NotificationSourceSettingsService {
    fun options(): List<NotificationSourceOption>
    fun setAllowed(
        packageName: String,
        allowed: Boolean
    ): NotificationAccessUpdateResult
}
```

**동작:**

- [ ] options는 registry catalog + observed LRU + explicitly enabled package의 union
- [ ] 전체 설치 앱 열거 없음, 정적 registry catalog만 사용
- [ ] registry label → PackageManager label → packageName fallback
- [ ] enabled 우선, 그 안에서 lastSeenAt 내림차순
- [ ] registry 미감지 항목은 `lastSeenAt=null`, `count=0`
- [ ] unknown ON → `SELECTED_UNVERIFIED`
- [ ] default-trusted known ON → `TRUSTED`
- [ ] registry default-false known ON → `SELECTED_UNVERIFIED`
- [ ] OFF → 즉시 `BLOCKED`
- [ ] 51번째 명시 허용은 `LIMIT_REACHED`, 상태 변경 없음
- [ ] invalid/exact-deny package는 `INVALID_PACKAGE` 또는 `DENIED_PACKAGE`
- [ ] OFF한 package가 최근 detailed diagnostic의 source면 즉시 clear
- [ ] 거래/검토 기록은 toggle OFF로 삭제하지 않음
- [ ] AppContainer가 Settings와 Listener가 공유할 동일 인스턴스 제공
- [ ] Preview/테스트용 nullable/default wiring 가능
- [ ] label resolver를 주입해 PackageManager 없는 JVM에서도 service 순수 테스트 가능

**검증:**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.choiyoonseo.automoney.notification.NotificationSourceSettingsServiceTest" --no-daemon --console=plain
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```

**Handoff gate:**

- [ ] T1~T5 각 커밋 push
- [ ] Codex branch 사용자 검토 후 main merge
- [ ] Claude가 main fetch/rebase 후 T6 시작
- [ ] `di/AppContainer.kt` claim은 T8 cleanup 전까지 유지

**Commit:** `feat: expose notification source settings contract`

---

## Task 6: 설정 UI — 감지 앱 목록·허용 토글

**Owner:** Claude

**선행:** T1~T5 contract commit이 main에 merge된 뒤 시작

**Files:**

- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`
- Optional UI-only tests under `app/src/test/java/com/choiyoonseo/automoney/ui/settings/**`

**Claims:** `ui/AppRoot.kt`, `MainActivity.kt` 편집 전 claim

**Codex 제공 계약:** `NotificationSourceSettingsService.options()/setAllowed()`

**UI 요구:**

- [ ] 설정에 `알림 수집 앱` 카드 추가
- [ ] trusted 앱은 `기본 지원` 표시 + toggle 제공
- [ ] observed unknown 앱은 `감지됨 · 직접 허용 필요` 표시
- [ ] `기본 지원`과 `추가 감지 앱` 그룹 분리
- [ ] packageName을 보조 텍스트로 표시
- [ ] unknown toggle ON 전 확인 문구: 해당 앱 알림 본문을 기기 안에서 분석함
- [ ] unknown 첫 알림은 처리되지 않았고 다음 알림부터 적용됨을 안내
- [ ] toggle OFF 즉시 다음 알림부터 차단
- [ ] 최근 감지순, enabled 항목 우선
- [ ] AutoMoney 자체, `android`, `com.android.systemui` exact deny만 미표시
- [ ] 추가 감지 앱 빈 상태: `새 앱 알림이 감지되면 여기에 표시돼요`
- [ ] 화면 재진입/앱 재시작 후 toggle 상태 유지
- [ ] Settings tab 진입·ON_RESUME·성공한 toggle 뒤 `options()` snapshot 재조회
- [ ] `LIMIT_REACHED`면 상태를 바꾸지 않고 최대 50개 안내
- [ ] `INVALID_PACKAGE` / `DENIED_PACKAGE`면 일반 오류 안내, 원문·raw exception 로그 금지
- [ ] 접근성: row/toggle label에 앱명 + 현재 허용 상태 포함
- [ ] 최근 진단 카드는 지원/사용자 허용 앱 처리 결과만 표시

**Claude 금지 범위:**

- `notification/**`, `domain/parser/**`, `data/**` 수정 금지
- package fuzzy match 추가 금지
- unknown 앱 자동 활성화 금지
- 알림 원문 UI 노출 확대 금지

**검증:**

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
```

**Handoff gate:**

- [ ] Claude commit push
- [ ] 사용자 검토 후 `claude/ui-polish` → main merge
- [ ] `ui/AppRoot.kt`, `MainActivity.kt` claims는 T8 cleanup 전까지 유지
- [ ] Claude가 UI 수동 검증 결과를 T7 Codex에 전달

**Commit:** `feat(ui): choose notification source apps`

---

## Task 7: 통합·Galaxy 실기기 검증

**Owner:** Codex

**Input:** Claude의 T6 UI 수동 검증 결과

**Files:**

- Update: `docs/testing/bank-notification-balance-sync.md`

**Claims:** 신규 없음

검증 중 결함 발견 시 해당 Owner가 별도 수정 커밋 작성
shared file 수정이 필요하면 편집 직전 claim을 다시 추가

결함 loop:

- 로직 결함 → Codex 수정·관련 테스트·push
- UI 결함 → Claude가 최신 main 기반 `claude/*`에서 수정·검증·push
- 사용자 UI fix main merge → Codex `origin/main` 재동기화
- 실패 시나리오 재검증 완료 전 T8 진입 금지

**착수 gate:** T5와 T6가 main에 merge된 뒤 Codex branch를 `origin/main`에 동기화

**Wiring:**

- [ ] AppContainer가 access/observed stores와 coordinator 한 인스턴스씩 제공
- [ ] listener와 parser/ingestion이 같은 access policy 사용
- [ ] Settings가 같은 stores를 사용
- [ ] Generic parser는 trusted router와 분리되고 `SELECTED_UNVERIFIED`에서만 호출
- [ ] Preview는 nullable/default 인자로 유지
- [ ] AndroidManifest 및 `app/build.gradle.kts` 변경 없음

**Galaxy 시나리오:**

1. [ ] AutoMoney notification listener enabled 확인
2. [ ] 케이뱅크 toggle OFF 상태에서 알림 1건 발생
3. [ ] 케이뱅크 package/time/count만 감지 목록에 표시
4. [ ] 최근 금융 진단·거래·검토함 변화 없음
5. [ ] 케이뱅크 toggle ON
6. [ ] 안전한 소액 알림 1건 발생
7. [ ] 원문 없는 고정 preview 진단 생성 + 검토함 진입, 자동확정 없음
8. [ ] 검토 완료 후 transaction/report 반영
9. [ ] `bankAccountHint=null`이므로 검토·편집에서 계좌를 명시 선택하기 전 asset balance 불변
10. [ ] toggle OFF 후 다음 알림 차단
11. [ ] SystemUI/KakaoTalk 알림이 최근 금융 진단을 덮지 않음
12. [ ] 앱 재시작 후 toggle 유지
13. [ ] KB/Hana/IBK/KakaoBank/Toss 기존 동작 회귀 없음

**ADB metadata 확인 — 본문 출력 금지:**

```powershell
$adb='C:\Users\cys04\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb shell settings get secure enabled_notification_listeners
& $adb shell pm list packages com.kbankwith.smartbank
.\gradlew.bat :app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.notification.NotificationSourceStoresInstrumentedTest,com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreInstrumentedTest `
  --no-daemon --console=plain
```

실알림 fixture를 문서/테스트에 남길 때:

- full account/card number 제거
- 사람 이름·상호 등 불필요한 식별자 일반화
- 금액은 parser 검증용 고정 예시로 교체
- 원본 screenshot/본문 commit 금지

**Commit:** `docs: record selected notification source verification`

---

## Task 8: 최종 회귀·문서 완료

**Owner:** Codex

**Files:**

- Modify: `docs/superpowers/plans/2026-07-13-notification-app-selection-content-gate.md`
- Modify: `docs/testing/bank-notification-balance-sync.md`
- Modify: `docs/APP_REVIEW_FIX_LIST.md`
- Modify: `docs/AI_COLLABORATION.md`

- [ ] targeted notification/parser tests 전부 통과
- [ ] full unit tests 통과
- [ ] debug APK build 통과
- [ ] `[DEBUG-*]` 임시 로그 없음
- [ ] raw notification text 로그 없음
- [ ] `QUERY_ALL_PACKAGES` 없음
- [ ] T5/T6 commit이 main에 있음을 확인한 뒤 AppContainer/AppRoot/MainActivity claim 행 제거
- [ ] `docs/testing/bank-notification-balance-sync.md` 실기기 결과 갱신
- [ ] `docs/APP_REVIEW_FIX_LIST.md` N1/N2에 새 gate·사용자 선택 지원 결과 갱신
- [ ] 본 계획서 모든 하위 체크박스와 Task 상태 일치
- [ ] `Status: complete`

**최종 명령:**

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
git status --short --branch
```

**Commit:** `docs: verify selected notification sources`

Commit 뒤 `git push origin codex/app-logic`

**Final handoff — 외부 gate:** 사용자 검토 후 `codex/app-logic` → main merge

사용자 merge 뒤 `git fetch origin` 후 최종 commit이 `origin/main`의 ancestor인지 확인

---

## 테스트 매트릭스

| 출처 | 선택 | 문구 | 기대 |
| --- | --- | --- | --- |
| known bank | 기본 ON | 기존 정상 fixture | 기존 결과 유지 |
| known bank | OFF | 정상 금융 문구 | content read 0, 저장 없음 |
| unknown | OFF | `10,000원 송금` | metadata-only, 저장 없음 |
| unknown | ON | `10,000원 송금 완료` | TRANSFER + TRANSFER_UNKNOWN review |
| unknown | ON | `스타벅스 6,100원 결제 완료` | EXPENSE review, 자동확정 없음 |
| unknown | ON | `계좌이체 시 5,000원 할인` | ignored |
| unknown | ON | 복수 금액 + 잔액 | ignored, 임의 첫 금액 선택 금지 |
| near-match | ON 아님 | trusted package + `.fake` | blocked |
| exact deny | 해당 없음 | `android` / `com.android.systemui` / AutoMoney | 목록·진단 미노출 |

## 롤백

- 사용자: 설정 toggle OFF → 즉시 다음 알림부터 차단
- 앱: 새 stores 제거 시 기존 registry-only 동작으로 복귀 가능
- SharedPreferences 추가 키는 남아도 거래/Room 데이터에 영향 없음
- Room schema 변경 없음
- parser 이상 시 ingestion dispatch에서 Generic parser 경로만 제거 가능

## 범위 제외

- 사용자가 선택한 unknown 앱 자동확정
- unknown 앱의 계좌/provider 추론 및 자동 잔액 반영
- 케이뱅크 `BankProvider` trusted 승격
- 카드사별/페이별 전용 parser
- 과거 차단 알림 재처리
- 클라우드 동기화
- 전체 알림 기록 화면
- ML/LLM 분류
- SMS/OCR/이메일 수집

## 후속 승격 조건

케이뱅크·KB Pay·하나Pay 등을 trusted registry로 승격하려면 별도 작은 변경 필요

1. 실제 알림을 비식별화한 fixture 확보
2. 광고/다중 금액 음성 fixture 포함 parser 테스트
3. provider/account hint 정확성 검증
4. Galaxy 실기기 debit/credit/transfer 확인
5. 그 후 exact package를 registry에 추가

## 커밋·merge 순서

```text
docs plan
→ Codex T1 access policy
→ Codex T2 selection stores
→ Codex T3 privacy gate/diagnostics
→ Codex T4 generic parser/review invariant
→ Codex T5 settings contract/headless wiring
→ 사용자 codex branch → main merge
→ Claude main rebase 후 T6 settings UI
→ 사용자 claude branch → main merge
→ Codex main 동기화 후 T7/T8 통합·최종 검증
→ T8 cleanup commit에서 AppContainer/AppRoot/MainActivity claims 제거
→ Codex push → 사용자 최종 main merge로 claim 해제 게시
```

각 커밋 전 `git status`, 특정 파일만 `git add <path>`

## 이어받기 바통

다른 세션이 이어받을 때:

> AutoMoney 알림 앱 선택 기능 이어서 작업. `AGENTS.md`, `docs/AI_COLLABORATION.md`, `docs/superpowers/plans/2026-07-13-notification-app-selection-content-gate.md`를 읽고 첫 미체크 Task부터 진행. 현재 Owner를 본인으로 갱신하고 Shared File Claims 확인. 미허용 앱에서는 content supplier 0회가 최우선 privacy gate. `git status`로 타 에이전트 변경을 섞지 말 것.

기록 항목:

- Current branch:
- Last commit SHA:
- First unchecked task:
- Prerequisite merge status:
- Active claims:
- Uncommitted files:
- Last verification command/result:
- Galaxy verification status:

현재 상태:

- 계획 작성 완료
- 구현 미시작
- active claim 없음
- 실기기 기존 결함 재현 완료
