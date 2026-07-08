# DB Backed Review Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace sample review cards with open `review_items` joined to saved transactions.

**Architecture:** Add an `OpenReviewItem` domain model and repository Flow for open review items. Map each open item to `ReviewCardUi`. Review actions resolve the item so the card disappears; wallet topup usage still saves actual spend before resolving.

**Tech Stack:** Kotlin, Room, Kotlin Flow, Jetpack Compose Material 3, JUnit + Truth, Gradle wrapper.

## Global Constraints

- Preview/fallback may still use `sampleReviewCards` when no repository is injected.
- Actual app must render DB-backed review cards.
- Handled review cards disappear by setting `resolvedAt`.
- Wallet topup amount itself stays excluded from spending; only user-entered usage creates `WALLET_SPEND`.
- Verification: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 1: Review Card Mapper

**Files:**
- Modify: `app/src/main/java/com/choiyoonseo/automoney/domain/model/MoneyModels.kt`
- Create: `app/src/main/java/com/choiyoonseo/automoney/ui/model/ReviewItemMapper.kt`
- Create: `app/src/test/java/com/choiyoonseo/automoney/ui/model/ReviewItemMapperTest.kt`

- [x] Write failing mapper tests for wallet topup and transfer cards.
- [x] Run mapper tests and confirm RED.
- [x] Implement mapper.
- [x] Run mapper tests and confirm GREEN.

### Task 2: Repository Observe And Resolve

**Files:**
- Modify: `data/local/entity/Entities.kt`
- Modify: `data/local/dao/ReviewItemDao.kt`
- Modify: `data/repository/MoneyRepository.kt`
- Modify: `data/repository/RoomMoneyRepository.kt`
- Modify fake repository tests.

- [x] Add Room relation for review item plus transaction.
- [x] Add `observeOpenReviewItems()`.
- [x] Add `resolveReviewItem(reviewItemId)`.
- [x] Map relation to `OpenReviewItem`.

### Task 3: Review Screen Wiring

**Files:**
- Modify: `ui/AppRoot.kt`
- Modify: `ui/review/ReviewScreen.kt`

- [x] Pass repository into review screen.
- [x] Collect open review items and map to cards.
- [x] Resolve non-wallet actions.
- [x] Resolve wallet cards after usage save or `아직 안 씀`.

### Task 4: Verification

- [x] Run full Gradle verification.
- [x] Install APK on emulator.
- [x] Verify empty review screen on emulator.
- [x] Verify DB review item observe/resolve flow with connected Android test.
