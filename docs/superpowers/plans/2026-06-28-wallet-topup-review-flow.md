# Wallet Topup Review Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Treat prepaid/point topups as review-only money movement and create expense records only when the user enters actual usage.

**Architecture:** Keep notification parsing conservative: topup notifications create `WALLET_TOPUP` transactions with `NEEDS_REVIEW`. Add a focused domain service that turns a reviewed topup plus user-entered usage into a `WALLET_SPEND` expense and remaining-balance result. Update sample Review UI so this rule is visible before full DB-backed screens.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit + Truth, Gradle wrapper.

## Global Constraints

- `WALLET_TOPUP` must not count as monthly expense.
- Actual point/prepaid usage must be recorded as `WALLET_SPEND`.
- Unknown or unobserved point usage must stay in review/pending state.
- Verification command: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Parser Topup Confidence

**Files:**
- Modify: `app/src/test/java/com/choiyoonseo/automoney/domain/parser/TossNotificationParserTest.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/parser/TossNotificationParser.kt`

**Interfaces:**
- Produces: topup drafts with `TransactionType.WALLET_TOPUP`, `TransactionDirection.NEUTRAL`, `TransactionStatus.NEEDS_REVIEW`, `ReviewReason.WALLET_TOPUP`, and no category.

- [x] **Step 1: Add failing tests for topup variants**

Add tests for 네이버페이, 카카오페이, 페이코, 포인트 충전 text.

- [x] **Step 2: Run parser test and confirm RED**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "*TossNotificationParserTest"`.

- [x] **Step 3: Expand topup keyword/wallet extraction**

Update parser so topup variants are all neutral review drafts.

- [x] **Step 4: Run parser test and confirm GREEN**

Run same parser test command.

### Task 2: Wallet Topup Review Service

**Files:**
- Create: `app/src/test/java/com/choiyoonseo/automoney/domain/review/WalletTopupReviewServiceTest.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/domain/review/WalletTopupReviewService.kt`

**Interfaces:**
- Produces: `WalletTopupReviewService.recordUsage(topup, usedAmount, category, merchant, memo): WalletTopupUsageResult`.
- Result fields: `reviewedTopup`, `walletSpend`, `remainingAmount`.

- [x] **Step 1: Add failing tests**

Test 10,000 topup + 6,000 usage creates `WALLET_SPEND` expense and 4,000 remaining.

- [x] **Step 2: Confirm RED**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "*WalletTopupReviewServiceTest"`.

- [x] **Step 3: Implement service**

Validate input transaction type and used amount. Mark topup as `USER_EDITED`. Create wallet spend only when used amount is greater than zero.

- [x] **Step 4: Confirm GREEN**

Run same service test command.

### Task 3: Review UI Copy And Sample State

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/model/DashboardUiModels.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/review/ReviewScreen.kt`
- Modify: `app/src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt`

**Interfaces:**
- Consumes: existing `ReviewCardUi`.
- Produces: user-facing copy that says topup itself is not a spend and asks for actual usage.

- [x] **Step 1: Update sample review card**

Show topup amount, actual usage guidance, and remaining-balance concept.

- [x] **Step 2: Update transaction guidance**

Explain topups go to review and only entered usage becomes expense.

### Task 4: Verification

**Files:**
- Build outputs only.

- [x] **Step 1: Run full verification**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

- [x] **Step 2: Install and launch on emulator when available**

Run: `adb install -r app\build\outputs\apk\debug\app-debug.apk` and `adb shell am start -n com.choiyoonseo.automoney/.MainActivity`.
