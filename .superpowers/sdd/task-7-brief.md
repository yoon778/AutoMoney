### Task 7: Full Verification And Galaxy Device Check

**Files:**
- Modify: `docs/superpowers/plans/2026-07-03-financial-notification-router.md`

**Interfaces:**
- Consumes: completed Tasks 1-6.
- Produces: verified debug APK for Galaxy testing.

- [ ] **Step 1: Run full unit tests and debug build**

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
