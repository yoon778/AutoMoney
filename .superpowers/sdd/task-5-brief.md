### Task 5: Wire Router Into Ingestion And AppContainer

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt`
- Test: existing ingestion tests, parser tests, and diagnostics tests

**Interfaces:**
- Consumes: `NotificationParser`
- Produces: `NotificationIngestionUseCase(parser: NotificationParser, ...)`
- Produces detailed `IngestionResult` if Task 6 uses parsed type diagnostics.

- [ ] **Step 1: Change ingestion constructor dependency**

Change:

```kotlin
private val parser: TossNotificationParser
```

To:

```kotlin
private val parser: NotificationParser
```

And update the import from `TossNotificationParser` to `NotificationParser`.

- [ ] **Step 2: Wire the router in AppContainer**

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

- [ ] **Step 3: Run targeted ingestion-related tests**

Run:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.choiyoonseo.automoney.domain.parser.TossNotificationParserTest --tests com.choiyoonseo.automoney.domain.parser.CommonFinanceNotificationParserTest --tests com.choiyoonseo.automoney.domain.parser.NotificationParserRouterTest --no-daemon --console=plain
```

Expected: PASS.

---

