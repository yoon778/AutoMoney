# Bank Notification Balance Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Match high-confidence bank account notifications to registered asset accounts and update balances exactly once while ambiguous transfers remain in Review.

**Architecture:** Parsers emit temporary account hints. `RoomMoneyRepository` resolves each hint against current accounts inside the same Room transaction that inserts the transaction and applies an explicit balance effect. New records use stable account IDs and `CREDIT`/`DEBIT`/`NONE`; migrated records keep the existing name-based behavior until edited.

**Tech Stack:** Kotlin, Android `NotificationListenerService`, Jetpack Compose Material3, Room 2.8.4, Coroutines, JUnit 4, Truth, Gradle, ADB.

## Global Constraints

- Workspace: `C:/Users/cys04/Desktop/AutoMoney`, branch `codex/app-logic`
- Keep `android:allowBackup="false"` unchanged
- Store only bank provider and account-number last four digits
- Never persist or log a full account number or unmasked account-like notification text
- Auto-apply only with exact package, explicit direction, one transaction amount, one suffix, and one matching account
- Credit-card approval balance effect remains `NONE`
- Transfer classification remains in Review even when its factual balance effect is applied
- Never infer TossBank from the Toss package alone
- Room migration is exactly 5→6
- `balanceImpact == null` means legacy behavior only
- TDD and one focused commit per task
- Before every commit, inspect `git status --short` and stage only files owned by that task
- Synthetic fixtures never count as real-device support

## Verified Package IDs

- KB: `com.kbstar.kbbank`
- Shinhan current/legacy: `com.shinhan.sbanking`, `com.shinhan.smartcaremgr`
- Hana current/legacy: `com.hanabank.oqf`, `com.kebhana.hanapush`
- Woori: `com.wooribank.smart.npib`
- NH: `nh.smart.banking`
- IBK: `com.ibk.android.ionebank`
- KakaoBank: `com.kakaobank.channel`
- Toss aggregator: `viva.republica.toss`

Verified 2026-07-10 from the matching Google Play `details?id=...` pages.

## File Structure

Create focused units:

- `domain/assets/BankAccountMetadata.kt`: provider, effect, suffix normalization
- `domain/assets/BankAccountHint.kt`: temporary parsed movement
- `domain/assets/AssetAccountMatcher.kt`: exact unique matching
- `domain/assets/AccountBalanceDecision.kt`: match-to-effect policy
- `domain/parser/NotificationIdentity.kt`: stable source hash
- `domain/parser/BankAccountHintExtractor.kt`: strict movement extraction
- `ui/assets/AssetAccountForm.kt`: full-input-to-suffix conversion
- Matching tests and `docs/testing/bank-notification-balance-sync.md`

Modify existing models, parsers, listener/ingestion, Room, repositories, DI, asset UI, manual/edit UI, Home, Transactions, Review, and their tests.

---

### Task 1: Stable Notification Identity

**Files:**

- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationIdentity.kt`
- Modify: `NotificationModels.kt`, `NotificationSnapshotBuilder.kt`, `MoneyNotificationListenerService.kt`
- Modify: `CommonFinanceNotificationParser.kt`, `TossNotificationParser.kt`
- Test: `NotificationIdentityTest.kt`, `NotificationSnapshotBuilderTest.kt`

**Interfaces:**

- `notificationIdentityHash(snapshot): String`
- `NotificationSnapshot.notificationKey`
- `NotificationSnapshot.sourceNotificationHash`

- [ ] **Step 1: Write failing tests**

~~~kotlin
@Test
fun sameAndroidNotificationKeepsHashWhenTextChanges() {
    val first = snapshot(key = "bank-key", text = "10,000원 출금")
    val updated = snapshot(key = "bank-key", text = "10,000원 출금 완료")
    assertThat(first.sourceNotificationHash).isEqualTo(updated.sourceNotificationHash)
}

@Test
fun repeatedTextAtAnotherPostTimeIsNotDuplicate() {
    val first = snapshot(key = null, text = "10,000원 출금", postTime = 1_000)
    val second = snapshot(key = null, text = "10,000원 출금", postTime = 2_000)
    assertThat(first.sourceNotificationHash).isNotEqualTo(second.sourceNotificationHash)
}

private fun snapshot(
    key: String?,
    text: String,
    postTime: Long = 1_000
) = NotificationSnapshot(
    packageName = "test.bank",
    title = "은행",
    text = text,
    bigText = null,
    postedAt = Instant.ofEpochMilli(postTime),
    notificationKey = key
)
~~~

Add builder assertion:

~~~kotlin
assertThat(snapshot.notificationKey).isEqualTo("bank-key")
~~~

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*NotificationIdentityTest" --tests "*NotificationSnapshotBuilderTest"
~~~

Expected: missing identity fields.

- [ ] **Step 3: Implement central hash**

~~~kotlin
fun notificationIdentityHash(snapshot: NotificationSnapshot): String {
    val identity = snapshot.notificationKey
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { key ->
            listOf(snapshot.packageName, key, snapshot.postedAt.toEpochMilli().toString())
                .joinToString("|")
        }
        ?: listOf(
            snapshot.packageName,
            snapshot.postedAt.toEpochMilli().toString(),
            sha256(snapshot.combinedText)
        ).joinToString("|")
    return sha256(identity)
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
~~~

Add `notificationKey: String? = null` and computed `sourceNotificationHash` to `NotificationSnapshot`. Thread `StatusBarNotification.key` through `NotificationContentFields` and the builder by passing `sbn.key`. Replace both parser-local hashes with `snapshot.sourceNotificationHash` and remove local hash functions.

- [ ] **Step 4: Verify GREEN**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*NotificationIdentityTest" --tests "*NotificationSnapshotBuilderTest" --tests "*TossNotificationParserTest" --tests "*CommonFinanceNotificationParserTest"
~~~

Expected: PASS.

- [ ] **Step 5: Commit**

~~~powershell
git add app/src/main app/src/test
git commit -m "fix: use stable notification identity"
~~~

---

### Task 2: Domain Fields And Room 5→6 Migration

**Files:**

- Create: `BankAccountMetadata.kt` and `BankAccountMetadataTest.kt`
- Modify: `AssetModels.kt`, `MoneyModels.kt`, `Entities.kt`, `AppDatabase.kt`
- Modify: `RoomAssetRepository.kt`, `RoomMoneyRepository.kt`, `AppContainer.kt`
- Modify: `AppDatabaseMigrationTest.kt`, `DatabaseIntegritySchemaTest.kt`
- Generate: `app/schemas/com.choiyoonseo.automoney.data.local.AppDatabase/6.json`

**Interfaces:** `BankProvider`, `BalanceImpact`, `normalizeAccountLast4`, `maskedAccountLast4`.

- [ ] **Step 1: Write failing tests**

~~~kotlin
@Test
fun keepsOnlyLastFourDigits() {
    assertThat(normalizeAccountLast4("123-456-789012")).isEqualTo("9012")
}

@Test
fun rejectsShortInput() {
    assertThrows(IllegalArgumentException::class.java) {
        normalizeAccountLast4("123")
    }
}

@Test
fun masksStoredSuffix() {
    assertThat(maskedAccountLast4("9012")).isEqualTo("****9012")
}
~~~

Update schema test expectations to version 6, `MIGRATION_5_6`, linked-account index, and `6.json`.

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*BankAccountMetadataTest" --tests "*DatabaseIntegritySchemaTest"
~~~

- [ ] **Step 3: Add domain types**

~~~kotlin
enum class BankProvider(val displayName: String, val badgeText: String) {
    KB("KB국민은행", "KB"),
    SHINHAN("신한은행", "신한"),
    HANA("하나은행", "하나"),
    WOORI("우리은행", "우리"),
    NH("NH농협은행", "NH"),
    IBK("IBK기업은행", "IBK"),
    KAKAO_BANK("카카오뱅크", "카카오"),
    TOSS_BANK("토스뱅크", "토스")
}

enum class BalanceImpact { CREDIT, DEBIT, NONE }

fun normalizeAccountLast4(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    require(digits.length >= 4) { "계좌번호는 숫자 4자리 이상 입력해 주세요." }
    return digits.takeLast(4)
}

fun maskedAccountLast4(last4: String?): String? =
    last4?.takeIf { it.length == 4 && it.all(Char::isDigit) }?.let { "****" + it }
~~~

Add nullable `bankProvider`/`accountLast4` defaults to `AssetAccount`. Add nullable `linkedAssetAccountId`/`balanceImpact` defaults to `MoneyTransaction`. Add review reasons `ACCOUNT_AMBIGUOUS`, `ACCOUNT_MOVEMENT_UNKNOWN`, `BALANCE_MISMATCH`.

- [ ] **Step 4: Add Room fields and migration**

Add `Index(value = ["linkedAssetAccountId"])` and:

~~~kotlin
val linkedAssetAccountId: Long?,
val balanceImpact: BalanceImpact?
~~~

Add to asset entity:

~~~kotlin
val bankProvider: BankProvider?,
val accountLast4: String?
~~~

Set version 6:

~~~kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE asset_accounts ADD COLUMN bankProvider TEXT")
        db.execSQL("ALTER TABLE asset_accounts ADD COLUMN accountLast4 TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN linkedAssetAccountId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN balanceImpact TEXT")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_transactions_linkedAssetAccountId " +
                "ON transactions(linkedAssetAccountId)"
        )
    }
}
~~~

Register it in `AppContainer` and map all four fields in both directions. Defaults avoid meaningless bulk `null` edits.

- [ ] **Step 5: Validate migration**

Target version 6 in `AppDatabaseMigrationTest`, include `MIGRATION_5_6`, insert one legacy account, and assert all new columns remain null for legacy rows.

~~~powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest --tests "*BankAccountMetadataTest" --tests "*DatabaseIntegritySchemaTest"
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.local.AppDatabaseMigrationTest
~~~

Expected: schema 6 generated; unit and device migration tests PASS.

- [ ] **Step 6: Commit**

~~~powershell
git add app/src/main app/src/test app/src/androidTest app/schemas
git commit -m "feat: persist bank account balance metadata"
~~~

---

### Task 3: Eight-Bank Financial App Registry

**Files:** `FinancialAppRegistry.kt`, `FinancialAppRegistryTest.kt`.

**Interfaces:** `FinancialAppInfo.bankProvider`, `aggregatesMultipleBanks`, `providerCandidateForPackage`.

- [ ] **Step 1: Write failing tests**

~~~kotlin
@Test
fun supportsCurrentMajorBankPackages() {
    val packages = listOf(
        "com.kbstar.kbbank",
        "com.shinhan.sbanking",
        "com.hanabank.oqf",
        "com.wooribank.smart.npib",
        "nh.smart.banking",
        "com.ibk.android.ionebank",
        "com.kakaobank.channel",
        "viva.republica.toss"
    )
    assertThat(packages.all(FinancialAppRegistry::isSupportedPackage)).isTrue()
}

@Test
fun tossDoesNotClaimTossBankProvider() {
    val info = FinancialAppRegistry.infoForPackage("viva.republica.toss")
    assertThat(info?.bankProvider).isNull()
    assertThat(info?.aggregatesMultipleBanks).isTrue()
}
~~~

Add assertions for every dedicated and legacy package/provider pair.

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*FinancialAppRegistryTest"
~~~

- [ ] **Step 3: Expand registry**

~~~kotlin
data class FinancialAppInfo(
    val packageName: String,
    val displayName: String,
    val badgeText: String,
    val bankProvider: BankProvider?,
    val aggregatesMultipleBanks: Boolean = false
)

fun providerCandidateForPackage(packageName: String): BankProvider? =
    appInfoByPackage[packageName]?.bankProvider
~~~

Register all verified IDs. Dedicated packages get a provider. Toss gets `bankProvider = null` and `aggregatesMultipleBanks = true`.

- [ ] **Step 4: Verify and commit**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*FinancialAppRegistryTest" --tests "*MoneyNotificationListenerServiceTest"
git add app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt app/src/test/java/com/choiyoonseo/automoney/notification/FinancialAppRegistryTest.kt
git commit -m "feat: register major bank notification apps"
~~~

---

### Task 4: Conservative Bank Account Hint Parsing

**Files:**

- Create: `BankAccountHint.kt`, `BankAccountHintExtractor.kt`, extractor test
- Modify: `NotificationModels.kt`, both parsers, `AppContainer.kt`, parser tests

**Interfaces:** `BankAccountHint`, `ParsedBankMovement`, `AccountMovementDirection`, `BankEventKind`, `TransactionDraft.bankAccountHint`.

- [ ] **Step 1: Write failing tests**

~~~kotlin
private val extractor = BankAccountHintExtractor()

@Test
fun extractsUniqueWithdrawalHint() {
    val movement = extractor.extract(
        provider = BankProvider.KB,
        text = "계좌 123-***-4567\n10,000원 출금"
    )
    assertThat(movement?.amountWon).isEqualTo(10_000)
    assertThat(movement?.hint).isEqualTo(
        BankAccountHint(
            BankProvider.KB,
            "4567",
            AccountMovementDirection.DEBIT,
            BankEventKind.WITHDRAWAL
        )
    )
}

@Test
fun rejectsMovementLineWithTwoAmounts() {
    assertThat(
        extractor.extract(
            BankProvider.KB,
            "계좌 123-***-4567 10,000원 출금 잔액 90,000원"
        )
    ).isNull()
}

@Test
fun aggregatorRequiresExplicitBankName() {
    assertThat(extractor.resolveAggregatorProvider("10,000원 송금했어요")).isNull()
    assertThat(extractor.resolveAggregatorProvider("토스뱅크 계좌 123-***-4567 10,000원 출금"))
        .isEqualTo(BankProvider.TOSS_BANK)
}
~~~

Add a loop over all dedicated package/provider mappings.

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*BankAccountHintExtractorTest" --tests "*CommonFinanceNotificationParserTest" --tests "*TossNotificationParserTest"
~~~

- [ ] **Step 3: Add hint models**

~~~kotlin
enum class AccountMovementDirection { CREDIT, DEBIT, UNKNOWN }
enum class BankEventKind { DEPOSIT, WITHDRAWAL, TRANSFER }

data class BankAccountHint(
    val provider: BankProvider,
    val accountLast4: String,
    val direction: AccountMovementDirection,
    val eventKind: BankEventKind
)

data class ParsedBankMovement(
    val amountWon: Long,
    val hint: BankAccountHint
)
~~~

Add `bankAccountHint: BankAccountHint? = null` to `TransactionDraft`.

- [ ] **Step 4: Implement strict extractor**

~~~kotlin
class BankAccountHintExtractor {
    fun extract(provider: BankProvider, text: String): ParsedBankMovement? {
        val lines = text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filter(::containsMovementKeyword)
            .toList()
        if (lines.size != 1) return null
        val line = lines.single()
        val amountMatch = amountRegex.findAll(line).singleOrNull() ?: return null
        val amountWon = amountMatch.groupValues[1].replace(",", "").toLong()

        val accountText = amountRegex.replace(text, " ")
        val token = labeledAccountRegex.find(accountText)?.groupValues?.get(1)
            ?: accountTokenRegex.findAll(accountText)
                .map(MatchResult::value)
                .filter { value -> value.contains('-') || value.contains('*') }
                .singleOrNull()
            ?: return null
        val digits = token.filter(Char::isDigit)
        if (digits.length < 4) return null

        val kind = when {
            line.contains("송금") || line.contains("이체") -> BankEventKind.TRANSFER
            line.contains("입금") || line.contains("받았") -> BankEventKind.DEPOSIT
            else -> BankEventKind.WITHDRAWAL
        }
        val direction = when {
            line.contains("입금") || line.contains("받았") || line.contains("받음") ->
                AccountMovementDirection.CREDIT
            line.contains("출금") || line.contains("보냈") || line.contains("송금했") ||
                line.contains("이체완료") -> AccountMovementDirection.DEBIT
            else -> AccountMovementDirection.UNKNOWN
        }
        return ParsedBankMovement(
            amountWon = amountWon,
            hint = BankAccountHint(provider, digits.takeLast(4), direction, kind)
        )
    }

    fun resolveAggregatorProvider(text: String): BankProvider? =
        BankProvider.entries.singleOrNull { provider ->
            text.contains(provider.displayName, ignoreCase = true)
        }

    private fun containsMovementKeyword(value: String): Boolean =
        listOf("입금", "출금", "송금", "이체", "자동이체", "ATM", "받았", "보냈")
            .any { keyword -> value.contains(keyword, ignoreCase = true) }

    private val amountRegex = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
    private val labeledAccountRegex =
        Regex("""(?:계좌|통장)\s*[:：]?\s*([0-9*Xx-]{4,})""")
    private val accountTokenRegex = Regex("""(?<![\d,])[0-9*Xx-]{7,}(?![\d,])""")
}
~~~

- [ ] **Step 5: Integrate parsers**

Expand the common parser from KB-only to all dedicated bank packages:

~~~kotlin
override fun canParse(snapshot: NotificationSnapshot): Boolean =
    FinancialAppRegistry.infoForPackage(snapshot.packageName)?.bankProvider != null
~~~

Dedicated packages use `providerCandidateForPackage`. Toss resolves provider from explicit text only. Movement drafts use `MoneyAmount(candidate.amountWon)` from the extractor, never the first amount from the full notification. Attach `candidate.hint` only to movement drafts; card approval, top-up, and card refund remain hintless.

Map semantics exactly:

- `DEPOSIT + CREDIT` → `INCOME`, `INCOME` direction, `INCOME_UNKNOWN` review
- `TRANSFER + CREDIT/DEBIT` → `TRANSFER`, `NEUTRAL` direction, `TRANSFER_UNKNOWN` review
- `WITHDRAWAL + DEBIT` → `EXPENSE`, `EXPENSE` direction, `ACCOUNT_MOVEMENT_UNKNOWN` review
- `UNKNOWN` direction → `ACCOUNT_MOVEMENT_UNKNOWN` review with no balance effect

All account movements remain in Review. Use `snapshot.sourceNotificationHash`. Wire one shared extractor in `AppContainer`.

- [ ] **Step 6: Verify and commit**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*BankAccountHintExtractorTest" --tests "*CommonFinanceNotificationParserTest" --tests "*TossNotificationParserTest" --tests "*NotificationParserRouterTest"
git add app/src/main/java/com/choiyoonseo/automoney/domain app/src/main/java/com/choiyoonseo/automoney/di app/src/test/java/com/choiyoonseo/automoney/domain
git commit -m "feat: parse bank account movement hints"
~~~

---

### Task 5: Account Matching And Balance Decision

**Files:** Create `AssetAccountMatcher.kt`, `AccountBalanceDecision.kt`, and their tests.

**Interfaces:** `AssetAccountMatch`, `AccountBalanceDecision`, `decideAccountBalance`.

- [ ] **Step 1: Write failing tests**

~~~kotlin
@Test
fun duplicateSuffixInSameBankIsAmbiguous() {
    val match = matchAssetAccount(
        listOf(account(1, BankProvider.KB, "4567"), account(2, BankProvider.KB, "4567")),
        hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT)
    )
    assertThat(match).isEqualTo(AssetAccountMatch.Ambiguous)
}

@Test
fun debitOverBalanceMovesToReviewWithoutEffect() {
    val decision = decideAccountBalance(
        listOf(account(1, BankProvider.KB, "4567", 5_000)),
        hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT),
        10_000
    )
    assertThat(decision.balanceImpact).isEqualTo(BalanceImpact.NONE)
    assertThat(decision.reviewReason).isEqualTo(ReviewReason.BALANCE_MISMATCH)
}
~~~

Also test missing, exact, wrong bank, credit, debit, and unknown direction.

Use these local factories:

~~~kotlin
private fun account(
    id: Long,
    provider: BankProvider,
    last4: String,
    balanceWon: Long = 100_000
) = AssetAccount(
    id = id,
    name = provider.displayName,
    balanceWon = balanceWon,
    kind = AssetAccountKind.BANK,
    bankProvider = provider,
    accountLast4 = last4
)

private fun hint(
    provider: BankProvider,
    last4: String,
    direction: AccountMovementDirection
) = BankAccountHint(
    provider = provider,
    accountLast4 = last4,
    direction = direction,
    eventKind = BankEventKind.TRANSFER
)
~~~

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*AssetAccountMatcherTest" --tests "*AccountBalanceDecisionTest"
~~~

- [ ] **Step 3: Implement matcher**

~~~kotlin
sealed interface AssetAccountMatch {
    data class Matched(val account: AssetAccount) : AssetAccountMatch
    data object Missing : AssetAccountMatch
    data object Ambiguous : AssetAccountMatch
}

fun matchAssetAccount(accounts: List<AssetAccount>, hint: BankAccountHint): AssetAccountMatch {
    val matches = accounts.filter { account ->
        account.kind == AssetAccountKind.BANK &&
            account.bankProvider == hint.provider &&
            account.accountLast4 == hint.accountLast4
    }
    return when (matches.size) {
        0 -> AssetAccountMatch.Missing
        1 -> AssetAccountMatch.Matched(matches.single())
        else -> AssetAccountMatch.Ambiguous
    }
}
~~~

- [ ] **Step 4: Implement decision**

~~~kotlin
data class AccountBalanceDecision(
    val linkedAssetAccountId: Long?,
    val balanceImpact: BalanceImpact,
    val reviewReason: ReviewReason?
)

fun decideAccountBalance(
    accounts: List<AssetAccount>,
    hint: BankAccountHint?,
    amountWon: Long
): AccountBalanceDecision {
    if (hint == null) return AccountBalanceDecision(null, BalanceImpact.NONE, null)
    return when (val match = matchAssetAccount(accounts, hint)) {
        AssetAccountMatch.Missing ->
            AccountBalanceDecision(null, BalanceImpact.NONE, ReviewReason.ACCOUNT_UNMATCHED)
        AssetAccountMatch.Ambiguous ->
            AccountBalanceDecision(null, BalanceImpact.NONE, ReviewReason.ACCOUNT_AMBIGUOUS)
        is AssetAccountMatch.Matched -> {
            val impact = when (hint.direction) {
                AccountMovementDirection.CREDIT -> BalanceImpact.CREDIT
                AccountMovementDirection.DEBIT -> BalanceImpact.DEBIT
                AccountMovementDirection.UNKNOWN -> BalanceImpact.NONE
            }
            val reason = when {
                impact == BalanceImpact.NONE -> ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN
                impact == BalanceImpact.DEBIT && match.account.balanceWon < amountWon ->
                    ReviewReason.BALANCE_MISMATCH
                else -> null
            }
            AccountBalanceDecision(
                match.account.id,
                if (reason == null) impact else BalanceImpact.NONE,
                reason
            )
        }
    }
}
~~~

- [ ] **Step 5: Verify and commit**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*AssetAccountMatcherTest" --tests "*AccountBalanceDecisionTest"
git add app/src/main/java/com/choiyoonseo/automoney/domain/assets app/src/test/java/com/choiyoonseo/automoney/domain/assets
git commit -m "feat: decide safe account balance effects"
~~~

---

### Task 6: Explicit Effects And Atomic Notification Save

**Files:**

- Modify: `AssetBalanceSync.kt`, `MoneyRepository.kt`, `RoomMoneyRepository.kt`
- Modify: `AssetBalanceSyncTest.kt`
- Modify: `RoomMoneyRepositoryReviewItemTest.kt`

**Interfaces:**

- `NotificationSaveResult(transactionId, reviewReason)`
- `MoneyRepository.saveNotificationTransaction(transaction, accountHint, reviewReason)`
- Explicit effects use account IDs; null effects retain legacy behavior

- [ ] **Step 1: Write failing balance tests**

~~~kotlin
@Test
fun explicitDebitUsesIdEvenDuringReview() {
    val accounts = listOf(
        AssetAccount(id = 1, name = "급여", balanceWon = 100_000),
        AssetAccount(id = 2, name = "생활비", balanceWon = 50_000)
    )
    val transaction = transaction(
        amountWon = 10_000,
        status = TransactionStatus.NEEDS_REVIEW
    ).copy(
        linkedAssetAccountId = 2,
        balanceImpact = BalanceImpact.DEBIT
    )

    val updated = applyTransactionBalance(accounts, transaction)

    assertThat(updated.map(AssetAccount::balanceWon))
        .containsExactly(100_000L, 40_000L).inOrder()
}

@Test
fun explicitNoneNeverFallsBackToNameMatching() {
    val accounts = listOf(AssetAccount(id = 1, name = "KB", balanceWon = 100_000))
    val transaction = transaction(paymentMethod = "KB")
        .copy(balanceImpact = BalanceImpact.NONE)
    assertThat(applyTransactionBalance(accounts, transaction)).isEqualTo(accounts)
}

@Test
fun replacingExplicitCreditValidatesOnlyFinalBalance() {
    val accounts = listOf(AssetAccount(id = 1, name = "생활비", balanceWon = 50))
    val old = transaction(amountWon = 100).copy(
        linkedAssetAccountId = 1,
        balanceImpact = BalanceImpact.CREDIT
    )
    val new = old.copy(amount = MoneyAmount(50))

    val updated = replaceTransactionBalance(accounts, old, new)

    assertThat(updated.single().balanceWon).isEqualTo(0)
}
~~~

Keep all current name-matching tests to prove legacy compatibility.

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*AssetBalanceSyncTest"
~~~

- [ ] **Step 3: Add explicit path before legacy path**

~~~kotlin
private fun applyBalanceEffect(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction,
    multiplier: Int
): List<AssetAccount> {
    val impact = transaction.balanceImpact
    if (impact != null) {
        return applyExplicitBalanceEffect(accounts, transaction, impact, multiplier)
    }
    return applyLegacyBalanceEffect(accounts, transaction, multiplier)
}

private fun applyExplicitBalanceEffect(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction,
    impact: BalanceImpact,
    multiplier: Int
): List<AssetAccount> {
    if (impact == BalanceImpact.NONE) return accounts
    val accountId = requireNotNull(transaction.linkedAssetAccountId) {
        "잔액 반영 계좌가 필요해요."
    }
    val index = accounts.indexOfFirst { account -> account.id == accountId }
    require(index >= 0) { "연결된 계좌를 찾을 수 없어요." }
    val sign = if (impact == BalanceImpact.CREDIT) 1 else -1
    val next = accounts[index].balanceWon + transaction.amount.won * sign * multiplier
    require(next >= 0) { "잔액 반영 후 금액이 0원보다 작아져요." }
    return accounts.mapIndexed { current, account ->
        if (current == index) account.copy(balanceWon = next) else account
    }
}
~~~

Move the current body unchanged into `applyLegacyBalanceEffect`.

When both old and new transactions have explicit effects, `replaceTransactionBalance` must apply their combined per-account delta and validate only final balances:

~~~kotlin
private fun replaceExplicitBalance(
    accounts: List<AssetAccount>,
    oldTransaction: MoneyTransaction,
    newTransaction: MoneyTransaction
): List<AssetAccount> {
    val deltas = mutableMapOf<Long, Long>()

    fun add(transaction: MoneyTransaction, multiplier: Int) {
        val impact = requireNotNull(transaction.balanceImpact)
        if (impact == BalanceImpact.NONE) return
        val accountId = requireNotNull(transaction.linkedAssetAccountId)
        val sign = if (impact == BalanceImpact.CREDIT) 1 else -1
        val delta = transaction.amount.won * sign * multiplier
        deltas[accountId] = deltas.getOrDefault(accountId, 0L) + delta
    }

    add(oldTransaction, -1)
    add(newTransaction, 1)
    require(deltas.keys.all { id -> accounts.any { account -> account.id == id } }) {
        "연결된 계좌를 찾을 수 없어요."
    }
    return accounts.map { account ->
        val next = account.balanceWon + deltas.getOrDefault(account.id, 0L)
        require(next >= 0) { "잔액 반영 후 금액이 0원보다 작아져요." }
        account.copy(balanceWon = next)
    }
}
~~~

Use this branch only when both effects are non-null; mixed legacy/explicit replacement keeps the existing reverse-then-apply compatibility path.

- [ ] **Step 4: Add atomic repository contract**

~~~kotlin
data class NotificationSaveResult(
    val transactionId: Long,
    val reviewReason: ReviewReason?
)

suspend fun saveNotificationTransaction(
    transaction: MoneyTransaction,
    accountHint: BankAccountHint?,
    reviewReason: ReviewReason?
): NotificationSaveResult {
    val explicitNone = transaction.copy(
        linkedAssetAccountId = null,
        balanceImpact = BalanceImpact.NONE
    )
    val id = if (reviewReason == null) {
        saveTransaction(explicitNone)
    } else {
        saveTransactionWithReview(explicitNone, reviewReason)
    }
    return NotificationSaveResult(id, reviewReason)
}
~~~

This default keeps existing fake repositories source-compatible. The Room override performs one `db.withTransaction`:

1. Load current accounts
2. Call `decideAccountBalance`
3. Prefer its account error over the parser reason
4. Set linked ID, explicit effect, and effective status
5. Insert transaction
6. Apply effect
7. Insert exactly one review item

Core transformation:

~~~kotlin
val decision = decideAccountBalance(currentAccounts, accountHint, transaction.amount.won)
val effectiveReason = decision.reviewReason ?: reviewReason
val transactionToSave = transaction.copy(
    linkedAssetAccountId = decision.linkedAssetAccountId,
    balanceImpact = decision.balanceImpact,
    status = if (effectiveReason != null) {
        TransactionStatus.NEEDS_REVIEW
    } else {
        transaction.status
    }
)
~~~

Return `NotificationSaveResult(id, effectiveReason)` and preserve unique-index exception conversion.

- [ ] **Step 5: Restrict old account review**

The current payment-method review path runs only when `balanceImpact == null`. Explicit `NONE` means intentionally balance-neutral, so card approvals do not become `ACCOUNT_UNMATCHED`.

~~~kotlin
private fun needsLegacyAccountReview(
    accounts: List<AssetAccount>,
    transaction: MoneyTransaction
): Boolean =
    transaction.balanceImpact == null &&
        transaction.sourceType == SourceType.NOTIFICATION &&
        needsAccountMatchReview(accounts, transaction.asResolvedForAccountReview())
~~~

- [ ] **Step 6: Add Room integration cases**

Prove:

- Matched review transfer applies debit once
- Missing suffix stores `NONE` plus `ACCOUNT_UNMATCHED`
- Duplicate suffix stores `NONE` plus `ACCOUNT_AMBIGUOUS`
- Hintless card stores `NONE` without account review
- Duplicate insert rolls back transaction and balance
- Explicit update/delete reverse and replace effects

- [ ] **Step 7: Verify and commit**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*AssetBalanceSyncTest"
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.repository.RoomMoneyRepositoryReviewItemTest
git add app/src/main/java/com/choiyoonseo/automoney/domain/assets/AssetBalanceSync.kt app/src/main/java/com/choiyoonseo/automoney/data/repository app/src/test/java/com/choiyoonseo/automoney/domain/assets/AssetBalanceSyncTest.kt app/src/androidTest/java/com/choiyoonseo/automoney/data/repository
git commit -m "feat: apply notification balances atomically"
~~~

---

### Task 7: Ingestion And Review Semantics

**Files:**

- Modify: `NotificationIngestionUseCase.kt`
- Modify: `ReviewResolution.kt`, `ResolveAccountTransferUseCase.kt`
- Modify: `EditTransactionUseCase.kt`
- Modify their existing unit tests

**Guarantees:** effective repository reason is returned; settlement/exclusion retain factual effects; own-account pairing never double-applies.

- [ ] **Step 1: Write failing ingestion test**

~~~kotlin
@Test
fun ingestionReturnsRepositoryEffectiveReason() = runTest {
    val repository = RecordingMoneyRepository(
        notificationResult = NotificationSaveResult(
            transactionId = 1,
            reviewReason = ReviewReason.ACCOUNT_AMBIGUOUS
        )
    )
    val result = useCase(repository, expenseDraft()).ingest(snapshot())
    assertThat(result).isEqualTo(
        IngestionResult.Saved(
            TransactionType.EXPENSE,
            ReviewReason.ACCOUNT_AMBIGUOUS
        )
    )
}

private fun useCase(
    repository: MoneyRepository,
    draft: TransactionDraft
) = NotificationIngestionUseCase(
    parser = StaticParser(draft),
    categorizationEngine = CategorizationEngine(),
    duplicateDetector = DuplicateDetector(),
    repository = repository
)
~~~

Update the fake to record the account hint and implement `saveNotificationTransaction`.

- [ ] **Step 2: Write failing review tests**

~~~kotlin
@Test
fun settlementKeepsAppliedCredit() {
    val resolved = transaction().copy(
        linkedAssetAccountId = 7,
        balanceImpact = BalanceImpact.CREDIT
    ).resolveReview(ReviewResolution.SETTLEMENT, null)

    assertThat(resolved.linkedAssetAccountId).isEqualTo(7)
    assertThat(resolved.balanceImpact).isEqualTo(BalanceImpact.CREDIT)
}

@Test
fun statisticsExcludeKeepsAppliedDebit() {
    val resolved = transaction().copy(
        linkedAssetAccountId = 7,
        balanceImpact = BalanceImpact.DEBIT
    ).resolveReview(ReviewResolution.EXCLUDE, null)

    assertThat(resolved.type).isEqualTo(TransactionType.EXCLUDED)
    assertThat(resolved.balanceImpact).isEqualTo(BalanceImpact.DEBIT)
}
~~~

Add a paired debit/credit transfer test asserting no second direct balance change.

- [ ] **Step 3: Route ingestion through atomic save**

~~~kotlin
val saved = repository.saveNotificationTransaction(
    transaction = finalDraft.toDomain(),
    accountHint = finalDraft.bankAccountHint,
    reviewReason = finalDraft.reviewReason
)
return IngestionResult.Saved(finalDraft.type, saved.reviewReason)
~~~

Keep duplicate detector and `DuplicateNotificationException` behavior.

- [ ] **Step 4: Preserve effects through review**

Rename `ReviewResolution.EXCLUDE` memo to `통계 제외`. No review branch changes linked ID or explicit effect. `EditTransactionUseCase.exclude` also leaves these fields unchanged.

- [ ] **Step 5: Prevent own-transfer double application**

Keep the current direct `applyAccountTransfer` path only for legacy null-effect transactions. For explicit transactions require a paired opposite effect and resolve both without directly changing balances:

~~~kotlin
val paired = requireNotNull(pairedIncomingReviewItem) {
    "입금 알림이 확인된 뒤 내 계좌 이동으로 처리해 주세요."
}
require(transaction.amount == paired.transaction.amount) {
    "출금과 입금 금액이 같아야 해요."
}
require(
    setOf(transaction.balanceImpact, paired.transaction.balanceImpact) ==
        setOf(BalanceImpact.DEBIT, BalanceImpact.CREDIT)
) {
    "출금과 입금 방향을 확인해 주세요."
}
~~~

Validate selected account IDs against the two linked IDs. Pass unchanged accounts to the current atomic repository method; transaction replacement reverses and reapplies identical effects with net zero.

- [ ] **Step 6: Verify and commit**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*NotificationIngestionAtomicityTest" --tests "*ReviewResolutionTest" --tests "*ResolveAccountTransferUseCaseTest" --tests "*EditTransactionUseCaseTest"
git add app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt app/src/main/java/com/choiyoonseo/automoney/domain/review app/src/main/java/com/choiyoonseo/automoney/domain/transactions app/src/test/java/com/choiyoonseo/automoney
git commit -m "fix: separate transfer review from balance effects"
~~~

---

### Task 8: Asset Account Registration UI

**Files:**

- Create: `ui/assets/AssetAccountForm.kt` and `AssetAccountFormTest.kt`
- Modify: `AssetsScreen.kt`, `AssetModels.kt`, `AssetAccountEditorTest.kt`

**Interfaces:** `createAssetAccountFromForm`, `updateAssetAccountFromForm`, `assetAccountMetadataLabel`.

- [ ] **Step 1: Write failing form tests**

~~~kotlin
@Test
fun newAccountStoresOnlySuffix() {
    val account = createAssetAccountFromForm(
        name = "생활비",
        balanceWon = 100_000,
        kind = AssetAccountKind.BANK,
        bankProvider = BankProvider.KB,
        accountNumberInput = "123-456-789012"
    )
    assertThat(account.bankProvider).isEqualTo(BankProvider.KB)
    assertThat(account.accountLast4).isEqualTo("9012")
    assertThat(account.toString()).doesNotContain("123-456-789012")
}

@Test
fun blankEditInputPreservesSuffix() {
    val original = AssetAccount(
        id = 1,
        name = "생활비",
        balanceWon = 100_000,
        bankProvider = BankProvider.KB,
        accountLast4 = "9012"
    )
    val updated = updateAssetAccountFromForm(
        original, "생활비 통장", 90_000, AssetAccountKind.BANK,
        BankProvider.KB, ""
    )
    assertThat(updated.accountLast4).isEqualTo("9012")
}
~~~

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*AssetAccountFormTest" --tests "*AssetAccountEditorTest"
~~~

- [ ] **Step 3: Implement pure conversion**

~~~kotlin
fun createAssetAccountFromForm(
    name: String,
    balanceWon: Long,
    kind: AssetAccountKind,
    bankProvider: BankProvider?,
    accountNumberInput: String
): AssetAccount {
    val last4 = if (kind == AssetAccountKind.BANK && bankProvider != null) {
        normalizeAccountLast4(accountNumberInput)
    } else {
        null
    }
    return AssetAccount(
        name = name.trim(),
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = if (kind == AssetAccountKind.BANK) bankProvider else null,
        accountLast4 = last4
    ).validatedForSave()
}

fun assetAccountMetadataLabel(account: AssetAccount): String =
    listOfNotNull(
        account.kind.label,
        account.bankProvider?.displayName,
        maskedAccountLast4(account.accountLast4)
    ).joinToString(" · ")
~~~

Add validation and exact update behavior:

~~~kotlin
fun AssetAccount.validatedForSave(): AssetAccount {
    val cleanName = name.trim()
    require(cleanName.isNotBlank()) { "계좌 이름을 입력해 주세요." }
    require(balanceWon >= 0) { "잔액은 0원 이상이어야 해요." }
    if (kind == AssetAccountKind.BANK) {
        require((bankProvider == null) == (accountLast4 == null)) {
            "은행과 계좌번호를 함께 입력해 주세요."
        }
    } else {
        require(bankProvider == null && accountLast4 == null) {
            "은행 계좌가 아닌 자산에는 계좌번호를 저장할 수 없어요."
        }
    }
    return copy(name = cleanName)
}

fun updateAssetAccountFromForm(
    account: AssetAccount,
    name: String,
    balanceWon: Long,
    kind: AssetAccountKind,
    bankProvider: BankProvider?,
    accountNumberInput: String
): AssetAccount {
    val nextProvider = bankProvider.takeIf { kind == AssetAccountKind.BANK }
    val nextLast4 = when {
        nextProvider == null -> null
        accountNumberInput.isNotBlank() -> normalizeAccountLast4(accountNumberInput)
        nextProvider == account.bankProvider -> account.accountLast4
        else -> null
    }
    return account.copy(
        name = name,
        balanceWon = balanceWon,
        kind = kind,
        bankProvider = nextProvider,
        accountLast4 = nextLast4
    ).validatedForSave()
}
~~~

- [ ] **Step 4: Add usable UI**

- Bank dropdown appears only for `BANK`
- Account number field appears only after provider selection
- Full input uses `remember`, never `rememberSaveable`
- Edit field starts blank and shows stored masked suffix separately
- Save/cancel clears full input
- Account row uses `assetAccountMetadataLabel`
- Form conversion occurs before repository save

- [ ] **Step 5: Verify and commit**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*AssetAccountFormTest" --tests "*AssetAccountEditorTest"
.\gradlew.bat :app:assembleDebug
git add app/src/main/java/com/choiyoonseo/automoney/ui/assets app/src/main/java/com/choiyoonseo/automoney/domain/assets app/src/test/java/com/choiyoonseo/automoney/ui/assets app/src/test/java/com/choiyoonseo/automoney/domain/assets
git commit -m "feat: register bank account notification identifiers"
~~~

---

### Task 9: Stable IDs In Manual And Edit Flows

**Files:**

- Modify: `SaveManualTransactionUseCase.kt`, `EditTransactionUseCase.kt`
- Modify: `ManualTransactionForm.kt`, `TransactionsScreen.kt`
- Modify: `TransactionEditAccountOptions.kt`, `TransactionEditDialog.kt`
- Modify: `HomeScreen.kt`, `ReviewScreen.kt`
- Modify their existing tests

**Interfaces:** manual/edit callbacks pass `AssetAccount?` instead of a name; all new manual income/expense transactions have explicit effects.

- [ ] **Step 1: Write failing use-case tests**

~~~kotlin
@Test
fun manualExpenseStoresStableIdAndDebit() = runTest {
    val account = AssetAccount(id = 7, name = "생활비", balanceWon = 100_000)
    SaveManualTransactionUseCase(repository).save(
        type = ManualEntryType.EXPENSE,
        amountWon = 10_000,
        categoryText = "식비",
        memo = "점심",
        account = account
    )
    val saved = repository.savedTransactions.single()
    assertThat(saved.linkedAssetAccountId).isEqualTo(7)
    assertThat(saved.balanceImpact).isEqualTo(BalanceImpact.DEBIT)
}

@Test
fun editingIncomingTransferKeepsCredit() = runTest {
    val original = transaction().copy(
        type = TransactionType.TRANSFER,
        linkedAssetAccountId = 7,
        balanceImpact = BalanceImpact.CREDIT
    )
    useCase.update(
        original,
        original.amount.won,
        "기타",
        "",
        account = AssetAccount(id = 7, name = "생활비", balanceWon = 100_000),
        transactionType = TransactionType.TRANSFER
    )
    assertThat(repository.updatedTransactions.single().balanceImpact)
        .isEqualTo(BalanceImpact.CREDIT)
}
~~~

- [ ] **Step 2: Verify RED**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SaveManualTransactionUseCaseTest" --tests "*EditTransactionUseCaseTest" --tests "*TransactionEditAccountOptionsTest"
~~~

- [ ] **Step 3: Update manual semantics**

Accept `account: AssetAccount? = null`:

~~~kotlin
val accountId = account?.id?.takeIf { it > 0 }
val impact = when {
    accountId == null -> BalanceImpact.NONE
    type == ManualEntryType.EXPENSE -> BalanceImpact.DEBIT
    type == ManualEntryType.INCOME -> BalanceImpact.CREDIT
    else -> BalanceImpact.NONE
}
~~~

Store `paymentMethod = account?.name ?: "수동 입력"`, account ID, and impact.

- [ ] **Step 4: Update edit semantics**

Accept `account: AssetAccount?` and compute:

~~~kotlin
val accountId = account?.id?.takeIf { it > 0 }
val nextImpact = when (transactionType) {
    TransactionType.INCOME ->
        accountId?.let { BalanceImpact.CREDIT } ?: BalanceImpact.NONE
    TransactionType.EXPENSE,
    TransactionType.FIXED_EXPENSE,
    TransactionType.SAVING,
    TransactionType.INVESTMENT,
    TransactionType.WALLET_SPEND ->
        accountId?.let { BalanceImpact.DEBIT } ?: BalanceImpact.NONE
    TransactionType.TRANSFER,
    TransactionType.SETTLEMENT,
    TransactionType.EXCLUDED,
    TransactionType.REFUND,
    TransactionType.WALLET_TOPUP ->
        transaction.balanceImpact ?: BalanceImpact.NONE
}
~~~

Store ID/name/effect. `exclude` leaves them unchanged.

- [ ] **Step 5: Replace name-only UI state**

Pass `List<AssetAccount>` to both forms and keep selected ID:

~~~kotlin
var selectedAccountId by remember(resetSignal, accounts) {
    mutableStateOf(accounts.firstOrNull()?.id)
}
val selectedAccount = accounts.firstOrNull { account ->
    account.id == selectedAccountId
}
~~~

Change callbacks to `AssetAccount?`. Pass `assetAccounts` directly from Transactions, Home, and Review. Replace string options with:

~~~kotlin
data class TransactionAccountOption(
    val accountId: Long?,
    val label: String
)
~~~

Show account selection for both manual expense and manual income. Manual transfer remains `NONE` because its two account legs require the existing transfer-review flow.

- [ ] **Step 6: Clarify Review labels**

Add optional dialog labels. Review uses `통계 제외` for exclusion and `알림 오인식` for delete. Delete reverses the effect; exclusion preserves it.

- [ ] **Step 7: Verify and commit**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SaveManualTransactionUseCaseTest" --tests "*EditTransactionUseCaseTest" --tests "*TransactionEditAccountOptionsTest" --tests "*ReviewResolutionTest"
.\gradlew.bat :app:assembleDebug
git add app/src/main/java/com/choiyoonseo/automoney/domain app/src/main/java/com/choiyoonseo/automoney/ui app/src/test/java/com/choiyoonseo/automoney
git commit -m "refactor: link transaction account selections by id"
~~~

---

### Task 10: Samples, Full Verification, Device Update

**Files:**

- Modify: `SampleNotificationScenarios.kt`, `SampleNotificationScenariosTest.kt`
- Create: `docs/testing/bank-notification-balance-sync.md`

- [ ] **Step 1: Add deterministic account samples**

Add `packageName` and `notificationKey` to each sample. Add KB debit, credit, and transfer samples:

~~~kotlin
SampleNotificationScenario(
    id = "kb_account_withdrawal",
    label = "KB 계좌 출금",
    description = "등록 계좌가 맞으면 잔액을 차감하고 거래를 검토해요",
    packageName = "com.kbstar.kbbank",
    notificationKey = "sample-kb-withdrawal",
    notificationTitle = "KB스타뱅킹",
    text = "계좌 123-***-4567",
    bigText = "10,000원 출금"
)
~~~

Keep existing Toss card/top-up/refund/gateway samples.

- [ ] **Step 2: Update sample tests**

Assert unique IDs, package/key propagation, same identity hash stability, and different-post-time hash separation.

- [ ] **Step 3: Run full automated verification**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.choiyoonseo.automoney.data.local.AppDatabaseMigrationTest,com.choiyoonseo.automoney.data.repository.RoomMoneyRepositoryReviewItemTest
.\gradlew.bat :app:assembleDebug
~~~

Expected: all commands BUILD SUCCESSFUL.

- [ ] **Step 4: Install and launch**

~~~powershell
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell monkey -p com.choiyoonseo.automoney -c android.intent.category.LAUNCHER 1
~~~

Expected: authorized Galaxy, install `Success`, process starts.

- [ ] **Step 5: Verify device behavior**

1. Add bank/provider/full account number; reopen and confirm masked suffix only
2. Run unmatched sample; Review created, balance unchanged
3. Register matching suffix and run a new-time sample; balance changes once
4. Repeat same identity; no second balance change
5. Mark `통계 제외`; balance stays changed
6. Run another sample and choose `알림 오인식`; effect reverses
7. Confirm no new app `FATAL EXCEPTION`:

~~~powershell
& $adb logcat -d -t 500 AndroidRuntime:E *:S
~~~

- [ ] **Step 6: Record honest support matrix**

Create one row per provider with package, synthetic tests, real sample, debit, credit, transfer review, notes. Use `VERIFIED` only for executed real-device cases. Use `UNVERIFIED - no real sample available` for unavailable banks.

- [ ] **Step 7: Final verification and commit**

~~~powershell
git diff --check
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
git status --short
git add app/src/main app/src/test app/src/androidTest app/schemas docs/testing
git commit -m "feat: sync asset balances from bank notifications"
~~~

Do not push or synchronize `main`/`claude/ui-polish` without a later explicit request.
