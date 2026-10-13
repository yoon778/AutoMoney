# AutoMoney

**금융 알림을 거래 기록으로 정리하는 Android 가계부 MVP**

결제·송금·충전 알림에서 거래 정보를 추출하고, 중복 여부와 분류 규칙을 적용합니다. 확인이 필요한 기록은 수정 대기함에 모아 사용자가 검토하고, 확정된 기록은 월간 리포트와 예산 화면에 반영합니다.

[핵심 기능](#핵심-기능) · [구현 살펴보기](#구현-살펴보기) · [로컬 실행](#로컬-실행) · [다른 프로젝트](#다른-프로젝트)

## 핵심 기능

| 기능 | 구현 내용 |
| --- | --- |
| 알림 수집·파싱 | Android 알림 접근 권한과 앱별 허용 설정을 바탕으로 금융 알림을 처리합니다. Toss·일반 금융·증권 알림 파서를 분리합니다. |
| 중복 판별·규칙 분류 | 알림 식별값과 거래 정보를 비교하고, 가맹점·키워드 등의 규칙으로 거래 유형과 카테고리를 정리합니다. |
| 수정 대기함 | 중복 의심, 낮은 분류 신뢰도, 충전 등 확인이 필요한 거래를 검토하고 확정합니다. |
| 수동 기록·수정 | 알림으로 기록되지 않은 거래를 직접 입력하고 날짜·금액·유형·카테고리를 수정합니다. |
| 월간 리포트 | 월별 거래와 소비 내역을 조회하고, 환불 연결과 정산 시 본인 부담액을 집계에 반영합니다. |
| 예산 관리 | 월별 카테고리 예산과 사용액을 비교하고, 예산 밖 지출을 확인합니다. |

## 구현 살펴보기

알림을 읽는 Android 서비스, 거래를 판단하는 도메인 로직, Room 저장소, Compose 화면을 나누어 구성했습니다.

```text
허용한 앱의 알림
    → 내용 추출 및 파서 선택
    → 분류 규칙 · 중복 판별
    → 거래 저장 / 수정 대기
    → 거래 목록 · 월간 리포트 · 예산
```

| 살펴볼 부분 | 소스 |
| --- | --- |
| 알림 수집과 처리 큐 | [MoneyNotificationListenerService](app/src/main/java/com/choiyoonseo/automoney/notification/MoneyNotificationListenerService.kt) |
| 파싱·분류·중복 처리 흐름 | [NotificationIngestionUseCase](app/src/main/java/com/choiyoonseo/automoney/notification/NotificationIngestionUseCase.kt) |
| 알림별 파서 선택 | [NotificationParserRouter](app/src/main/java/com/choiyoonseo/automoney/domain/parser/NotificationParserRouter.kt) |
| 사용자 검토 후 거래 확정 | [ResolveReviewUseCase](app/src/main/java/com/choiyoonseo/automoney/domain/review/ResolveReviewUseCase.kt) |
| 월간 지출·환불·정산 집계 | [TransactionReportRules](app/src/main/java/com/choiyoonseo/automoney/domain/report/TransactionReportRules.kt) |
| 로컬 저장과 DB 마이그레이션 | [AppDatabase](app/src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt) · [RoomMoneyRepository](app/src/main/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepository.kt) |

### 기술 구성

- **언어·화면:** Kotlin, Jetpack Compose, Material 3
- **상태·비동기 처리:** Kotlin Coroutines, Flow
- **저장:** Room, SQLite
- **Android 연동:** NotificationListenerService
- **테스트:** JUnit, Truth, Coroutines Test, AndroidX Test, Room Testing
- **빌드:** Gradle Wrapper, Android Gradle Plugin, KSP

의존성과 SDK 설정은 [앱 빌드 설정](app/build.gradle.kts)에서 확인할 수 있습니다.

## 로컬 실행

1. 이 저장소를 내려받아 Android Studio에서 루트 폴더를 엽니다.
2. Gradle 동기화를 완료하고 필요한 Android SDK를 설치합니다. 현재 `compileSdk`는 37, 최소 실행 버전은 Android 8.0(API 26)입니다.
3. 휴대폰 또는 에뮬레이터를 연결하고 `app` 구성을 실행합니다.
4. 알림 자동 기록을 사용하려면 Android 설정에서 AutoMoney의 **알림 접근 권한**을 허용합니다.
5. 앱 설정에서 수집할 알림 앱을 확인하고, 거래 목록과 수정 대기함에서 처리 결과를 확인합니다.

### 터미널 빌드

Android Studio의 Gradle JDK를 사용하거나, 해당 JDK의 실제 설치 경로로 `JAVA_HOME`을 설정합니다. 아래 경로는 설치 위치에 맞게 바꿉니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

생성되는 디버그 APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

연결된 기기에서 DB 마이그레이션 등 Android 계측 테스트를 실행하려면:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 테스트와 현재 범위

[단위 테스트](app/src/test/java/com/choiyoonseo/automoney)에는 알림 파싱, 중복·수집 처리, 분류 규칙, 검토 확정, 월간 집계 관련 테스트가 있습니다. [기기 테스트](app/src/androidTest/java/com/choiyoonseo/automoney)는 Room 저장소와 DB 마이그레이션 등을 다룹니다.

개발 중인 MVP이며, 알림 형식과 앱별 권한 상태에 따라 자동 기록 결과가 달라질 수 있습니다. 현재 소스에는 CSV 내보내기 기능이 포함되어 있지 않습니다.

## 다른 프로젝트

[개발자 프로필](https://github.com/yoon778) · [기특해](https://github.com/yoon778/giteukhae) · [말사이](https://github.com/yoon778/mal-sai) · [No Save Love](https://github.com/yoon778/26_WinAPI_No_Save_Love_Project)
