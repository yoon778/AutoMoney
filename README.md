# AutoMoney

알림 기반 자동 가계부 MVP Android 앱.

## 실행

1. Android Studio에서 이 폴더 열기:
   `C:\Users\cys04\OneDrive\Desktop\AutoMoney`
2. 휴대폰 또는 에뮬레이터 연결
3. Android Studio 상단 `Run` 실행
4. 앱 첫 실행 후 Android 설정에서 `알림 접근 권한` 허용

## 터미널 빌드

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

디버그 APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## MVP 기능

- Toss/송금/결제/충전 알림 텍스트 파싱
- 중복 알림 방지
- 자동 분류 규칙
- 수정 대기함
- 월간 리포트
- CSV 내보내기
