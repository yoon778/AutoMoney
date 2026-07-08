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

- [ ] **Step 1: Write failing masking tests**

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

- [ ] **Step 2: Run the failing masking test**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.SensitiveTextMaskerTest --no-daemon --console=plain
```

Expected: FAIL with unresolved reference `SensitiveTextMasker`.

- [ ] **Step 3: Add the masker**

```kotlin
package com.choiyoonseo.automoney.domain.parser

object SensitiveTextMasker {
    private val amountLikePattern = Regex("""\d{1,3}(,\d{3})*\s*(won|??""", RegexOption.IGNORE_CASE)
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

- [ ] **Step 4: Use masking in diagnostic previews**

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

- [ ] **Step 5: Add or update diagnostics test for masking**

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

- [ ] **Step 6: Run masking and diagnostics tests**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.SensitiveTextMaskerTest --tests com.choiyoonseo.automoney.notification.NotificationDiagnosticsStoreTest --no-daemon --console=plain
```

Expected: PASS.

---

