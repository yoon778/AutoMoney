# Financial Notification Router Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend AutoMoney from Toss-only notification ingestion to allowlisted financial-app notification ingestion, starting with KB Star Banking.

**Architecture:** Introduce a `NotificationParser` interface and a parser router. Keep the existing Toss parser, add a common Korean finance parser, and replace the listener's hardcoded Toss filter with a small financial-app registry. Keep parsing conservative: clear card payments can auto-save, while transfers, deposits, top-ups, refunds, account movements, and low-confidence matches go to Review.

**Tech Stack:** Kotlin, Android NotificationListenerService, Jetpack Compose Material3, Room, JUnit, Truth, Gradle.

## Global Constraints

- Do not read all notifications; inspect only allowlisted financial app packages.
- Do not add OCR, SMS reading, email reading, screen scraping, cloud sync, or balance syncing.
- Do not store full account-like numbers; mask sensitive number patterns before saving diagnostics or review-facing text.
- Do not change the Room schema unless implementation proves it is unavoidable.
- Keep Toss parsing functional.
- In this repository, do not create partial commits until the initial untracked project baseline is intentionally staged.

---

## File Structure

- Create `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParser.kt`
  - Owns the parser interface used by Toss, common finance parser, and the router.
- Create `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouter.kt`
  - Selects the first parser that can parse a snapshot.
- Modify `app/src/main/java/com/choiyoonseo/automoney/domain/parser/TossNotificationParser.kt`
  - Implements `NotificationParser`.
- Create `app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt`
  - Owns exact package allowlist constants, including Toss and KB Star Banking.
- Modify `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`
  - Uses the registry instead of the hardcoded Toss check.
- Create `app/src/main/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMasker.kt`
  - Masks account-like and long non-amount numbers.
- Modify `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
  - Saves masked previews and richer ingestion messages.
- Create `app/src/main/java/com/choiyoonseo/automoney/domain/parser/CommonFinanceNotificationParser.kt`
  - Parses KB/bank/card/pay notifications with conservative Korean finance keyword rules.
- Modify `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
  - Depends on `NotificationParser` instead of `TossNotificationParser`; returns detailed ingestion results.
- Modify `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
  - Wires `NotificationParserRouter(TossNotificationParser(), CommonFinanceNotificationParser())`.
- Modify `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
  - Renames Toss-specific copy to financial-app copy and shows richer diagnostics.
- Tests:
  - Create `app/src/test/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouterTest.kt`
  - Create `app/src/test/java/com/choiyoonseo/automoney/notification/FinancialAppRegistryTest.kt`
  - Create `app/src/test/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMaskerTest.kt`
  - Create `app/src/test/java/com/choiyoonseo/automoney/domain/parser/CommonFinanceNotificationParserTest.kt`
  - Modify existing notification ingestion and diagnostics tests as needed.

---

### Task 1: Parser Interface And Router

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParser.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouter.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/TossNotificationParser.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouterTest.kt`

**Interfaces:**
- Produces: `interface NotificationParser`
- Produces: `class NotificationParserRouter(private val parsers: List<NotificationParser>) : NotificationParser`
- Consumes: existing `NotificationSnapshot` and `ParseResult`

- [x] **Step 1: Write failing router tests**

```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class NotificationParserRouterTest {
    @Test
    fun routesToFirstParserThatCanParse() {
        val parser = RecordingParser(canParse = true, result = ParseResult.Ignored("handled"))
        val fallback = RecordingParser(canParse = true, result = ParseResult.Ignored("fallback"))
        val router = NotificationParserRouter(listOf(parser, fallback))

        val result = router.parse(snapshot("viva.republica.toss"))

        assertThat(result).isEqualTo(ParseResult.Ignored("handled"))
        assertThat(parser.parseCalls).isEqualTo(1)
        assertThat(fallback.parseCalls).isEqualTo(0)
    }

    @Test
    fun ignoresWhenNoParserCanParse() {
        val router = NotificationParserRouter(listOf(RecordingParser(canParse = false)))

        val result = router.parse(snapshot("unknown.package"))

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    private fun snapshot(packageName: String) = NotificationSnapshot(
        packageName = packageName,
        title = "title",
        text = "text",
        bigText = null,
        postedAt = Instant.parse("2026-07-03T01:00:00Z")
    )

    private class RecordingParser(
        private val canParse: Boolean,
        private val result: ParseResult = ParseResult.Ignored("unused")
    ) : NotificationParser {
        var parseCalls = 0

        override fun canParse(snapshot: NotificationSnapshot): Boolean = canParse

        override fun parse(snapshot: NotificationSnapshot): ParseResult {
            parseCalls += 1
            return result
        }
    }
}
```

- [x] **Step 2: Run the failing router test**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest --no-daemon --console=plain
```

Expected: FAIL with unresolved reference for `NotificationParser` or `NotificationParserRouter`.

- [x] **Step 3: Add the parser interface**

```kotlin
package com.choiyoonseo.automoney.domain.parser

interface NotificationParser {
    fun canParse(snapshot: NotificationSnapshot): Boolean
    fun parse(snapshot: NotificationSnapshot): ParseResult
}
```

- [x] **Step 4: Add the parser router**

```kotlin
package com.choiyoonseo.automoney.domain.parser

class NotificationParserRouter(
    private val parsers: List<NotificationParser>
) : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        parsers.any { it.canParse(snapshot) }

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        val parser = parsers.firstOrNull { it.canParse(snapshot) }
            ?: return ParseResult.Ignored("unsupported package")
        return parser.parse(snapshot)
    }
}
```

- [x] **Step 5: Make Toss parser implement the interface**

Change the class declaration and add `canParse`:

```kotlin
class TossNotificationParser : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        snapshot.packageName == TOSS_PACKAGE

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        if (!canParse(snapshot)) {
            return ParseResult.Ignored("unsupported package")
        }
        // keep existing parse body
    }
}
```

- [x] **Step 6: Run router and Toss parser tests**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest --tests com.choiyoonseo.automoney.domain.parser.TossNotificationParserTest --no-daemon --console=plain
```

Expected: PASS.

---

### Task 2: Financial App Registry And Listener Filtering

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/FinancialAppRegistryTest.kt`

**Interfaces:**
- Produces: `object FinancialAppRegistry`
- Produces: `fun isSupportedPackage(packageName: String): Boolean`
- Produces constants: `TOSS_PACKAGE`, `KB_STAR_BANKING_PACKAGE`

- [x] **Step 1: Write failing registry tests**

```kotlin
package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FinancialAppRegistryTest {
    @Test
    fun supportsTossAndKbStarBanking() {
        assertThat(FinancialAppRegistry.isSupportedPackage("viva.republica.toss")).isTrue()
        assertThat(FinancialAppRegistry.isSupportedPackage("com.kbstar.kbbank")).isTrue()
    }

    @Test
    fun rejectsUnknownAndBlankPackages() {
        assertThat(FinancialAppRegistry.isSupportedPackage("com.shopping.adapp")).isFalse()
        assertThat(FinancialAppRegistry.isSupportedPackage("")).isFalse()
    }
}
```

- [x] **Step 2: Run the failing registry test**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.FinancialAppRegistryTest --no-daemon --console=plain
```

Expected: FAIL with unresolved reference `FinancialAppRegistry`.

- [x] **Step 3: Add the registry**

```kotlin
package com.choiyoonseo.automoney.notification

object FinancialAppRegistry {
    const val TOSS_PACKAGE = "viva.republica.toss"
    const val KB_STAR_BANKING_PACKAGE = "com.kbstar.kbbank"

    private val supportedPackages = setOf(
        TOSS_PACKAGE,
        KB_STAR_BANKING_PACKAGE
    )

    fun isSupportedPackage(packageName: String): Boolean =
        packageName in supportedPackages
}
```

Note: `com.kbstar.kbbank` is the Google Play package id for KB Star Banking.

- [x] **Step 4: Replace the listener's hardcoded Toss filter**

Change:

```kotlin
if (sbn.packageName != "viva.republica.toss") return
```

To:

```kotlin
if (!FinancialAppRegistry.isSupportedPackage(sbn.packageName)) return
```

- [x] **Step 5: Run registry test and compile**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.FinancialAppRegistryTest :app:assembleDebug --no-daemon --console=plain
```

Expected: PASS and debug build succeeds.

---

### Task 3: Sensitive Text Masking

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMasker.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/parser/SensitiveTextMaskerTest.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt`

**Interfaces:**
- Produces: `object SensitiveTextMasker`
- Produces: `fun mask(text: String): String`
- Consumes: `LastNotificationDiagnostic.fromIngestionResult(...)`

- [x] **Step 1: Write failing masking tests**

```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SensitiveTextMaskerTest {
    @Test
    fun masksAccountLikeNumbersButKeepsAmounts() {
        val text = "KB 123456-78-901234 account 10,000 won payment"

        val masked = SensitiveTextMasker.mask(text)

        assertThat(masked).contains("****1234")
        assertThat(masked).contains("10,000 won")
        assertThat(masked).doesNotContain("123456-78-901234")
    }

    @Test
    fun masksLongPlainNumbers() {
        val masked = SensitiveTextMasker.mask("sender 110123456789 sent 5,000 won")

        assertThat(masked).contains("****6789")
        assertThat(masked).doesNotContain("110123456789")
    }
}
```

- [x] **Step 2: Run the failing masking test**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.SensitiveTextMaskerTest --no-daemon --console=plain
```

Expected: FAIL with unresolved reference `SensitiveTextMasker`.

- [x] **Step 3: Add the masker**

```kotlin
package com.choiyoonseo.automoney.domain.parser

object SensitiveTextMasker {
    private val amountLikePattern = Regex("""\d{1,3}(,\d{3})*\s*(won|원)""", RegexOption.IGNORE_CASE)
    private val longNumberPattern = Regex("""\d[\d-]{5,}\d""")

    fun mask(text: String): String {
        val protectedAmounts = mutableListOf<String>()
        val protectedText = amountLikePattern.replace(text) { match ->
            val token = "__AMOUNT_${protectedAmounts.size}__"
            protectedAmounts += match.value
            token
        }

        val masked = longNumberPattern.replace(protectedText) { match ->
            val digits = match.value.filter(Char::isDigit)
            if (digits.length < 6) {
                match.value
            } else {
                "****" + digits.takeLast(4)
            }
        }

        return protectedAmounts.foldIndexed(masked) { index, current, amount ->
            current.replace("__AMOUNT_${index}__", amount)
        }
    }
}
```

- [x] **Step 4: Use masking in diagnostic previews**

In `NotificationDiagnosticsStore.kt`, import `SensitiveTextMasker` and apply it in preview creation:

```kotlin
private fun NotificationSnapshot.textPreview(): String {
    val rawPreview = listOfNotNull(title, text, bigText)
        .flatMap { it.lineSequence().map(String::trim).filter(String::isNotBlank).toList() }
        .distinct()
        .joinToString("\n")
        .take(160)
    return SensitiveTextMasker.mask(rawPreview)
}
```

Keep the existing function name if it already exists; replace only its body.

- [x] **Step 5: Add or update diagnostics test for masking**

Add to `NotificationDiagnosticsStoreTest`:

```kotlin
@Test
fun masksSensitiveNumbersInDiagnosticPreview() {
    val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
        snapshot = NotificationSnapshot(
            packageName = "com.kbstar.kbbank",
            title = "KB",
            text = "account 123456-78-901234 10,000 won payment",
            bigText = null,
            postedAt = Instant.parse("2026-07-03T01:00:00Z")
        ),
        result = IngestionResult.Saved,
        receivedAt = Instant.parse("2026-07-03T01:00:05Z")
    )

    assertThat(diagnostic.textPreview).contains("****1234")
    assertThat(diagnostic.textPreview).doesNotContain("123456-78-901234")
}
```

If Task 5 has already changed `IngestionResult.Saved` to a data class, use `IngestionResult.Saved(TransactionType.EXPENSE, null)`.

- [x] **Step 6: Run masking and diagnostics tests**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.SensitiveTextMaskerTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest --no-daemon --console=plain
```

Expected: PASS.

---

### Task 4: Common Korean Finance Parser

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/CommonFinanceNotificationParser.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/domain/parser/CommonFinanceNotificationParserTest.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/model/MoneyModels.kt`

**Interfaces:**
- Produces: `class CommonFinanceNotificationParser : NotificationParser`
- Consumes: `FinancialAppRegistry.KB_STAR_BANKING_PACKAGE`
- Produces parsed `TransactionDraft` values for expense, transfer, income, wallet top-up, and refund.
- Produces new `ReviewReason.INCOME_UNKNOWN` if needed for deposit review items.

- [x] **Step 1: Add deposit review reason**

Add to `ReviewReason`:

```kotlin
INCOME_UNKNOWN
```

Place it after `TRANSFER_UNKNOWN` so existing enum names remain unchanged for persisted values.

- [x] **Step 2: Write failing common parser tests**

```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class CommonFinanceNotificationParserTest {
    private val parser = CommonFinanceNotificationParser()

    @Test
    fun parsesKbCardPaymentAsAutoConfirmedExpense() {
        val result = parser.parse(snapshot(text = "STARBUCKS 6,100${WON} ${APPROVAL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6100)
        assertThat(draft.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
        assertThat(draft.merchant).isEqualTo("STARBUCKS")
        assertThat(draft.sourceApp).isEqualTo("com.kbstar.kbbank")
    }

    @Test
    fun parsesTransferAsNeedsReview() {
        val result = parser.parse(snapshot(text = "Kim 10,000${WON} ${TRANSFER}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.TRANSFER_UNKNOWN)
    }

    @Test
    fun parsesDepositAsNeedsReviewIncome() {
        val result = parser.parse(snapshot(text = "Company 500,000${WON} ${DEPOSIT}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.INCOME)
        assertThat(draft.direction).isEqualTo(TransactionDirection.INCOME)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.INCOME_UNKNOWN)
    }

    @Test
    fun parsesTopupAsNeedsReviewWalletTopup() {
        val result = parser.parse(snapshot(text = "Naver Pay 10,000${WON} ${TOPUP}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.WALLET_TOPUP)
    }

    @Test
    fun parsesCancelAsRefundReview() {
        val result = parser.parse(snapshot(text = "Coupang 12,000${WON} ${CANCEL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.REFUND)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.REFUND_OR_CANCEL)
    }

    @Test
    fun ignoresCouponPromotionWithMoneyAmount() {
        val result = parser.parse(snapshot(text = "Event 10,000${WON} ${COUPON}"))

        assertThat(result).isEqualTo(ParseResult.Ignored("promotional notification"))
    }

    @Test
    fun ignoresUnsupportedPackage() {
        val result = parser.parse(snapshot(packageName = "com.shopping.adapp", text = "STARBUCKS 6,100${WON} ${APPROVAL}"))

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    private fun snapshot(
        packageName: String = "com.kbstar.kbbank",
        text: String
    ) = NotificationSnapshot(
        packageName = packageName,
        title = "KB",
        text = text,
        bigText = null,
        postedAt = Instant.parse("2026-07-03T01:00:00Z")
    )

    companion object {
        private const val WON = "\uc6d0"
        private const val APPROVAL = "\uc2b9\uc778"
        private const val TRANSFER = "\uc774\uccb4"
        private const val DEPOSIT = "\uc785\uae08"
        private const val TOPUP = "\ucda9\uc804"
        private const val CANCEL = "\ucde8\uc18c"
        private const val COUPON = "\ucfe0\ud3f0"
    }
}
```

- [x] **Step 3: Run the failing common parser tests**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --no-daemon --console=plain
```

Expected: FAIL with unresolved reference `CommonFinanceNotificationParser`.

- [x] **Step 4: Add the common parser implementation**

```kotlin
package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.notification.FinancialAppRegistry
import java.security.MessageDigest

class CommonFinanceNotificationParser : NotificationParser {
    override fun canParse(snapshot: NotificationSnapshot): Boolean =
        snapshot.packageName == FinancialAppRegistry.KB_STAR_BANKING_PACKAGE

    override fun parse(snapshot: NotificationSnapshot): ParseResult {
        if (!canParse(snapshot)) return ParseResult.Ignored("unsupported package")

        val text = snapshot.combinedText.trim()
        if (text.isBlank()) return ParseResult.Ignored("empty notification")
        if (isPromotion(text)) return ParseResult.Ignored("promotional notification")

        val amount = extractAmount(text) ?: return ParseResult.Ignored("amount not found")
        val hash = hash(text)
        val merchant = extractMerchant(text, amount)
        val maskedMemo = SensitiveTextMasker.mask(text.lineSequence().firstOrNull().orEmpty())

        return when {
            containsAny(text, REFUND_KEYWORDS) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.REFUND,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.75,
                reviewReason = ReviewReason.REFUND_OR_CANCEL,
                merchant = merchant,
                memo = maskedMemo,
                hash = hash
            )
            containsAny(text, TOPUP_KEYWORDS) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.WALLET_TOPUP,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.8,
                reviewReason = ReviewReason.WALLET_TOPUP,
                merchant = merchant,
                memo = maskedMemo,
                hash = hash
            )
            containsAny(text, DEPOSIT_KEYWORDS) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.INCOME,
                type = TransactionType.INCOME,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.7,
                reviewReason = ReviewReason.INCOME_UNKNOWN,
                counterparty = merchant,
                memo = maskedMemo,
                hash = hash
            )
            containsAny(text, TRANSFER_KEYWORDS) || hasAccountHint(text) -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.TRANSFER,
                status = TransactionStatus.NEEDS_REVIEW,
                confidence = 0.7,
                reviewReason = ReviewReason.TRANSFER_UNKNOWN,
                counterparty = merchant,
                memo = maskedMemo,
                hash = hash
            )
            containsAny(text, PAYMENT_KEYWORDS) && merchant.isNotBlank() -> parsed(
                snapshot = snapshot,
                amount = amount,
                direction = TransactionDirection.EXPENSE,
                type = TransactionType.EXPENSE,
                status = TransactionStatus.AUTO_CONFIRMED,
                confidence = 0.86,
                reviewReason = null,
                merchant = merchant,
                memo = merchant,
                hash = hash
            )
            else -> ParseResult.Ignored("unsupported finance notification")
        }
    }

    private fun parsed(
        snapshot: NotificationSnapshot,
        amount: MoneyAmount,
        direction: TransactionDirection,
        type: TransactionType,
        status: TransactionStatus,
        confidence: Double,
        reviewReason: ReviewReason?,
        merchant: String? = null,
        counterparty: String? = null,
        memo: String?,
        hash: String
    ): ParseResult.Parsed = ParseResult.Parsed(
        TransactionDraft(
            occurredAt = snapshot.postedAt,
            amount = amount,
            direction = direction,
            type = type,
            category = if (type == TransactionType.EXPENSE) guessCategory(merchant.orEmpty()) else null,
            paymentMethod = "KB",
            merchant = merchant,
            counterparty = counterparty,
            memo = memo,
            sourceApp = snapshot.packageName,
            sourceNotificationHash = hash,
            status = status,
            confidence = confidence,
            monthKey = snapshot.postedAt.toKoreanMonthKey(),
            reviewReason = reviewReason
        )
    )

    private fun extractAmount(text: String): MoneyAmount? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        return MoneyAmount(match.groupValues[1].replace(",", "").toLong())
    }

    private fun extractMerchant(text: String, amount: MoneyAmount): String {
        val amountText = "%,d\uc6d0".format(amount.won)
        val line = text.lineSequence().firstOrNull { it.contains(amountText) } ?: text
        return line.substringBefore(amountText)
            .replace("KB", "")
            .trim()
            .ifBlank { "" }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it, ignoreCase = true) }

    private fun isPromotion(text: String): Boolean =
        containsAny(text, PROMOTION_KEYWORDS) && !containsAny(text, PAYMENT_KEYWORDS)

    private fun hasAccountHint(text: String): Boolean =
        containsAny(text, ACCOUNT_KEYWORDS) || ACCOUNT_LIKE_REGEX.containsMatchIn(text)

    private fun guessCategory(merchant: String): Category {
        val lower = merchant.lowercase()
        return when {
            merchant.contains("GS25") || merchant.contains("CU") -> Category.FOOD
            lower.contains("starbucks") || lower.contains("coffee") -> Category.CAFE_SNACK
            else -> Category.OTHER
        }
    }

    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val AMOUNT_REGEX = Regex("""([0-9,]+)\s*\uc6d0""")
        private val ACCOUNT_LIKE_REGEX = Regex("""\d[\d-]{5,}\d""")
        private val PAYMENT_KEYWORDS = listOf("\uacb0\uc81c", "\uc2b9\uc778", "\uc0ac\uc6a9")
        private val TRANSFER_KEYWORDS = listOf("\uc774\uccb4", "\uc1a1\uae08", "\ucd9c\uae08")
        private val DEPOSIT_KEYWORDS = listOf("\uc785\uae08")
        private val TOPUP_KEYWORDS = listOf("\ucda9\uc804", "\ud3ec\uc778\ud2b8", "\ud398\uc774\uba38\ub2c8")
        private val REFUND_KEYWORDS = listOf("\ucde8\uc18c", "\ud658\ubd88")
        private val ACCOUNT_KEYWORDS = listOf("\uacc4\uc88c", "\uacc4\uc88c\ubc88\ud638")
        private val PROMOTION_KEYWORDS = listOf("\ucfe0\ud3f0", "\ud61c\ud0dd", "\uc774\ubca4\ud2b8", "\uad11\uace0")
    }
}
```

- [x] **Step 5: Run common parser tests**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --no-daemon --console=plain
```

Expected: PASS.

---

### Task 5: Wire Router Into Ingestion And AppContainer

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Test: existing ingestion tests, parser tests, and diagnostics tests

**Interfaces:**
- Consumes: `NotificationParser`
- Produces: `NotificationIngestionUseCase(parser: NotificationParser, ...)`
- Produces detailed `IngestionResult` if Task 6 uses parsed type diagnostics.

- [x] **Step 1: Change ingestion constructor dependency**

Change:

```kotlin
private val parser: TossNotificationParser
```

To:

```kotlin
private val parser: NotificationParser
```

And update the import from `TossNotificationParser` to `NotificationParser`.

- [x] **Step 2: Wire the router in AppContainer**

Change the parser construction to:

```kotlin
val notificationIngestionUseCase = NotificationIngestionUseCase(
    parser = NotificationParserRouter(
        listOf(
            TossNotificationParser(),
            CommonFinanceNotificationParser()
        )
    ),
    categorizationEngine = CategorizationEngine(),
    duplicateDetector = DuplicateDetector(),
    repository = repository
)
```

Add imports:

```kotlin
import com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParser
import com.choiyoonseo.automoney.domain.parser.NotificationParserRouter
```

- [x] **Step 3: Run targeted ingestion-related tests**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.TossNotificationParserTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --tests com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest --no-daemon --console=plain
```

Expected: PASS.

---

### Task 6: Diagnostics And Settings Copy

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStore.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/NotificationDiagnosticsStoreTest.kt`

**Interfaces:**
- Produces: detailed ingestion result types.
- Produces: `LastNotificationDiagnostic.parsedType: String?`
- Produces Settings copy that refers to allowed financial apps instead of Toss only.

- [x] **Step 1: Write failing diagnostics test for parsed type and source package**

```kotlin
@Test
fun diagnosticIncludesParsedTypeForSavedFinanceNotification() {
    val diagnostic = LastNotificationDiagnostic.fromIngestionResult(
        snapshot = NotificationSnapshot(
            packageName = "com.kbstar.kbbank",
            title = "KB",
            text = "STARBUCKS 6,100 won payment",
            bigText = null,
            postedAt = Instant.parse("2026-07-03T01:00:00Z")
        ),
        result = IngestionResult.Saved(TransactionType.EXPENSE, null),
        receivedAt = Instant.parse("2026-07-03T01:00:05Z")
    )

    assertThat(diagnostic.packageName).isEqualTo("com.kbstar.kbbank")
    assertThat(diagnostic.parsedType).isEqualTo("EXPENSE")
}
```

- [x] **Step 2: Replace enum ingestion result with sealed result**

Replace:

```kotlin
enum class IngestionResult {
    Saved,
    Ignored,
    Duplicate
}
```

With:

```kotlin
sealed interface IngestionResult {
    data class Saved(
        val transactionType: TransactionType,
        val reviewReason: ReviewReason?
    ) : IngestionResult

    data class Duplicate(
        val transactionType: TransactionType?
    ) : IngestionResult

    data class Ignored(
        val reason: String
    ) : IngestionResult
}
```

Add imports for `TransactionType` and `ReviewReason`.

- [x] **Step 3: Update ingestion returns**

Use these exact return shapes:

```kotlin
if (parsed !is ParseResult.Parsed) {
    val reason = (parsed as? ParseResult.Ignored)?.reason ?: "not parsed"
    return IngestionResult.Ignored(reason)
}
```

```kotlin
if (duplicateDecision == DuplicateDecision.DUPLICATE) {
    return IngestionResult.Duplicate(withRules.type)
}
```

```kotlin
return IngestionResult.Saved(finalDraft.type, finalDraft.reviewReason)
```

- [x] **Step 4: Add parsed type to diagnostics model and serialization**

Add field:

```kotlin
val parsedType: String?
```

Set it from result:

```kotlin
val parsedType = when (result) {
    is IngestionResult.Saved -> result.transactionType.name
    is IngestionResult.Duplicate -> result.transactionType?.name
    is IngestionResult.Ignored -> null
}
```

Add preference key:

```kotlin
private const val KEY_PARSED_TYPE = "parsedType"
```

Round-trip it in `toPreferenceMap()` and `lastNotificationDiagnosticFromPreferenceMap(...)`.

- [x] **Step 5: Update diagnostic result mapping**

Use:

```kotlin
val diagnosticResult = when (result) {
    is IngestionResult.Saved -> NotificationDiagnosticResult.SAVED
    is IngestionResult.Duplicate -> NotificationDiagnosticResult.DUPLICATE
    is IngestionResult.Ignored -> NotificationDiagnosticResult.IGNORED
}
```

Set message for ignored:

```kotlin
val message = (result as? IngestionResult.Ignored)?.reason
```

- [x] **Step 6: Update Settings text**

Replace Toss-only user-facing copy in `SettingsScreen.kt` with finance-app copy:

```kotlin
true -> "허용한 금융 앱 알림이 들어오면 자동 기록 후보로 처리해요."
false -> "알림 접근 권한을 허용해야 결제/송금 알림을 읽을 수 있어요."
```

For the empty diagnostics card:

```kotlin
Text("아직 처리한 금융 앱 알림이 없어요.")
Text("허용한 은행, 카드, 페이, 토스 알림이 들어오면 여기에 마지막 결과가 표시돼요.")
```

- [x] **Step 7: Show parsed type when present**

Inside the non-null diagnostic branch:

```kotlin
lastNotificationDiagnostic.parsedType?.let {
    Text("Type $it")
}
```

- [x] **Step 8: Run diagnostics tests and compile**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest :app:assembleDebug --no-daemon --console=plain
```

Expected: PASS and debug build succeeds.

---

### Task 7: Full Verification And Galaxy Device Check

**Files:**
- Modify: `docs/superpowers/plans/2026-07-03-financial-notification-router.md`

**Interfaces:**
- Consumes: completed Tasks 1-6.
- Produces: verified debug APK for Galaxy testing.

- [x] **Step 1: Run full unit tests and debug build**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Reconnect the Galaxy phone and confirm ADB sees it**

Run:

```powershell
$adb='C:\Users\cys04\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
```

Expected: one device with state `device`.

- [ ] **Step 3: Install the debug APK**

Run:

```powershell
$adb='C:\Users\cys04\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$apk='C:\Users\cys04\OneDrive\Desktop\AutoMoney\app\build\outputs\apk\debug\app-debug.apk'
& $adb install -r $apk
```

Expected: `Success`.

- [ ] **Step 4: Launch AutoMoney**

Run:

```powershell
$adb='C:\Users\cys04\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb shell monkey -p com.choiyoonseo.automoney 1
```

Expected: AutoMoney opens on the phone.

- [ ] **Step 5: Confirm notification access remains enabled**

Run:

```powershell
$adb='C:\Users\cys04\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb shell settings get secure enabled_notification_listeners
```

Expected: output contains `com.choiyoonseo.automoney/com.choiyoonseo.automoney.notification.MoneyNotificationListenerService`.

- [ ] **Step 6: Trigger a real KB notification**

On the phone, create a safe small KB Star Banking notification, such as a low-risk transfer, card approval, or account alert that the user is comfortable testing.

Expected: AutoMoney Settings diagnostics card changes from no-record to saved, ignored, duplicate, or error for package `com.kbstar.kbbank`.

- [ ] **Step 7: Check app result**

Open AutoMoney:

- If diagnostics says `saved`, check Transactions and Review.
- If diagnostics says `ignored`, capture the masked diagnostics text and add a new parser test for that wording.
- If diagnostics says `error`, inspect Logcat and fix the exception before retesting.

- [ ] **Step 8: Record verification notes in this plan**

Append:

```markdown
## Verification Notes

- Unit tests:
- Debug build:
- Galaxy install:
- Notification access:
- KB real notification result:
- Remaining parser wording gaps:
```

Fill each line with concrete results.

## Verification Notes

- Unit tests: `:app:testDebugUnitTest` passed on 2026-07-03.
- Debug build: `:app:assembleDebug` passed on 2026-07-03.
- Galaxy install: pending; `adb devices -l` currently shows no connected device.
- Notification access: pending device reconnect.
- KB real notification result: pending a physical Galaxy test with a real KB Star Banking notification.
- Remaining parser wording gaps: real KB notification wording still needs capture from the phone.
