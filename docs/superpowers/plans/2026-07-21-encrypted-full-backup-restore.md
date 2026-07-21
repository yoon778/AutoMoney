# Encrypted Full Backup and Restore Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

Status: in-progress
Owner: Codex logic → Claude UI

**Goal:** 사용자의 휴대 가능한 금융 기록 전체를 비밀번호 암호화 파일로 백업하고 실패 시 기존 상태를 보존하며 전체 교체 복원한다

**Architecture:** versioned DTO를 JSON→GZIP→AES-256-GCM으로 변환하고 SAF stream으로 읽고 쓴다. 복원은 완전 검증 후 내부 Android Keystore rollback snapshot과 journal을 만든 다음 Room 전체 교체와 portable preferences commit을 수행한다. process death가 있으면 다음 시작에서 snapshot으로 이전 상태를 회복한다

**Tech Stack:** Kotlin serialization JSON 1.9.0, Java Cryptography Architecture, Android Keystore, Room, Storage Access Framework, coroutines

## Global Constraints

- DB version 15 기준, 백업 기능 자체는 schema version을 올리지 않음
- 포함: 거래 관계, 월 계획, 고정지출, 사용자 분류, 규칙, 자산 계정, portable category 표시 설정
- 현재 앱에 저장형 테마·통화·월 시작일 설정은 없으므로 존재하지 않는 설정을 새로 만들지 않음
- 제외: 알림 원문·처리 이력·진단·listener 권한·기기별 출처 허용 목록·onboarding·임시 UI 상태
- 병합 복원 없음, 전체 교체만 제공
- 비밀번호 8자 이상·128자 이하, 생성 시 두 번 확인, 저장 금지
- `PBKDF2WithHmacSHA256` 600,000회, random 16-byte salt, AES-256-GCM random 12-byte nonce
- 파일 format 미래 version은 거부
- 복원 validation 실패 전 DB·preferences 쓰기 금지
- UI는 설정의 `백업 및 복원` 한 진입점만 추가
- shared build/DI/AppRoot/MainActivity 편집 전 claim

보안 근거: [OWASP PBKDF2-HMAC-SHA256 600,000회](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [Android AES-GCM·Keystore 권고](https://developer.android.com/privacy-and-security/cryptography), [Android SAF 문서](https://developer.android.com/training/data-storage/shared/documents-files)

---

## File Structure

- `data/backup/BackupDtos.kt`: format v1 DTO와 manifest
- `data/backup/BackupCodec.kt`: deterministic JSON·GZIP·checksum
- `data/backup/BackupCrypto.kt`: password KDF·portable file envelope
- `data/backup/KeystoreSnapshotCrypto.kt`: 기기 내부 rollback 암호화
- `data/backup/BackupDao.kt`: 포함 table 전체 export·insert
- `data/backup/RoomBackupStore.kt`: DB snapshot·전체 교체
- `data/backup/PortableSettingsStore.kt`: allowlist preference snapshot
- `data/backup/BackupValidator.kt`: enum·범위·참조 검증
- `data/backup/RestoreJournal.kt`: PREPARED/COMPLETED 상태
- `data/backup/BackupService.kt`: export stream orchestration
- `data/backup/RestoreCoordinator.kt`: validation·rollback·replace
- Claude 설정 UI: SAF launcher와 비밀번호 입력

### Task 1: serialization 의존성과 versioned DTO

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupDtos.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupCodec.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/data/backup/BackupCodecTest.kt`

**Interfaces:**
- Produces: `BackupEnvelopeV1`, `BackupDataV1`, `BackupCodec.encode/decode`

- [ ] **Step 1: build claim과 의존성 추가**

root plugins:

```kotlin
id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
```

app plugins/dependencies:

```kotlin
id("org.jetbrains.kotlin.plugin.serialization")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
```

- [ ] **Step 2: codec 실패 테스트 작성**

```kotlin
@Test fun v1PayloadRoundTripsWithStableChecksum() {
    val encoded = codec.encode(sampleData())
    val decoded = codec.decode(encoded)
    assertThat(decoded.data).isEqualTo(sampleData())
    assertThat(decoded.manifest.checksumSha256).hasLength(64)
}

@Test fun changedPayloadFailsChecksum() {
    val bytes = codec.encode(sampleData()).copyOf().also { it[it.lastIndex - 3] = (it[it.lastIndex - 3].toInt() xor 1).toByte() }
    assertThrows<BackupFormatException> { codec.decode(bytes) }
}
```

- [ ] **Step 3: DTO 구현**

```kotlin
@Serializable data class BackupEnvelopeV1(val manifest: BackupManifestV1, val data: BackupDataV1)
@Serializable data class BackupManifestV1(
    val createdAt: String,
    val appVersion: String,
    val payloadSchemaVersion: Int = 1,
    val checksumSha256: String
)

@Serializable data class BackupDataV1(
    val transactions: List<TransactionBackupV1>,
    val reviewItems: List<ReviewItemBackupV1>,
    val rules: List<RuleBackupV1>,
    val assetAccounts: List<AssetAccountBackupV1>,
    val fixedExpenses: List<FixedExpenseBackupV1>,
    val monthlyPlans: List<MonthlyPlanBackupV1>,
    val userCategories: List<UserCategoryBackupV1>,
    val settings: PortableSettingsBackupV1
)

@Serializable data class TransactionBackupV1(
    val id: Long, val occurredAt: String, val amountWon: Long, val direction: String,
    val type: String, val category: String?, val paymentMethod: String?, val merchant: String?,
    val counterparty: String?, val memo: String?, val sourceApp: String?, val sourceType: String,
    val sourceNotificationHash: String?, val status: String, val confidence: Double,
    val monthKey: String, val linkedAssetAccountId: Long?, val balanceImpact: String?,
    val customCategoryId: Long?, val customCategoryName: String?, val settlementPartyCount: Int?,
    val settlementMyShareWon: Long?, val settlementParentId: Long?,
    val settlementTrackingHidden: Boolean, val budgetPlanId: Long?,
    val fixedExpensePlanId: Long?, val refundParentTransactionId: Long?
)

@Serializable data class ReviewItemBackupV1(
    val id: Long, val transactionId: Long, val reason: String,
    val createdAt: String, val resolvedAt: String?
)
@Serializable data class RuleBackupV1(
    val id: Long, val matchType: String, val matchValue: String,
    val action: String, val targetValue: String, val enabled: Boolean
)
@Serializable data class AssetAccountBackupV1(
    val id: Long, val name: String, val balanceWon: Long, val kind: String,
    val bankProvider: String?, val accountLast4: String?, val providerLabel: String?
)
@Serializable data class FixedExpenseBackupV1(
    val id: Long, val name: String, val amountWon: Long, val withdrawalDay: Int,
    val accountName: String, val accountId: Long?, val active: Boolean
)
@Serializable data class MonthlyPlanBackupV1(
    val id: Long, val label: String, val amountWon: Long, val type: String,
    val monthKey: String, val identityKey: String, val category: String?,
    val customCategoryId: Long?, val customCategoryName: String?
)
@Serializable data class UserCategoryBackupV1(
    val id: Long, val kind: String, val name: String,
    val normalizedName: String, val active: Boolean
)
@Serializable data class PortableSettingsBackupV1(
    val enabledExpenseCategories: Set<String>,
    val enabledIncomeCategories: Set<String>
)

class BackupFormatException(code: String, cause: Throwable? = null) : IllegalArgumentException(code, cause)
class UnsupportedBackupVersionException : IllegalArgumentException("UNSUPPORTED_BACKUP_VERSION")
```

- [ ] **Step 4: codec 구현**

`BackupCodec`는 먼저 `BackupDataV1` JSON bytes의 SHA-256을 계산해 manifest에 넣고 envelope JSON을 GZIP한다. decode는 GZIP limit 64 MiB, schema version 1, 재계산 checksum을 검증

```kotlin
class BackupCodec(private val json: Json = Json { encodeDefaults = true; explicitNulls = true }) {
    fun encode(data: BackupDataV1, createdAt: Instant, appVersion: String): ByteArray {
        val dataBytes = json.encodeToString(data).encodeToByteArray()
        val envelope = BackupEnvelopeV1(
            BackupManifestV1(createdAt.toString(), appVersion, 1, sha256Hex(dataBytes)),
            data
        )
        return gzip(json.encodeToString(envelope).encodeToByteArray())
    }

    fun decode(compressed: ByteArray): BackupEnvelopeV1 {
        val envelope = try {
            json.decodeFromString<BackupEnvelopeV1>(
                gunzipBounded(compressed, 64 * 1024 * 1024).decodeToString()
            )
        } catch (failure: Exception) {
            throw BackupFormatException("INVALID_PAYLOAD", failure)
        }
        if (envelope.manifest.payloadSchemaVersion != 1) throw UnsupportedBackupVersionException()
        val actual = sha256Hex(json.encodeToString(envelope.data).encodeToByteArray())
        if (actual != envelope.manifest.checksumSha256) throw BackupFormatException("CHECKSUM_MISMATCH")
        return envelope
    }
}
```

- [ ] **Step 5: 검증·커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*BackupCodecTest' :app:assembleDebug`

Expected: BUILD SUCCESSFUL

```bash
git add build.gradle.kts app/build.gradle.kts app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupDtos.kt app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupCodec.kt app/src/test/java/com/choiyoonseo/automoney/data/backup/BackupCodecTest.kt docs/AI_COLLABORATION.md
git commit -m "feat: add versioned backup codec"
```

완료 커밋에서 build claim 제거

### Task 2: portable password 암호화

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupCrypto.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/data/backup/BackupCryptoTest.kt`

**Interfaces:**
- Produces: `BackupCrypto.encrypt/decrypt`, `WrongBackupPasswordException`, `CorruptBackupException`

- [ ] **Step 1: crypto 실패 테스트 작성**

```kotlin
@Test fun encryptDecryptRoundTripUsesDifferentCiphertextEachTime() {
    val first = crypto.encrypt(DATA, PASSWORD)
    val second = crypto.encrypt(DATA, PASSWORD)
    assertThat(first).isNotEqualTo(second)
    assertThat(crypto.decrypt(first, PASSWORD)).isEqualTo(DATA)
}

@Test fun wrongPasswordAndTamperAreDistinct() {
    val encrypted = crypto.encrypt(DATA, PASSWORD)
    assertThrows<WrongBackupPasswordException> { crypto.decrypt(encrypted, "different password".toCharArray()) }
    val tampered = encrypted.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
    assertThrows<CorruptBackupException> { crypto.decrypt(tampered, PASSWORD) }
}
```

- [ ] **Step 2: file header와 암호 구현**

```kotlin
@Serializable data class BackupFileHeaderV1(
    val formatVersion: Int = 1,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 600_000,
    val saltBase64: String,
    val nonceBase64: String,
    val passwordVerifierBase64: String
)
```

binary layout: ASCII `AMK1` 4 bytes → big-endian header length 4 bytes → UTF-8 header JSON → header SHA-256 32 bytes → GCM ciphertext+tag. verifier는 derived key로 `HmacSHA256("AutoMoney backup password check v1")`. GCM AAD는 magic+length+header bytes+header hash 전체

```kotlin
private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
    require(password.size in 8..128)
    val spec = PBEKeySpec(password, salt, iterations, 256)
    return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
}
```

decrypt 순서: magic/header length 1..4096/header hash/format/KDF/iterations 범위 검증 → key derive → constant-time verifier 비교 → GCM decrypt. header hash 불일치와 verifier 일치 후 tag 실패는 corrupt, verifier만 불일치하면 wrong password

- [ ] **Step 3: 성능·검증·커밋**

instrumented benchmark가 기준 Galaxy 실기기에서 derive 2초 미만인지 기록. 2초 이상이면 iteration을 낮추지 말고 비동기 progress UI를 유지

Run: `./gradlew :app:testDebugUnitTest --tests '*BackupCryptoTest' :app:assembleDebug`

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupCrypto.kt app/src/test/java/com/choiyoonseo/automoney/data/backup/BackupCryptoTest.kt
git commit -m "feat: encrypt portable backup files"
```

### Task 3: DB export·validation·전체 교체

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/local/dao/BackupDao.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/RoomBackupStore.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupValidator.kt`
- Create: `app/src/androidTest/java/com/choiyoonseo/automoney/data/backup/RoomBackupStoreTest.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/data/backup/BackupValidatorTest.kt`

**Interfaces:**
- Produces: `RoomBackupStore.snapshot`, `replaceAll`, `BackupValidator.validate`

- [ ] **Step 1: validator 실패 테스트 작성**

```kotlin
@Test fun rejectsBrokenReferencesNegativeMoneyAndUnknownEnums() {
    assertThrows<BackupValidationException> { validator.validate(data(refundParentId = 999)) }
    assertThrows<BackupValidationException> { validator.validate(data(amountWon = -1)) }
    assertThrows<BackupValidationException> { validator.validate(data(transactionType = "FUTURE")) }
}
```

- [ ] **Step 2: BackupDao 구현**

포함 table 7개의 `SELECT * ORDER BY id`, list `@Insert(ABORT)`, delete query를 선언. `notification_history`는 export하지 않고 전체 복원 시 clear만 한다. replace delete 순서는 notification_history → review_items → transactions → fixed_expenses → monthly_plan_items → rules → user_categories → asset_accounts, insert는 부모가 먼저인 역순 관계로 asset_accounts → user_categories → rules → monthly_plan_items → fixed_expenses → transactions → review_items

```kotlin
@Dao interface BackupDao {
    @Query("SELECT * FROM transactions ORDER BY id") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM review_items ORDER BY id") suspend fun reviewItems(): List<ReviewItemEntity>
    @Query("SELECT * FROM rules ORDER BY id") suspend fun rules(): List<RuleEntity>
    @Query("SELECT * FROM asset_accounts ORDER BY id") suspend fun assetAccounts(): List<AssetAccountEntity>
    @Query("SELECT * FROM fixed_expenses ORDER BY id") suspend fun fixedExpenses(): List<FixedExpenseEntity>
    @Query("SELECT * FROM monthly_plan_items ORDER BY id") suspend fun monthlyPlans(): List<MonthlyPlanItemEntity>
    @Query("SELECT * FROM user_categories ORDER BY id") suspend fun userCategories(): List<UserCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTransactions(rows: List<TransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertReviewItems(rows: List<ReviewItemEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRules(rows: List<RuleEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAssetAccounts(rows: List<AssetAccountEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertFixedExpenses(rows: List<FixedExpenseEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMonthlyPlans(rows: List<MonthlyPlanItemEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertUserCategories(rows: List<UserCategoryEntity>)

    @Query("DELETE FROM notification_history") suspend fun deleteNotificationHistory()
    @Query("DELETE FROM review_items") suspend fun deleteReviewItems()
    @Query("DELETE FROM transactions") suspend fun deleteTransactions()
    @Query("DELETE FROM fixed_expenses") suspend fun deleteFixedExpenses()
    @Query("DELETE FROM monthly_plan_items") suspend fun deleteMonthlyPlans()
    @Query("DELETE FROM rules") suspend fun deleteRules()
    @Query("DELETE FROM user_categories") suspend fun deleteUserCategories()
    @Query("DELETE FROM asset_accounts") suspend fun deleteAssetAccounts()
}
```

- [ ] **Step 3: validator와 store 구현**

validator는 모든 ID 양수·unique, 금액 0 이상, Instant/YearMonth parse, enum `valueOf`, confidence 0..1, withdrawalDay 1..31, review transaction FK, fixed account FK, settlement/refund parent FK, plan/category 참조를 검사

```kotlin
suspend fun replaceAll(data: BackupDataV1) = db.withTransaction {
    validator.validate(data)
    backupDao.deleteNotificationHistory()
    backupDao.deleteReviewItems()
    backupDao.deleteTransactions()
    backupDao.deleteFixedExpenses()
    backupDao.deleteMonthlyPlans()
    backupDao.deleteRules()
    backupDao.deleteUserCategories()
    backupDao.deleteAssetAccounts()
    backupDao.insertAssetAccounts(data.assetAccounts.map(mapper::accountEntity))
    backupDao.insertUserCategories(data.userCategories.map(mapper::categoryEntity))
    backupDao.insertRules(data.rules.map(mapper::ruleEntity))
    backupDao.insertMonthlyPlans(data.monthlyPlans.map(mapper::planEntity))
    backupDao.insertFixedExpenses(data.fixedExpenses.map(mapper::fixedEntity))
    backupDao.insertTransactions(data.transactions.map(mapper::transactionEntity))
    backupDao.insertReviewItems(data.reviewItems.map(mapper::reviewEntity))
}
```

snapshot signature를 하나로 고정:

```kotlin
suspend fun snapshot(settings: PortableSettingsBackupV1): BackupDataV1 =
    db.withTransaction {
        BackupDataV1(
            transactions = backupDao.transactions().map(mapper::transactionDto),
            reviewItems = backupDao.reviewItems().map(mapper::reviewDto),
            rules = backupDao.rules().map(mapper::ruleDto),
            assetAccounts = backupDao.assetAccounts().map(mapper::accountDto),
            fixedExpenses = backupDao.fixedExpenses().map(mapper::fixedDto),
            monthlyPlans = backupDao.monthlyPlans().map(mapper::planDto),
            userCategories = backupDao.userCategories().map(mapper::categoryDto),
            settings = settings
        )
    }
```

- [ ] **Step 4: 검증·커밋**

Run: `./gradlew :app:testDebugUnitTest --tests '*BackupValidatorTest' :app:compileDebugAndroidTestKotlin :app:assembleDebug`

기기 연결 시 Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.backup.RoomBackupStoreTest`

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/local/dao/BackupDao.kt app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt app/src/main/java/com/choiyoonseo/automoney/data/backup/RoomBackupStore.kt app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupValidator.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/backup/RoomBackupStoreTest.kt app/src/test/java/com/choiyoonseo/automoney/data/backup/BackupValidatorTest.kt
git commit -m "feat: export and replace portable app data"
```

### Task 4: portable settings·rollback journal

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/PortableSettingsStore.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/KeystoreSnapshotCrypto.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/RestoreJournal.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/RestoreCoordinator.kt`
- Create: `app/src/androidTest/java/com/choiyoonseo/automoney/data/backup/RestoreCoordinatorTest.kt`

**Interfaces:**
- Produces: `RestoreCoordinator.restore`, `recoverIfNeeded`

- [ ] **Step 1: process-death 실패 테스트 작성**

```kotlin
@Test fun preparedJournalRestoresOldDatabaseAndSettings() = runTest {
    coordinator.prepareRollbackForTest(oldSnapshot())
    store.replaceAll(newData())
    settings.replace(newSettings())
    recreatedCoordinator.recoverIfNeeded()
    assertThat(store.snapshot(settings.snapshot())).isEqualTo(oldData())
    assertThat(settings.snapshot()).isEqualTo(oldData().settings)
}
```

- [ ] **Step 2: settings allowlist 구현**

`category_preferences`의 `enabled_expense_categories`, `enabled_income_categories` 두 key만 snapshot/replace. replace는 `SharedPreferences.Editor.clear()`를 쓰지 않고 `putStringSet(KEY_EXPENSE, expense).putStringSet(KEY_INCOME, income).commit()`하며 실패 시 예외

- [ ] **Step 3: Keystore snapshot과 journal 구현**

alias `automoney_restore_rollback_v1`, AES/GCM/NoPadding 256-bit key. 내부 파일 `filesDir/restore/rollback-v1.bin`은 nonce 12 bytes + ciphertext. journal preference는 `NONE` 또는 `PREPARED`, snapshot checksum을 synchronous commit

```kotlin
suspend fun restore(envelope: BackupEnvelopeV1) = mutex.withLock {
    validator.validate(envelope.data)
    val rollback = store.snapshot(settings.snapshot())
    val rollbackBytes = codec.encode(rollback, Instant.now(), "internal-rollback")
    snapshotFile.write(keystoreCrypto.encrypt(rollbackBytes))
    journal.markPrepared(sha256Hex(snapshotFile.read()))
    try {
        store.replaceAll(envelope.data)
        settings.replace(envelope.data.settings)
        journal.clear()
        snapshotFile.delete()
    } catch (failure: Throwable) {
        rollbackPreparedSnapshot()
        throw RestoreFailedException(failure)
    }
}
```

`recoverIfNeeded()`는 PREPARED일 때 파일 checksum을 확인하고 `codec.decode(keystoreCrypto.decrypt(snapshotFile.read())).data`를 검증해 DB와 settings를 이전 snapshot으로 교체한 뒤 journal/file 삭제

- [ ] **Step 4: 검증·커밋**

Run: `./gradlew :app:compileDebugAndroidTestKotlin :app:assembleDebug`

기기 연결 시 RestoreCoordinatorTest 실행. 기기 없으면 컴파일만 확인으로 보고

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/backup/PortableSettingsStore.kt app/src/main/java/com/choiyoonseo/automoney/data/backup/KeystoreSnapshotCrypto.kt app/src/main/java/com/choiyoonseo/automoney/data/backup/RestoreJournal.kt app/src/main/java/com/choiyoonseo/automoney/data/backup/RestoreCoordinator.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/backup/RestoreCoordinatorTest.kt
git commit -m "feat: rollback interrupted full restores"
```

### Task 5: stream service·startup recovery·Claude UI

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupService.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/AutoMoneyApplication.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt`
- Create: `app/src/androidTest/java/com/choiyoonseo/automoney/data/backup/BackupServiceTest.kt`

**Interfaces:**
- Produces: `BackupService.writeBackup(output,password)`, `readAndRestore(input,password)`

- [ ] **Step 1: bounded stream test 작성**

```kotlin
@Test fun writesAndRestoresThroughStreams() = runTest {
    val output = ByteArrayOutputStream()
    service.writeBackup(output, PASSWORD)
    store.replaceAll(emptyData())
    service.readAndRestore(ByteArrayInputStream(output.toByteArray()), PASSWORD)
    assertThat(store.snapshot(settings.snapshot()).transactions).isNotEmpty()
}
```

- [ ] **Step 2: service 구현**

```kotlin
class BackupService(
    private val store: RoomBackupStore,
    private val settings: PortableSettingsStore,
    private val codec: BackupCodec,
    private val crypto: BackupCrypto,
    private val restore: RestoreCoordinator,
    private val appVersion: String
) {
    suspend fun writeBackup(output: OutputStream, password: CharArray) = withContext(Dispatchers.IO) {
        val data = store.snapshot(settings.snapshot())
        output.use { it.write(crypto.encrypt(codec.encode(data, Instant.now(), appVersion), password)) }
    }

    suspend fun readAndRestore(input: InputStream, password: CharArray) = withContext(Dispatchers.IO) {
        val encrypted = input.use { it.readBytesBounded(64 * 1024 * 1024) }
        restore.restore(codec.decode(crypto.decrypt(encrypted, password)))
    }
}
```

같은 파일에 bounded reader를 정의:

```kotlin
internal fun InputStream.readBytesBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw BackupFormatException("FILE_TOO_LARGE")
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
```

- [ ] **Step 3: startup recovery와 DI**

`AppContainer`가 service/coordinator를 노출. `AutoMoneyApplication.onCreate()`는 journal이 PREPARED일 때만 `runBlocking(Dispatchers.IO) { restoreCoordinator.recoverIfNeeded() }`를 실행하고 정상 시작 경로에서는 blocking DB 접근 없음

- [ ] **Step 4: Claude SAF UI**

`CreateDocument("application/octet-stream")` 기본 파일명 `AutoMoney-YYYY-MM-DD.amk`, `OpenDocument()` MIME `application/octet-stream`. 백업은 비밀번호·확인 두 필드, 복원은 비밀번호 한 필드와 `현재 데이터를 전체 교체` 확인 dialog. 5회 실패 시 해당 화면 인메모리 상태로 5초 입력 지연

- [ ] **Step 5: 전체 검증·커밋**

Run: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug`

실기기 수동 점검: 백업 → 거래 추가 → 전체 복원 → 추가 거래 사라짐, 원본 관계·계획·분류 복구, 알림 이력은 유지되지 않고 현재 DB에서 비워짐

```bash
git add app/src/main/java/com/choiyoonseo/automoney/data/backup/BackupService.kt app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt app/src/main/java/com/choiyoonseo/automoney/AutoMoneyApplication.kt app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt app/src/main/java/com/choiyoonseo/automoney/ui/AppRoot.kt app/src/main/java/com/choiyoonseo/automoney/MainActivity.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/backup/BackupServiceTest.kt docs/AI_COLLABORATION.md
git commit -m "feat: add encrypted backup and full restore"
```

완료 커밋에서 모든 shared claim 제거

## 이어받기 바통

Codex가 tasks 1–4와 Task 5 service·startup contract를 push한 뒤 Claude가 SAF UI를 구현. 암호 primitive·format·복원 순서는 UI 단계에서 변경 금지
