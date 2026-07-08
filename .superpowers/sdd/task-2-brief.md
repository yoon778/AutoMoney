### Task 2: Financial App Registry And Listener Filtering

**Files:**
- Create: `app/src/main/java/com/choiyoonseo/automoney/notification/FinancialAppRegistry.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt`
- Test: `app/src/test/java/com/choiyoonseo/automoney/notification/FinancialAppRegistryTest.kt`

**Interfaces:**
- Produces: `object FinancialAppRegistry`
- Produces: `fun isSupportedPackage(packageName: String): Boolean`
- Produces constants: `TOSS_PACKAGE`, `KB_STAR_BANKING_PACKAGE`

- [ ] **Step 1: Write failing registry tests**

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

- [ ] **Step 2: Run the failing registry test**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.FinancialAppRegistryTest --no-daemon --console=plain
```

Expected: FAIL with unresolved reference `FinancialAppRegistry`.

- [ ] **Step 3: Add the registry**

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

- [ ] **Step 4: Replace the listener's hardcoded Toss filter**

Change:

```kotlin
if (sbn.packageName != "viva.republica.toss") return
```

To:

```kotlin
if (!FinancialAppRegistry.isSupportedPackage(sbn.packageName)) return
```

- [ ] **Step 5: Run registry test and compile**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.notification.FinancialAppRegistryTest :app:assembleDebug --no-daemon --console=plain
```

Expected: PASS and debug build succeeds.

---

